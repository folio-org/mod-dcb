package org.folio.dcb.utils;

import static java.sql.Types.BOOLEAN;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;
import static java.util.Objects.requireNonNull;
import static org.folio.dcb.utils.EntityUtils.REQUEST_ID;
import static org.folio.dcb.utils.EntityUtils.REQUEST_USER_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.folio.dcb.domain.dto.DcbTransaction;
import org.folio.dcb.domain.dto.TransactionStatus.StatusEnum;
import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

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
    var txItem = requireNonNull(tx.getItem());
    var txPickup = requireNonNull(tx.getPickup());
    var txPatron = requireNonNull(tx.getPatron());

    @Language("SQL") var dcbTransactionSqlTemplate = """
      INSERT INTO %s_mod_dcb.transactions (id, request_id, item_id, item_title, item_barcode,
        service_point_id, service_point_name, pickup_library_code, material_type,
        lending_library_code, patron_id, patron_group, patron_barcode, status, role,
        self_borrowing, created_by, created_date, updated_by, updated_date)
      VALUES (:id, :requestId, :itemId, :itemTitle, :itemBarcode, :servicePointId,
        :servicePointName, :pickupLibraryCode, :materialType, :lendingLibraryCode, :patronId,
        :patronGroup, :patronBarcode, :status, :role, :selfBorrowing, :createdBy, :createdDate,
        :updatedBy, :updatedDate)""";

    var dcbTransactionSql = dcbTransactionSqlTemplate.formatted(tenantId);

    var params = new MapSqlParameterSource()
      .addValue("id", transactionId, VARCHAR)
      .addValue("requestId", REQUEST_ID, Types.OTHER)
      .addValue("itemId", txItem.getId(), Types.OTHER)
      .addValue("itemTitle", txItem.getTitle(), VARCHAR)
      .addValue("itemBarcode", txItem.getBarcode(), VARCHAR)
      .addValue("servicePointId", txPickup.getServicePointId(), VARCHAR)
      .addValue("servicePointName", txPickup.getServicePointName(), VARCHAR)
      .addValue("pickupLibraryCode", txPickup.getLibraryCode(), VARCHAR)
      .addValue("materialType", txItem.getMaterialType(), VARCHAR)
      .addValue("lendingLibraryCode", txItem.getLendingLibraryCode(), VARCHAR)
      .addValue("patronId", txPatron.getId(), Types.OTHER)
      .addValue("patronGroup", txPatron.getGroup(), VARCHAR)
      .addValue("patronBarcode", txPatron.getBarcode(), VARCHAR)
      .addValue("status", txStatus.getValue(), VARCHAR)
      .addValue("role", tx.getRole() != null ? tx.getRole().name() : null, VARCHAR)
      .addValue("selfBorrowing", Boolean.TRUE.equals(tx.getSelfBorrowing()), BOOLEAN)
      .addValue("createdBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("createdDate", Timestamp.from(Instant.now().minusSeconds(600)), TIMESTAMP)
      .addValue("updatedBy", REQUEST_USER_ID, Types.OTHER)
      .addValue("updatedDate", null, TIMESTAMP);

    //noinspection SqlSourceToSinkFlow
    jdbcTemplate.update(dcbTransactionSql, params);
  }
}
