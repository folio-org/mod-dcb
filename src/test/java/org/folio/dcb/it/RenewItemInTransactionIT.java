package org.folio.dcb.it;

import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
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
  @Sql("/db/scripts/lender_transaction(item_checked_out).sql")
  void renewTransaction_positive_lenderRole() throws Exception {
    putRenewTransaction(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.status").value("ITEM_CHECKED_OUT"))
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(10))
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(1))
      .andExpect(jsonPath("$.item.holdCount").value(0));
  }

  @Test
  @Sql("/db/scripts/lender_transaction(open).sql")
  void renewTransaction_negative_lenderRoleAndOpenStatus() throws Exception {
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
  @Sql("/db/scripts/borrower_transaction(item_checked_out).sql")
  void getTransactionStatus_positive_unlimitedRenewals() throws Exception {
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
  @Sql("/db/scripts/borrower_transaction(item_checked_out).sql")
  void getTransactionStatus_positive_nonRenewableLoanPolicy() throws Exception {
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
  @Sql("/db/scripts/borrower_transaction(item_checked_out).sql")
  void getTransactionStatus_positive_nullRenewalCountInLoan() throws Exception {
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo.renewalCount").value(0)) // defaults to 0
      .andExpect(jsonPath("$.item.renewalInfo.renewable").value(true))
      .andExpect(jsonPath("$.item.renewalInfo.renewalMaxCount").value(22));
  }

  @Test
  @WireMockStub({"/stubs/mod-circulation/loans/200-get-by-query(empty).json"})
  @Sql("/db/scripts/borrower_transaction(item_checked_out).sql")
  void getTransactionStatus_positive_noLoanExists() throws Exception {
    getDcbTransactionStatus(DCB_TRANSACTION_ID)
      .andExpect(jsonPath("$.item.renewalInfo").doesNotExist());
  }

  @Test
  @Sql("/db/scripts/borrowing_pickup_transaction(item_checked_out).sql")
  void renewTransaction_negative_borrowingPickupRole() throws Exception {
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role BORROWING-PICKUP, it could be renewed only with role LENDER"));
  }

  @Test
  @Sql("/db/scripts/pickup_transaction(item_checked_out).sql")
  void renewTransaction_negative_pickupRole() throws Exception {
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role PICKUP, it could be renewed only with role LENDER"));
  }

  @Test
  @Sql("/db/scripts/borrower_transaction(item_checked_out).sql")
  void renewTransaction_negative_borrowerRole() throws Exception {
    putRenewTransactionAttempt(DCB_TRANSACTION_ID)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].message").value("Loan couldn't be renewed with "
        + "role BORROWER, it could be renewed only with role LENDER"));
  }
}
