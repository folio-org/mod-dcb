package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.client.feign.HoldingsStorageClient.Holding;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbHoldingService implements DcbEntityService<Holding> {

  private final DcbInstanceService dcbInstanceService;
  private final DcbLocationService dcbLocationService;
  private final HoldingsStorageClient holdingsStorageClient;
  private final DcbHoldingSourceService dcbHoldingSourceService;

  @Override
  public Optional<Holding> findDcbEntity() {
    var holdingsByQuery = holdingsStorageClient.findByQuery(exactMatchById(HOLDING_ID));
    return findFirstValue(holdingsByQuery);
  }

  @Override
  public Holding createDcbEntity() {
    var locationId = dcbLocationService.findOrCreateEntity().getId();
    var instanceId = dcbInstanceService.findOrCreateEntity().getId();
    var holdingSourceId = dcbHoldingSourceService.findOrCreateEntity().getId();

    log.debug("createDcbEntity:: creating a new DCB Holding with source id: {}", holdingSourceId);
    var holding = getDcbHolding(instanceId, locationId, holdingSourceId);

    var created = holdingsStorageClient.createHolding(holding);
    log.info("createDcbEntity:: DCB Holding created: {}", created.getId());

    return created;
  }

  @Override
  public Holding getDefaultValue() {
    return getDcbHolding(INSTANCE_ID, LOCATION_ID, null);
  }

  private static Holding getDcbHolding(String instanceId, String locationId, String sourceId) {
    return Holding.builder()
      .id(HOLDING_ID)
      .instanceId(instanceId)
      .permanentLocationId(locationId)
      .sourceId(sourceId)
      .build();
  }
}
