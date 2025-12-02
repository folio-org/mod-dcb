package org.folio.dcb.it;

import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.ITEM_CHECKED_OUT;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.OPEN;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.borrowingPickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.lenderDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.pickupDcbTransaction;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.junit.jupiter.api.Test;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
class RenewItemInTransactionIT extends BaseTenantIntegrationTest {

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/requests/200-get-by-query(hold requests empty).json",
    "/stubs/mod-circulation/renew-by-id/201-post(item).json",
    "/stubs/mod-circulation-storage/loan-policy-storage/200-get-by-id(limited renewals).json"
  })
  void renewTransaction_positive_lenderRole() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, lenderDcbTransaction());
    putRenewTransaction(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(10))
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(1))
      .andExpect(jsonPath("$.item.holdCount").value(0));
  }

  @Test
  void renewTransaction_negative_lenderRoleAndOpenStatus() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, OPEN, lenderDcbTransaction());
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "transaction status OPEN, it could be renewed only with ITEM_CHECKED_OUT status"));
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation-storage/loan-policy-storage/200-get-by-query(unlimited renewals).json"
  })
  void getTransactionStatus_positive_unlimitedRenewals() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(-1)); // -1 for unlimited
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation/loans/200-get-by-query(dcb loans).json",
    "/stubs/mod-circulation-storage/loan-policy-storage/200-get-by-query(non renewable).json"
  })
  void getTransactionStatus_positive_nonRenewableLoanPolicy() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(8))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(false))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").doesNotExist());
  }

  @Test
  @WireMockStub({
    "/stubs/mod-circulation-storage/loan-policy-storage/200-get-by-query(limited renewals).json",
    "/stubs/mod-circulation/loans/200-get-by-query(item loans+null renewal count).json"
  })
  void getTransactionStatus_positive_nullRenewalCountInLoan() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(0)) // defaults to 0
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(22));
  }

  @Test
  @WireMockStub({"/stubs/mod-circulation/loans/200-get-by-query(empty).json"})
  void getTransactionStatus_positive_noLoanExists() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist());
  }

  @Test
  void renewTransaction_negative_borrowingPickupRole() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowingPickupDcbTransaction());
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role BORROWING-PICKUP, it could be renewed only with role LENDER"));
  }

  @Test
  void renewTransaction_negative_pickupRole() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, pickupDcbTransaction());
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role PICKUP, it could be renewed only with role LENDER"));
  }

  @Test
  void renewTransaction_negative_borrowerRole() throws Exception {
    testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, ITEM_CHECKED_OUT, borrowerDcbTransaction());
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role BORROWER, it could be renewed only with role LENDER"));
  }
}
