package org.folio.dcb.service;

import org.folio.dcb.domain.dto.TransactionStatusResponse;
import org.folio.dcb.domain.mapper.TransactionMapper;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.folio.dcb.repository.TransactionRepository;
import org.folio.dcb.service.impl.LendingLibraryServiceImpl;
import org.folio.dcb.service.impl.RequestServiceImpl;
import org.folio.dcb.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.createTransactionEntity;
import static org.folio.dcb.utils.EntityUtils.createUser;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class LendingLibraryServiceTest {

  @InjectMocks
  LendingLibraryServiceImpl lendingLibraryService;
  @Mock
  TransactionRepository transactionRepository;
  @Mock
  UserServiceImpl userService;
  @Mock
  RequestServiceImpl requestService;
  @Mock
  TransactionMapper transactionMapper;

  @Test
  void createTransactionTest() {
    var item = createDcbItem();
    var patron = createDcbPatron();
    var user = createUser();

    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(userService.createOrFetchUser(any()))
      .thenReturn(user);
    doNothing().when(requestService).createPageItemRequest(any(), any());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(createTransactionEntity());

    var response = lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(transactionRepository).existsById(DCB_TRANSACTION_ID);
    verify(transactionRepository).save(any());
    verify(transactionMapper).mapToEntity(DCB_TRANSACTION_ID, createDcbTransaction());
    verify(userService).createOrFetchUser(patron);
    verify(requestService).createPageItemRequest(user, item);

    Assertions.assertEquals(TransactionStatusResponse.StatusEnum.CREATED, response.getStatus());
  }

  @Test
  void createTransactionWithExistingTransactionIdTest() {
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(true);
    assertThrows(ResourceAlreadyExistException.class, () ->
      lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, createDcbTransaction()));
  }

  @Test
  void createTransactionWithInvalidEntityTest() {
    when(transactionRepository.existsById(DCB_TRANSACTION_ID)).thenReturn(false);
    when(userService.createOrFetchUser(any()))
      .thenReturn(createUser());
    doNothing().when(requestService).createPageItemRequest(any(), any());
    when(transactionMapper.mapToEntity(any(), any())).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () ->
      lendingLibraryService.createTransaction(DCB_TRANSACTION_ID, createDcbTransaction()));
  }
}
