package org.folio.dcb.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.domain.dto.CirculationItemRequest;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.ItemService;
import org.springframework.stereotype.Service;

import static org.folio.dcb.domain.dto.Status.NameEnum.IN_TRANSIT;

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
  public DcbItem fetchOrCreateItem(DcbItem dcbItem) {
    var dcbItemId = dcbItem.getId();
    log.debug("fetchOrCreateItem:: generate Circulation item by DcbItem with id={}", dcbItemId);

    CirculationItemRequest circulationItem;
    try {
      log.debug("fetchOrCreateItem:: trying to find existed Circulation item");
      circulationItem = circulationItemClient.retrieveCirculationItemById(dcbItemId);
    } catch (FeignException.NotFound ex) {
      log.debug("Circulation item not found by id={}. Creating it.", dcbItemId);
      circulationItem = createCirculationItem(dcbItem);
    }

    return mapCirculationItemRequestToDcbItem(circulationItem);
  }

  private CirculationItemRequest createCirculationItem(DcbItem item){
    var materialTypeId = itemService.fetchItemMaterialTypeIdByMaterialTypeName(TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK);
    var loanTypeId = itemService.fetchItemLoanTypeIdByLoanTypeName(INITIAL_CFG_LOAN_TYPE_VALUE);

    CirculationItemRequest circulationItemRequest =
      CirculationItemRequest.builder()
        .id(item.getId())
        .itemBarcode(item.getBarcode())
        .status(IN_TRANSIT.getValue())
        .holdingsRecordId(TEMP_VALUE_HOLDING_ID)
        .instanceTitle(item.getTitle())
        .materialTypeId(materialTypeId)
        .permanentLoanTypeId(loanTypeId)
        .pickupLocation(item.getPickupLocation())
        .build();

    return circulationItemClient.createCirculationItem(item.getId(), circulationItemRequest);
  }

  private DcbItem mapCirculationItemRequestToDcbItem(CirculationItemRequest ci) {
//    var materialTypeName = itemService.fetchItemMaterialTypeNameByMaterialTypeId(ci.getMaterialTypeId());

    return DcbItem.builder()
      .id(ci.getId())
      .barcode(ci.getItemBarcode())
      .title(ci.getInstanceTitle())
      .pickupLocation("3a40852d-49fd-4df2-a1f9-6e2641a6e91f")   // leave it as a temporary solution. checked with Magzhan. Until the field-container will be added into DcbTransaction
//      .lendingLibraryCode()   // we don't need it for the borrowing flow
      .materialType(TEMP_VALUE_MATERIAL_TYPE_NAME_BOOK)
      .build();
  }
}
