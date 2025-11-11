package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.DCBConstants.CANCELLATION_REASON_ID;
import static org.folio.dcb.utils.DCBConstants.DCB_CANCELLATION_REASON_NAME;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CancellationReasonClient;
import org.folio.dcb.client.feign.CancellationReasonClient.CancellationReason;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbCancellationReasonService implements DcbEntityService<CancellationReason> {

  private final CancellationReasonClient cancellationReasonClient;

  @Override
  public Optional<CancellationReason> findDcbEntity() {
    var query = exactMatchById(CANCELLATION_REASON_ID);
    var cancellationReasonsByQuery = cancellationReasonClient.findByQuery(query);
    return findFirstValue(cancellationReasonsByQuery);
  }

  @Override
  public CancellationReason createDcbEntity() {
    log.debug("createDcbCancellationReason:: Creating a new DCB Cancellation Reason");
    var dcbCancellationReason = getDcbCancellationReason();

    var createdReason = cancellationReasonClient.createCancellationReason(dcbCancellationReason);
    log.info("createDcbCancellationReason:: DCB Cancellation Reason created");
    return createdReason;
  }

  @Override
  public CancellationReason getDefaultValue() {
    return getDcbCancellationReason();
  }

  private static CancellationReason getDcbCancellationReason() {
    return CancellationReason.builder()
      .id(CANCELLATION_REASON_ID)
      .description(DCB_CANCELLATION_REASON_NAME)
      .name(DCB_CANCELLATION_REASON_NAME)
      .build();
  }
}
