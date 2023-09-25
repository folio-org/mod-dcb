package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.folio.dcb.utils.EntityUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LendingLibraryServiceTest {
  private static final String CHECK_IN_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/check_in.json");

  @InjectMocks
  private LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private UserServiceImpl userService;
  @Mock
  private RequestServiceImpl requestService;
  @Mock
  private TransactionMapper transactionMapper;

  @Test
  void createTransactionTest() {
    var item = createDcbItem();
    var patron = createDcbPatron();
    var user = createUser();

    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(userService.fetchOrCreateUser(any()))
      .thenReturn(user);
    doNothing().when(requestService).createPageItemRequest(any(), any());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());

    var response = lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(transactionRepository).existsById(DCB_TRANSACTION_ID);
    verify(transactionRepository).save(any());
    verify(transactionMapper).mapToEntity(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(userService).fetchOrCreateUser(patron);
    verify(requestService).createPageItemRequest(user, item);

    Assertions.assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
  }

  @Test
  void createTransactionWithExistingTransactionIdTest() {
    var dcbTransaction = createDcbTransaction();
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(true);
    assertThrows(ResourceAlreadyExistException.class, () ->
      lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, dcbTransaction));
  }

  @Test
  void createTransactionWithInvalidEntityTest() {
    var dcbTransaction = createDcbTransaction();
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(userService.fetchOrCreateUser(any()))
      .thenReturn(createUser());
    doNothing().when(requestService).createPageItemRequest(any(), any());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () ->
      lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, dcbTransaction));
  }

  @Test
  void updateTransactionTest() {
    var transactionEntity = createTransactionEntity();
    transactionEntity.setStatus(TransactionStatus.StatusEnum.CREATED);
    when(transactionRepository.findByItemId(any())).thenReturn(Optional.of(transactionEntity));

    lendingLibraryService.updateTransactionStatus(CHECK_IN_EVENT_SAMPLE);
    Mockito.verify(transactionRepository, times(1)).save(transactionEntity);
  }
}
