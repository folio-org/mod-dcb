package org.folio.dcb.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.client.feign.CirculationItemClient;
import org.folio.dcb.domain.dto.CirculationItem;
import org.folio.dcb.domain.dto.DcbItem;
import org.folio.dcb.domain.dto.ItemStatus;
import org.folio.dcb.service.CirculationItemService;
import org.folio.dcb.service.ItemService;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import static org.folio.dcb.domain.dto.ItemStatus.NameEnum.IN_TRANSIT;
import static org.folio.dcb.utils.DCBConstants.HOLDING_ID;
import static org.folio.dcb.utils.DCBConstants.LOAN_TYPE_ID;
import static org.folio.dcb.utils.DCBConstants.MATERIAL_TYPE_NAME_BOOK;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationItemServiceImpl implements CirculationItemService {

  private final ItemService itemService;
  private final CirculationItemClient circulationItemClient;


  @Override
  public CirculationItem checkIfItemExistsAndCreate(DcbItem dcbItem, String pickupServicePointId) {
    var dcbItemBarcode = dcbItem.getBarcode();
    log.debug("checkIfItemExistsAndCreate:: generate Circulation item with barcode {} if it doesn't exist.", dcbItemBarcode);
    var circulationItem = fetchCirculationItemByBarcode(dcbItem.getBarcode());
    if(Objects.isNull(circulationItem)) {
      log.warn("checkIfItemExistsAndCreate:: Circulation item not found by barcode={}. Creating it.", dcbItemBarcode);
      circulationItem = createCirculationItem(dcbItem, pickupServicePointId);
    }
    return circulationItem;
  }

  private CirculationItem fetchCirculationItemByBarcode(String barcode) {
    return circulationItemClient.fetchItemByIdAndBarcode("barcode==" + barcode)
      .getItems()
      .stream()
      .findFirst()
      .orElse(null);
  }

  public CirculationItem fetchItemById(String itemId) {
    log.info("fetchItemById:: fetching item details for id {} ", itemId);
    return circulationItemClient.retrieveCirculationItemById(itemId);
  }

  private CirculationItem createCirculationItem(DcbItem item, String pickupServicePointId){
    //SetupDefaultMaterialTypeIfNotGiven
    String materialType = StringUtils.isBlank(item.getMaterialType()) ? MATERIAL_TYPE_NAME_BOOK : item.getMaterialType();
    var materialTypeId = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialType);

    CirculationItem circulationItem =
      CirculationItem.builder()
        .id(UUID.randomUUID().toString())
        .barcode(item.getBarcode())
        .status(ItemStatus.builder()
          .name(IN_TRANSIT)
          .build())
        .holdingsRecordId(HOLDING_ID)
        .instanceTitle(item.getTitle())
        .materialTypeId(materialTypeId)
        .permanentLoanTypeId(LOAN_TYPE_ID)
        .pickupLocation(pickupServicePointId)
        .lendingLibraryCode(item.getLendingLibraryCode())
        .build();

    return circulationItemClient.createCirculationItem(item.getId(), circulationItem);
  }
}
