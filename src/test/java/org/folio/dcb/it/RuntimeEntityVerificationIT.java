package org.folio.dcb.it;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.dcb.domain.dto.TransactionStatus.StatusEnum.CREATED;
import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.EXISTED_PATRON_ID;
import static org.folio.dcb.utils.EntityUtils.borrowerDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.borrowingPickupDcbTransaction;
import static org.folio.dcb.utils.EntityUtils.dcbItem;
import static org.folio.dcb.utils.EntityUtils.dcbPatron;
import static org.folio.dcb.utils.EntityUtils.dcbTransactionUpdate;
import static org.folio.dcb.utils.EntityUtils.pickupDcbTransaction;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import support.types.IntegrationTest;
import support.wiremock.WireMockStub;

@IntegrationTest
@TestPropertySource(properties = { "application.features.dcb-entities-runtime-verification-enabled=true" })
class RuntimeEntityVerificationIT extends BaseTenantIntegrationTest {

  @Nested
  @DisplayName("BorrowerRoleIT")
  class BorrowerRoleIT {
    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
    })
    void createTransaction_positive_dcbEntitiesExists() throws Exception {
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction())
        .andExpect(jsonPath("$.status").value("CREATED"));

      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/service-points/200-get-by-name(Virtual).json",
      "/stubs/mod-inventory-storage/service-points/204-put(Virtual).json",
      "/stubs/mod-calendar/calendars/200-get-all.json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/location-units/institutions/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/campuses/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id empty).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory/instances/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/instance-types/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-sources/200-get-by-query(dcb empty).json",

      // POST DCB entity mappings
      "/stubs/mod-inventory-storage/location-units/libraries/201-post(dcb).json",
      "/stubs/mod-inventory-storage/locations/201-post(dcb).json",
      "/stubs/mod-inventory-storage/service-points/201-post(dcb).json",
      "/stubs/mod-inventory/instances/201-post(dcb).json",
      "/stubs/mod-inventory-storage/instance-types/201-post(dcb).json",
      "/stubs/mod-inventory-storage/holdings-sources/201-post(dcb).json",
      "/stubs/mod-inventory-storage/holdings-storage/201-post(dcb).json",
    })
    void createTransaction_positive_dcbEntitiesNotFound() throws Exception {
      postDcbTransaction(DCB_TRANSACTION_ID, borrowerDcbTransaction())
        .andExpect(jsonPath("$.status").value("CREATED"));

      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/institutions")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/libraries")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/campuses")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/locations")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/service-points")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/instance-types")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-sources")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/inventory/instances")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(new barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(new barcode).json",
      "/stubs/mod-users/users/200-get-by-query(patron).json",
      "/stubs/mod-circulation-storage/request-storage/200-get-by-id(circ-item).json",
      "/stubs/mod-circulation/requests/201-post(any).json",
      "/stubs/mod-circulation/requests/204-put(any).json",

      // DCB Entities existence verification and creation missing
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
      "/stubs/mod-circulation-storage/cancellation-reason-storage/200-get-by-query(dcb empty).json",
      "/stubs/mod-circulation-storage/cancellation-reason-storage/201-post(dcb).json",
    })
    void updateTransaction_positive_cancellationReasonNotFound() throws Exception {
      testJdbcHelper.saveDcbTransaction(DCB_TRANSACTION_ID, CREATED, borrowerDcbTransaction());
      putDcbTransactionDetailsAttempt(DCB_TRANSACTION_ID, dcbTransactionUpdate())
        .andExpect(status().isNoContent());

      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/locations")));
    }
  }

  @Nested
  @DisplayName("BorrowingPickupIT")
  class BorrowingPickupRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/201-post(pickup).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
      "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb).json",
    })
    void createTransaction_positive_foundDcbEntities() throws Exception {
      var patron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowingPickupDcbTransaction(patron))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem()))
        .andExpect(jsonPath("$.patron").value(patron));

      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/locations")));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode empty).json",
      "/stubs/mod-inventory-storage/material-types/200-get-by-query(book).json",
      "/stubs/mod-circulation-item/201-post(pickup).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/locations/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/institutions/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/campuses/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory/instances/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/instance-types/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-sources/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-storage/dcb-entity-scenario.json",

      // POST DCB entity mappings
      "/stubs/mod-inventory-storage/service-points/201-post(dcb).json",
      "/stubs/mod-inventory-storage/location-units/libraries/201-post(dcb).json",
      "/stubs/mod-inventory-storage/instance-types/201-post(dcb).json",
      "/stubs/mod-inventory/instances/201-post(dcb).json",
      "/stubs/mod-inventory-storage/holdings-sources/201-post(dcb).json",
      "/stubs/mod-inventory-storage/loan-types/201-post(dcb).json",
    })
    void createTransaction_positive_notFoundDcbEntities() throws Exception {
      var patron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, borrowingPickupDcbTransaction(patron))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem()))
        .andExpect(jsonPath("$.patron").value(patron));

      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/institutions")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/libraries")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/campuses")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/locations")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/service-points")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/instance-types")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-sources")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/inventory/instances")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
    }
  }

  @Nested
  @DisplayName("PickupRoleIT")
  class PickupRoleIT {

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
    })
    void createTransaction_positive_foundDcbEntities() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem()))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
      wiremock.verifyThat(0, postRequestedFor(urlPathEqualTo("/locations")));
    }

    @Test
    @WireMockStub({
      "/stubs/mod-users/users/200-get-by-query(dcb user).json",
      "/stubs/mod-inventory-storage/item-storage/200-get-by-query(barcode empty).json",
      "/stubs/mod-circulation-item/200-get-by-query(barcode).json",
      "/stubs/mod-users/groups/200-get-by-query(staff).json",
      "/stubs/mod-circulation/requests/201-post(any).json",

      // DCB Entities existence verification
      "/stubs/mod-inventory-storage/location-units/institutions/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/location-units/campuses/dcb-entity-scenario.json",
      "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/location-units/libraries/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id empty).json",
      "/stubs/mod-inventory-storage/service-points/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory/instances/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/instance-types/200-get-by-query(dcb empty).json",
      "/stubs/mod-inventory-storage/holdings-sources/200-get-by-query(dcb empty).json",

      // POST DCB entity mappings

      "/stubs/mod-inventory-storage/location-units/libraries/201-post(dcb).json",
      "/stubs/mod-inventory-storage/locations/201-post(dcb).json",
      "/stubs/mod-inventory-storage/service-points/201-post(dcb).json",
      "/stubs/mod-inventory/instances/201-post(dcb).json",
      "/stubs/mod-inventory-storage/instance-types/201-post(dcb).json",
      "/stubs/mod-inventory-storage/holdings-sources/201-post(dcb).json",
      "/stubs/mod-inventory-storage/holdings-storage/201-post(dcb).json",
    })
    void createTransaction_positive_notFoundDcbEntities() throws Exception {
      var dcbPatron = dcbPatron(EXISTED_PATRON_ID);
      postDcbTransaction(DCB_TRANSACTION_ID, pickupDcbTransaction(dcbPatron))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.item").value(dcbItem()))
        .andExpect(jsonPath("$.patron").value(dcbPatron));

      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/institutions")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/libraries")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/location-units/campuses")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/locations")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/service-points")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/instance-types")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-sources")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/inventory/instances")));
      wiremock.verifyThat(1, postRequestedFor(urlPathEqualTo("/holdings-storage/holdings")));
    }
  }
}
