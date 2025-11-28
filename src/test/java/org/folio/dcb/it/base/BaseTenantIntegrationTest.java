package org.folio.dcb.it.base;

import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static support.kafka.KafkaContainerExtension.createTopics;
import static support.kafka.KafkaContainerExtension.deleteTopics;
import static support.wiremock.WiremockContainerExtension.getWireMockClient;
import static support.wiremock.WiremockStubExtension.resetWiremockStubs;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.DcbUpdateTransaction;
import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.repository.TransactionAuditRepository;
import org.folio.dcb.utils.TestCirculationEventHelper;
import org.folio.dcb.utils.TestJdbcHelper;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import support.AuditEntityTestVerifier;
import support.wiremock.WiremockStubExtension;

@Sql(value = "/db/scripts/cleanup_dcb_tables.sql", executionPhase = AFTER_TEST_METHOD)
public abstract class BaseTenantIntegrationTest extends BaseIntegrationTest {

  protected static final List<String> TENANT_TOPICS = List.of(
    "folio.%s.circulation.request".formatted(TEST_TENANT),
    "folio.%s.circulation.check-in".formatted(TEST_TENANT)
  );

  protected static WireMock wiremock;
  protected static TestJdbcHelper testJdbcHelper;
  protected static TestCirculationEventHelper testEventHelper;
  protected static AuditEntityTestVerifier auditEntityVerifier;

  @BeforeAll
  static void beforeAll(
    @Autowired FolioModuleMetadata folioModuleMetadata,
    @Autowired TransactionAuditRepository transactionAuditRepository,
    @Autowired KafkaTemplate<String, Object> kafkaTemplate,
    @Autowired NamedParameterJdbcTemplate jdbcTemplate) {

    createTopics(TENANT_TOPICS);
    setUpMockForTestTenantInit();
    enableTenant();
    resetWiremockStubs();
    wiremock = getWireMockClient();
    auditEntityVerifier = new AuditEntityTestVerifier(
      new HashMap<>(defaultHeaders()), folioModuleMetadata, transactionAuditRepository);
    testEventHelper = new TestCirculationEventHelper(kafkaTemplate);
    testJdbcHelper = new TestJdbcHelper(jdbcTemplate);
  }

  @AfterAll
  static void afterAll() {
    resetWiremockStubs();
    purgeTenant();
    deleteTopics(TENANT_TOPICS);
    wiremock = null;
    auditEntityVerifier = null;
  }

  protected static void setUpMockForTestTenantInit() {
    WiremockStubExtension.addStubMappings(
      "/stubs/mod-inventory-storage/locations/200-get-by-query(dcb).json",
      "/stubs/mod-inventory-storage/holdings-storage/200-get-by-query(dcb+id).json",
      "/stubs/mod-circulation-storage/cancellation-reason-storage/200-get-by-id(dcb).json",
      "/stubs/mod-inventory-storage/loan-types/200-get-by-query(dcb).json",
      "/stubs/mod-calendar/calendars/200-get-all.json"
    );
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static ResultActions getDcbTransactionStatus(String id) {
    return getDcbTransactionStatusAttempt(id).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions getDcbTransactionStatuses(OffsetDateTime fromDate, OffsetDateTime toDate) {
    var queryParameters = Map.of("fromDate", Objects.toString(fromDate), "toDate", Objects.toString(toDate));
    return getDcbTransactionStatuses(queryParameters);
  }

  @SneakyThrows
  protected static ResultActions getDcbTransactionStatuses(Map<String, ?> queryParameters) {
    return getDcbTransactionStatusesAttempt(queryParameters).andExpect(status().isOk());
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static ResultActions postDcbTransaction(String id, DcbTransaction dcbTransaction) {
    return postDcbTransactionAttempt(id, dcbTransaction).andExpect(status().isCreated());
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static ResultActions putDcbTransactionStatus(String id, TransactionStatus requestBody) {
    return putDcbTransactionStatusAttempt(id, requestBody).andExpect(status().isOk());
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static ResultActions putRenewTransaction(String id) {
    return putRenewTransactionAttempt(id).andExpect(status().isOk());
  }

  @SneakyThrows
  protected static ResultActions getDcbTransactionStatusAttempt(String id) {
    return mockMvc.perform(
      get("/transactions/{id}/status", id)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions getDcbTransactionStatusesAttempt(Map<String, ?> queryParameters) {
    var requestQueryParams = new LinkedMultiValueMap<String, String>();
    queryParameters.entrySet().stream()
      .map(entry -> entry(entry.getKey(), String.valueOf(entry.getValue())))
      .forEach(entry -> requestQueryParams.add(entry.getKey(), entry.getValue()));

    return mockMvc.perform(
      get("/transactions/status")
        .queryParams(requestQueryParams)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions postDcbTransactionAttempt(String id, DcbTransaction body) {
    return mockMvc.perform(
      post("/transactions/{id}", id)
        .content(asJsonString(body))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions putDcbTransactionStatusAttempt(String id, TransactionStatus body) {
    return mockMvc.perform(
      put("/transactions/{id}/status", id)
        .content(asJsonString(body))
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @SneakyThrows
  protected static ResultActions putRenewTransactionAttempt(String id) {
    return mockMvc.perform(
      put("/transactions/{id}/renew", id)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @SuppressWarnings("SameParameterValue")
  @SneakyThrows
  protected static ResultActions putDcbTransactionDetailsAttempt(String id, DcbUpdateTransaction body) {
    return mockMvc.perform(put("/transactions/{id}", id)
      .content(asJsonString(body))
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON));
  }

  protected static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    Awaitility.await()
      .atMost(FIVE_SECONDS)
      .pollInterval(ONE_HUNDRED_MILLISECONDS)
      .untilAsserted(throwingRunnable);
  }
}
