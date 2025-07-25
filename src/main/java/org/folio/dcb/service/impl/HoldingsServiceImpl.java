package org.folio.dcb.service.impl;

import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.HOLDING_SOURCE;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;
import static org.folio.dcb.utils.DCBConstants.SOURCE;

import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingsStorageClient;
import org.folio.dcb.exception.InventoryResourceOperationException;
import org.folio.dcb.service.HoldingsService;
import org.folio.spring.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class HoldingsServiceImpl implements HoldingsService {

  private final HoldingsStorageClient holdingsStorageClient;
  private final HoldingSourcesClient holdingSourcesClient;

  @Override
  public HoldingsStorageClient.Holding fetchInventoryHoldingDetailsByHoldingId(String holdingsId) {
    log.debug("fetchInventoryHoldingDetailsByHoldingId:: Trying to fetch holdings detail for holdingsId {}", holdingsId);
    try {
      return holdingsStorageClient.findHolding(holdingsId);
    } catch (FeignException.NotFound ex) {
      throw new NotFoundException(String.format("Holdings not found for holdings id %s", holdingsId));
    }
  }

  @Override
  public HoldingsStorageClient.Holding fetchDcbHoldingOrCreateIfMissing() {
    try {
      return holdingsStorageClient.findHolding(HOLDING_ID);
    } catch (FeignException.NotFound ex) {
      return createHolding();
    }
  }

  private HoldingsStorageClient.Holding createHolding() {
    try {
      log.debug("createHolding:: creating holding");
      var holdingResourceId = getHoldingSourceId();
      HoldingsStorageClient.Holding holding = HoldingsStorageClient.Holding.builder()
        .id(HOLDING_ID)
        .instanceId(INSTANCE_ID)
        .permanentLocationId(LOCATION_ID)
        .sourceId(holdingResourceId)
        .build();

      var created = holdingsStorageClient.createHolding(holding);
      log.info("createHolding:: holding created: {}", created.getId());

      return created;
    } catch (Exception ex) {
      log.error("createHolding:: Error while creating holding: {}", ex.getMessage());
      throw InventoryResourceOperationException.createInventoryResourceException("Holding", ex);
    }
  }

  private String getHoldingSourceId() {
    log.debug("getHoldingSourceId:: Fetching holding source id for source {}", SOURCE);

    var holdingResourceList = holdingSourcesClient.querySourceByName(SOURCE);
    if (holdingResourceList.getTotalRecords() == 0) {
      var holdingResource = createHoldingSourceResource();
      return holdingResource.getId();
    }
    return holdingResourceList.getResult().getFirst().getId();
  }

  private HoldingSourcesClient.HoldingSource createHoldingSourceResource() {
    log.info("createHoldingResource:: Creating a new Holding Record source");
    return holdingSourcesClient.createHoldingsRecordSource(HoldingSourcesClient.HoldingSource
      .builder()
      .id(UUID.randomUUID().toString())
      .name(SOURCE)
      .source(HOLDING_SOURCE)
      .build());
  }

}
