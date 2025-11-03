package org.folio.dcb.service.impl;

import static java.lang.Integer.MAX_VALUE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.BORROWER;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.dcb.client.feign.CirculationClient;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.Loan;
import org.folio.dcb.domain.dto.LoanCollection;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.folio.dcb.domain.entity.TransactionEntity;
import org.folio.dcb.exception.StatusException;
import org.folio.dcb.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionsServiceImplTest {

  private static final String TX_ID = UUID.randomUUID().toString();
  private static final String LOAN_ID = UUID.randomUUID().toString();
  private static final String ITEM_ID = UUID.randomUUID().toString();
  @InjectMocks private TransactionsServiceImpl transactionsService;
  @Mock private CirculationClient circulationClient;
  @Mock private TransactionRepository transactionRepository;

  @AfterEach
  void tearDown() {
    Mockito.verifyNoMoreInteractions(circulationClient);
  }

  @Test
  void blockItemRenewalByTransactionId_positive() {
    var circulationLoans = circulationLoans(virtualItemLoan());
    when(transactionRepository.findById(TX_ID)).thenReturn(Optional.of(validTxEntity()));
    when(circulationClient.fetchLoanByQuery(anyString())).thenReturn(circulationLoans);

    transactionsService.blockItemRenewalByTransactionId(TX_ID);

    var expectedLoanToUpdate = virtualItemLoan().renewalCount(Integer.toString(MAX_VALUE));
    verify(circulationClient).updateLoan(LOAN_ID, expectedLoanToUpdate);
  }

  @Test
  void blockItemRenewalByTransactionId_negative_loansNotFound() {
    var circulationLoans = circulationLoans();
    when(transactionRepository.findById(TX_ID)).thenReturn(Optional.of(validTxEntity()));
    when(circulationClient.fetchLoanByQuery(anyString())).thenReturn(circulationLoans);

    assertThatThrownBy(() -> transactionsService.blockItemRenewalByTransactionId(TX_ID))
      .isInstanceOf(StatusException.class)
      .hasMessage("Virtual loan for DCB transaction not found.");
    verify(circulationClient, never()).updateLoan(anyString(), any());
  }

  @Test
  void blockItemRenewalByTransactionId_negative_tooManyLoansFound() {
    var circulationLoans = circulationLoans(virtualItemLoan(), virtualItemLoan());
    when(transactionRepository.findById(TX_ID)).thenReturn(Optional.of(validTxEntity()));
    when(circulationClient.fetchLoanByQuery(anyString())).thenReturn(circulationLoans);

    assertThatThrownBy(() -> transactionsService.blockItemRenewalByTransactionId(TX_ID))
      .isInstanceOf(StatusException.class)
      .hasMessage("Multiple virtual loans found for DCB transaction.");
    verify(circulationClient, never()).updateLoan(anyString(), any());
  }

  @ParameterizedTest
  @CsvSource(value = {
    "LENDER, ITEM_CHECKED_OUT", "LENDER, CREATED", "LENDER, CLOSED",
    "PICKUP, ITEM_CHECKED_OUT", "PICKUP, CREATED", "PICKUP, CLOSED",
    "BORROWER, CREATED", "BORROWER, CLOSED",
    "BORROWING_PICKUP, CREATED", "BORROWING_PICKUP, CLOSED",
  })
  void blockItemRenewalByTransactionId_negative_invalidEntity(RoleEnum role, StatusEnum status) {
    var transactionEntityOpt = Optional.of(txEntity(role, status));
    when(transactionRepository.findById(TX_ID)).thenReturn(transactionEntityOpt);

    assertThatThrownBy(() -> transactionsService.blockItemRenewalByTransactionId(TX_ID))
      .isInstanceOf(StatusException.class)
      .hasMessage("DCB transaction has invalid state for renewal block. "
        + "Item must be already checked out by borrower or borrowing pickup role.");
  }

  @ParameterizedTest
  @CsvSource(value = {
    "LENDER, ITEM_CHECKED_OUT", "LENDER, CREATED", "LENDER, CLOSED",
    "PICKUP, ITEM_CHECKED_OUT", "PICKUP, CREATED", "PICKUP, CLOSED",
    "BORROWER, CREATED", "BORROWER, CLOSED",
    "BORROWING_PICKUP, CREATED", "BORROWING_PICKUP, CLOSED",
  })
  void unblockItemRenewalByTransactionId_negative_invalidEntity(RoleEnum role, StatusEnum status) {
    var transactionEntityOpt = Optional.of(txEntity(role, status));
    when(transactionRepository.findById(TX_ID)).thenReturn(transactionEntityOpt);

    assertThatThrownBy(() -> transactionsService.unblockItemRenewalByTransactionId(TX_ID))
      .isInstanceOf(StatusException.class)
      .hasMessage("DCB transaction has invalid state for renewal unblock. "
        + "Item must be already checked out by borrower or borrowing pickup role.");
  }

  @Test
  void unblockItemRenewalByTransactionId_positive() {
    var circulationLoans = circulationLoans(virtualItemLoan());
    when(transactionRepository.findById(TX_ID)).thenReturn(Optional.of(validTxEntity()));
    when(circulationClient.fetchLoanByQuery(anyString())).thenReturn(circulationLoans);

    transactionsService.unblockItemRenewalByTransactionId(TX_ID);

    var expectedLoanToUpdate = virtualItemLoan().renewalCount("0");
    verify(circulationClient).updateLoan(LOAN_ID, expectedLoanToUpdate);
  }

  private static TransactionEntity validTxEntity() {
    return txEntity(BORROWER, ITEM_CHECKED_OUT);
  }

  private static TransactionEntity txEntity(RoleEnum roleEnum, StatusEnum statusEnum) {
    var transactionEntity = new TransactionEntity();
    transactionEntity.setId(TX_ID);
    transactionEntity.setRole(roleEnum);
    transactionEntity.setStatus(statusEnum);
    transactionEntity.setItemId(ITEM_ID);
    return transactionEntity;
  }

  private static LoanCollection circulationLoans(Loan... loans) {
    return new LoanCollection().loans(List.of(loans)).totalRecords(loans.length);
  }

  private static Loan virtualItemLoan() {
    return new Loan()
      .id(LOAN_ID)
      .itemId(ITEM_ID)
      .renewalCount("5");
  }
}
