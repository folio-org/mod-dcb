package org.folio.dcb.service;

import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.MaterialType;
import org.folio.dcb.integration.inventory.MaterialTypeClient;
import org.folio.dcb.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.folio.spring.exception.NotFoundException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@UnitTest
@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

    @InjectMocks
private ItemServiceImpl itemService;

    @Mock
private MaterialTypeClient materialTypeClient;

    @Test
void fetchItemMaterialTypeIdByMaterialTypeNameTest() {
  // TestMate-0f96f8c42d03650b357196259cdf7e6b
  // Given
  String materialTypeName = "book";
  String materialTypeId = "1a2b3c4d-5e6f-7g8h-9i0j";
  MaterialType materialType = MaterialType.builder()
    .id(materialTypeId)
    .name(materialTypeName)
    .build();
  ResultList<MaterialType> materialTypeResultList = ResultList.asSinglePage(materialType);
  when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(materialTypeResultList);
  // When
  String result = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialTypeName);
  // Then
  verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"book\"");
  assertEquals(materialTypeId, result);
}

    @Test
void fetchItemMaterialTypeIdByInvalidNameTest() {
  // TestMate-772865b75acf5b12a75b8a6a3937b4b1
  // Given
  String materialTypeName = "non-existent-type";
  when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(ResultList.empty());
  // When
  assertThrows(NotFoundException.class, () -> itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialTypeName));
  // Then
  verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"non-existent-type\"");
}

    @Test
void fetchItemMaterialTypeIdByMultipleResultsTest() {
  // TestMate-4b6d139cbb3dac517781e88fd0bd5828
  // Given
  String materialTypeName = "text";
  String firstMaterialTypeId = "11111111-1111-1111-1111-111111111111";
  String secondMaterialTypeId = "22222222-2222-2222-2222-222222222222";
  MaterialType firstMaterialType = MaterialType.builder()
    .id(firstMaterialTypeId)
    .name(materialTypeName)
    .build();
  MaterialType secondMaterialType = MaterialType.builder()
    .id(secondMaterialTypeId)
    .name(materialTypeName)
    .build();
  ResultList<MaterialType> materialTypeResultList = ResultList.asSinglePage(firstMaterialType, secondMaterialType);
  when(materialTypeClient.fetchMaterialTypeByQuery(anyString())).thenReturn(materialTypeResultList);
  // When
  String result = itemService.fetchItemMaterialTypeIdByMaterialTypeName(materialTypeName);
  // Then
  verify(materialTypeClient).fetchMaterialTypeByQuery("name==\"text\"");
  assertEquals(firstMaterialTypeId, result);
}
}
