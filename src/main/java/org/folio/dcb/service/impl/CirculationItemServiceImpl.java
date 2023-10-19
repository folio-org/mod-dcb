package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.ItemService;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationItemServiceImpl implements CirculationItemService {

  private static final String TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK = "book";
  private static final String TEMP_VALUE_HOLDING_ID = "10cd3a5a-d36f-4c7a-bc4f-e1ae3cf820c9";
  private static final String INITIAL_CFG_LOAN_TYPE_VALUE = "Can circulate";

  private final ItemService itemService;
  private final CirculationItemClient circulationItemClient;


  @Override
  public void checkIfItemExistsAndCreate(DcbItem dcbItem) {
    var dcbItemId = dcbItem.getId();
    log.debug("checkIfItemExistsAndCreate:: generate Circulation item by DcbItem with id={} if nit doesn't exist.", dcbItemId);

    try {
      log.debug("fetchOrCreateItem:: trying to find existed Circulation item");
      circulationItemClient.retrieveCirculationItemById(dcbItemId);
    } catch (FeignException.NotFound ex) {
      log.debug("Circulation item not found by id={}. Creating it.", dcbItemId);
      createCirculationItem(dcbItem);
    }

  }

  public CirculationItemRequest fetchItemById(String itemId) {
    log.info("fetchItemById:: fetching item details for id {} ", itemId);
    return circulationItemClient.retrieveCirculationItemById(itemId);
  }

  private void createCirculationItem(DcbItem item){
    var materialTypeId = itemService.fetchItemMaterialTypeIdByMaterialTypeName(TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK);
    var loanTypeId = itemService.fetchItemLoanTypeIdByLoanTypeName(INITIAL_CFG_LOAN_TYPE_VALUE);

    CirculationItemRequest circulationItemRequest =
      CirculationItemRequest.builder()
        .id(item.getId())
        .itemBarcode(item.getBarcode())
        .status(ItemStatus.IN_TRANSIT)
        .holdingsRecordId(TEMP_VALUE_HOLDING_ID)
        .instanceTitle(item.getTitle())
        .materialTypeId(materialTypeId)
        .permanentLoanTypeId(loanTypeId)
        .pickupLocation(item.getPickupLocation())
        .build();

    circulationItemClient.createCirculationItem(item.getId(), circulationItemRequest);
  }
}
