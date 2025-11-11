package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.HOLDING_SOURCE;
import static org.folio.dcb.utils.DCBConstants.SOURCE;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingSourcesClient.HoldingSource;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbHoldingSourceService implements DcbEntityService<HoldingSource> {

  private final HoldingSourcesClient holdingSourcesClient;

  @Override
  public Optional<HoldingSource> findDcbEntity() {
    var query = exactMatchByName(SOURCE);
    var holdingSourcesClientByQuery = holdingSourcesClient.findByQuery(query);
    return findFirstValue(holdingSourcesClientByQuery);
  }

  @Override
  public HoldingSource createDcbEntity() {
    log.debug("createDcbEntity:: Creating a new DCB Holding Record source");
    var dcbHoldingSource = getDcbHoldingSource();

    var createdHoldingSource =  holdingSourcesClient.createHoldingsRecordSource(dcbHoldingSource);
    log.info("createDcbEntity:: DCB Holding Record source created");
    return createdHoldingSource;
  }

  @Override
  public HoldingSource getDefaultValue() {
    return getDcbHoldingSource();
  }

  private static HoldingSource getDcbHoldingSource() {
    return HoldingSource.builder()
      .id(UUID.randomUUID().toString())
      .name(SOURCE)
      .source(HOLDING_SOURCE)
      .build();
  }
}
