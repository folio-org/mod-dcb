package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.integration.invstorage.HoldingsStorageClient;
import org.folio.dcb.integration.invstorage.model.InventoryHolding;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbHoldingService implements DcbEntityService<InventoryHolding> {

  private final DcbInstanceService dcbInstanceService;
  private final DcbLocationService dcbLocationService;
  private final HoldingsStorageClient holdingsStorageClient;
  private final DcbHoldingSourceService dcbHoldingSourceService;

  @Override
  public Optional<InventoryHolding> findDcbEntity() {
    var query = exactMatchById(HOLDING_ID).getQuery();
    var holdingsByQuery = holdingsStorageClient.findByQuery(query);
    return findFirstValue(holdingsByQuery);
  }

  @Override
  public InventoryHolding createDcbEntity() {
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
  public InventoryHolding getDefaultValue() {
    return getDcbHolding(INSTANCE_ID, LOCATION_ID, null);
  }

  private static InventoryHolding getDcbHolding(String instanceId, String locationId, String sourceId) {
    return InventoryHolding.builder()
      .id(HOLDING_ID)
      .instanceId(instanceId)
      .permanentLocationId(locationId)
      .sourceId(sourceId)
      .build();
  }
}
