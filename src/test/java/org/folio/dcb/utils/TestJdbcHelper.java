package org.folio.dcb.utils;

import static java.util.Objects.requireNonNull;
import static org.folio.dcb.utils.EntityUtils.REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.REQUEST_USER_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class TestJdbcHelper {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Transactional
  public void saveDcbTransaction(String transactionId, StatusEnum status, DcbTransaction tx) {
    saveDcbTransaction(TEST_TENANT, transactionId, status, tx);
  }

  @Transactional
  public void saveDcbTransaction(String tenantId,
    String transactionId, StatusEnum txStatus, DcbTransaction tx) {
    log.debug("saveDcbTransaction:: providing db entity: id={}, tenant={}, tx={}, status={}",
      () -> transactionId, () -> tenantId, () -> asJsonString(tx), () -> txStatus);
    var txItem = requireNonNull(tx.getItem());
    var txPickup = requireNonNull(tx.getPickup());
    var txPatron = requireNonNull(tx.getPatron());

    var params = new MapSqlParameterSource()
      .addValue("id", transactionId, Types.VARCHAR)
      .addValue("requestId", REQUEST_ID, Types.OTHER)
      .addValue("itemId", txItem.getId(), Types.OTHER)
      .addValue("itemTitle", txItem.getTitle(), Types.VARCHAR)
      .addValue("itemBarcode", txItem.getBarcode(), Types.VARCHAR)
      .addValue("servicePointId", txPickup.getServicePointId(), Types.VARCHAR)
      .addValue("servicePointName", txPickup.getServicePointName(), Types.VARCHAR)
      .addValue("pickupLibraryCode", txPickup.getLibraryCode(), Types.VARCHAR)
      .addValue("materialType", txItem.getMaterialType(), Types.VARCHAR)
      .addValue("lendingLibraryCode", txItem.getLendingLibraryCode(), Types.VARCHAR)
      .addValue("patronId", txPatron.getId(), Types.OTHER)
      .addValue("patronGroup", txPatron.getGroup(), Types.VARCHAR)
      .addValue("patronBarcode", txPatron.getBarcode(), Types.VARCHAR)
      .addValue("status", txStatus.getValue(), Types.VARCHAR)
      .addValue("role", tx.getRole() != null ? tx.getRole().name() : null, Types.VARCHAR)
      .addValue("selfBorrowing", Boolean.TRUE.equals(tx.getSelfBorrowing()), Types.BOOLEAN)
      .addValue("itemLocationCode", txItem.getLocationCode(), Types.VARCHAR)
      .addValue("createdBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("createdDate", Timestamp.from(Instant.now().minusSeconds(600)), Types.TIMESTAMP)
      .addValue("updatedBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("updatedDate", null, Types.TIMESTAMP);

    //noinspection SqlSourceToSinkFlow
    jdbcTemplate.update(getDcbTransactionSql(tenantId), params);
  }

  @Transactional
  public void saveDcbSetting(String tenantId, Setting setting) {
    log.debug("saveDcbSetting:: providing db entity: tenant={}, setting={}", () -> tenantId, () -> asJsonString(setting));

    var createdDate = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(600);
    var params = new MapSqlParameterSource()
      .addValue("id", setting.getId(), Types.OTHER)
      .addValue("scope", "mod-dcb", Types.VARCHAR)
      .addValue("key", setting.getKey(), Types.VARCHAR)
      .addValue("value", asJsonString(setting.getValue()), Types.OTHER)
      .addValue("version", 1, Types.INTEGER)
      .addValue("createdBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("createdDate", createdDate, Types.TIMESTAMP_WITH_TIMEZONE)
      .addValue("updatedBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("updatedDate", createdDate, Types.TIMESTAMP_WITH_TIMEZONE);

    //noinspection SqlSourceToSinkFlow
    jdbcTemplate.update(getDcbSettingSql(tenantId), params);
  }

  private static @NonNull String getDcbTransactionSql(String tenantId) {
    @Language("PostgreSQL") var dcbTransactionSqlTemplate = """
      INSERT INTO %s_mod_dcb.transactions (id, request_id, item_id, item_title, item_barcode,
        service_point_id, service_point_name, pickup_library_code, material_type,
        lending_library_code, patron_id, patron_group, patron_barcode, status, role,
        self_borrowing, item_location_code, created_by, created_date, updated_by, updated_date)
      VALUES (:id, :requestId, :itemId, :itemTitle, :itemBarcode, :servicePointId,
        :servicePointName, :pickupLibraryCode, :materialType, :lendingLibraryCode, :patronId,
        :patronGroup, :patronBarcode, :status, :role, :selfBorrowing, :itemLocationCode,
        :createdBy, :createdDate, :updatedBy, :updatedDate)""";

    return dcbTransactionSqlTemplate.formatted(tenantId);
  }

  private static @NonNull String getDcbSettingSql(String tenantId) {
    @Language("PostgreSQL") var dcbSettingSqlTemplate = """
      INSERT INTO %s_mod_dcb.settings (id, key, scope, value, version, created_by, created_date, updated_by, updated_date)
      VALUES (:id, :key, :scope, :value::jsonb, :version, :createdBy, :createdDate, :updatedBy, :updatedDate)
    """;

    return dcbSettingSqlTemplate.formatted(tenantId);
  }
}
