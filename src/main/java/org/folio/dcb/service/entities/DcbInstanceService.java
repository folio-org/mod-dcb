package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_ID;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TITLE;
import static org.folio.dcb.utils.DCBConstants.INSTANCE_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.SOURCE;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.integration.inventory.InstanceClient;
import org.folio.dcb.integration.inventory.model.InventoryInstance;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbInstanceService implements DcbEntityService<InventoryInstance> {

  private final InstanceClient instanceClient;
  private final DcbInstanceTypeService dcbInstanceTypeService;

  @Override
  public Optional<InventoryInstance> findDcbEntity() {
    var query = exactMatchById(INSTANCE_ID).getQuery();
    var instancesByQuery = instanceClient.findByQuery(query);
    return findFirstValue(instancesByQuery);
  }

  @Override
  public InventoryInstance createDcbEntity() {
    var dcbInstanceType = dcbInstanceTypeService.findOrCreateEntity();
    log.debug("createDcbEntity:: Creating a new DCB Instance");
    var inventoryInstanceDTO = getDcbInstance(dcbInstanceType.getId());

    instanceClient.createInstance(inventoryInstanceDTO);
    log.info("createDcbEntity:: DCB Instance created");
    return inventoryInstanceDTO;
  }

  @Override
  public InventoryInstance getDefaultValue() {
    return getDcbInstance(INSTANCE_TYPE_ID);
  }

  private static InventoryInstance getDcbInstance(String instanceTypeId) {
    return InventoryInstance.builder()
      .id(INSTANCE_ID)
      .instanceTypeId(instanceTypeId)
      .title(INSTANCE_TITLE)
      .source(SOURCE)
      .build();
  }
}
