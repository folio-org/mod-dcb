package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.config.DcbFeatureProperties;
import org.folio.dcb.integration.circstorage.CancellationReasonClient.CancellationReason;
import org.folio.dcb.integration.circstorage.model.LoanType;
import org.folio.dcb.integration.invstorage.model.InventoryHolding;
import org.folio.dcb.integration.invstorage.model.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.domain.dto.ServicePointRequest;

@ExtendWith(MockitoExtension.class)
class DcbEntityServiceFacadeTest {

  private static final String TEST_HOLDING_ID = "10cd3a5a-d36f-4c7a-bc4f-e1ae3cf820c9";
  private static final String TEST_LOCATION_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_LOAN_TYPE_ID = "4dec5417-0765-4767-bed6-b363a2d7d4e2";

  @InjectMocks private DcbEntityServiceFacade dcbEntityServiceFacade;
  @Mock private DcbHoldingService dcbHoldingService;
  @Mock private DcbLoanTypeService dcbLoanTypeService;
  @Mock private DcbCalendarService dcbCalendarService;
  @Mock private DcbLocationService dcbLocationService;
  @Mock private DcbFeatureProperties dcbFeatureProperties;
  @Mock private DcbCancellationReasonService dcbCancellationReasonService;

    @Mock private DcbServicePointService dcbServicePointService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(dcbHoldingService, dcbLoanTypeService, dcbCalendarService,
      dcbLocationService, dcbFeatureProperties, dcbCancellationReasonService);
  }

  @Test
  void createAll_shouldCallAllServicesInCorrectOrder() {
    dcbEntityServiceFacade.createAll();

    verify(dcbLocationService).findOrCreateEntity();
    verify(dcbHoldingService).findOrCreateEntity();
    verify(dcbCancellationReasonService).findOrCreateEntity();
    verify(dcbLoanTypeService).findOrCreateEntity();
    verify(dcbCalendarService).findOrCreateEntity();
  }

  @Test
  void findOrCreateHolding_positive_shouldCallRealServiceWhenSettingEnabled() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(true);
    when(dcbHoldingService.findOrCreateEntity()).thenReturn(dcbHolding());

    var result = dcbEntityServiceFacade.findOrCreateHolding();

    assertThat(result).isEqualTo(dcbHolding());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbHoldingService).findOrCreateEntity();
    verify(dcbHoldingService, never()).getDefaultValue();
  }

  @Test
  void findOrCreateHolding_positive_shouldReturnDefaultValueWhenSettingDisable() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(false);
    when(dcbHoldingService.getDefaultValue()).thenReturn(dcbHolding());

    var result = dcbEntityServiceFacade.findOrCreateHolding();

    assertThat(result).isEqualTo(dcbHolding());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbHoldingService, never()).findOrCreateEntity();
    verify(dcbHoldingService).getDefaultValue();
  }

  @Test
  void findOrCreateLocation_positive_shouldCallRealServiceWhenSettingEnabled() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(true);
    when(dcbLocationService.findOrCreateEntity()).thenReturn(dcbLocation());

    var result = dcbEntityServiceFacade.findOrCreateLocation();

    assertThat(result).isEqualTo(dcbLocation());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbLocationService).findOrCreateEntity();
    verify(dcbLocationService, never()).getDefaultValue();
  }

  @Test
  void findOrCreateLocation_positive_shouldReturnDefaultValueWhenSettingDisable() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(false);
    when(dcbLocationService.getDefaultValue()).thenReturn(dcbLocation());

    var result = dcbEntityServiceFacade.findOrCreateLocation();

    assertThat(result).isEqualTo(dcbLocation());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbLocationService, never()).findOrCreateEntity();
    verify(dcbLocationService).getDefaultValue();
  }

  @Test
  void findOrCreateLoanType_positive_shouldCallRealServiceWhenSettingEnabled() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(true);
    when(dcbLoanTypeService.findOrCreateEntity()).thenReturn(dcbLoanType());

    var result = dcbEntityServiceFacade.findOrCreateLoanType();

    assertThat(result).isEqualTo(dcbLoanType());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbLoanTypeService).findOrCreateEntity();
    verify(dcbLoanTypeService, never()).getDefaultValue();
  }

  @Test
  void findOrCreateLoanType_positive_shouldReturnDefaultValueWhenSettingDisable() {
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(false);
    when(dcbLoanTypeService.getDefaultValue()).thenReturn(dcbLoanType());

    var result = dcbEntityServiceFacade.findOrCreateLoanType();

    assertThat(result).isEqualTo(dcbLoanType());
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbLoanTypeService, never()).findOrCreateEntity();
    verify(dcbLoanTypeService).getDefaultValue();
  }

  @Test
  void findOrCreateCancellationReason_positive_shouldCallRealServiceWhenSettingEnabled() {
    // TestMate-233b10edf4c5abe0192c502e99901811
    var expectedReason = CancellationReason.builder()
      .id(UUID.randomUUID().toString())
      .name("Test Cancellation Reason")
      .description("Test Description")
      .build();
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(true);
    when(dcbCancellationReasonService.findOrCreateEntity()).thenReturn(expectedReason);

    var result = dcbEntityServiceFacade.findOrCreateCancellationReason();

    assertThat(result).isEqualTo(expectedReason);
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbCancellationReasonService).findOrCreateEntity();
    verify(dcbCancellationReasonService, never()).getDefaultValue();
  }

  @Test
  void findOrCreateCancellationReason_positive_shouldReturnDefaultValueWhenSettingDisable() {
    // TestMate-27e852d8ac959e89f0f20990af103d9b
    var defaultReason = CancellationReason.builder()
      .id(UUID.randomUUID().toString())
      .name("Default Cancellation Reason")
      .description("Default Description")
      .build();
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(false);
    when(dcbCancellationReasonService.getDefaultValue()).thenReturn(defaultReason);

    var result = dcbEntityServiceFacade.findOrCreateCancellationReason();

    assertThat(result).isEqualTo(defaultReason);
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbCancellationReasonService, never()).findOrCreateEntity();
    verify(dcbCancellationReasonService).getDefaultValue();
  }

    @Test
  void findOrCreateServicePoint_positive_shouldCallRealServiceWhenSettingEnabled() {
    // TestMate-f15a491a22f67aa18c13de8f320841b9
    // Given
    var servicePoint = ServicePointRequest.builder()
      .id("789e4567-e89b-12d3-a456-426614174000")
      .name("DCB Service Point")
      .build();
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(true);
    when(dcbServicePointService.findOrCreateEntity()).thenReturn(servicePoint);
    // When
    var result = dcbEntityServiceFacade.findOrCreateServicePoint();
    // Then
    assertThat(result).isEqualTo(servicePoint);
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbServicePointService).findOrCreateEntity();
    verify(dcbServicePointService, never()).getDefaultValue();
  }

    @Test
  void findOrCreateServicePoint_positive_shouldReturnDefaultValueWhenSettingDisable() {
    // TestMate-81ba568b37134c15dd397be23498b85b
    // Given
    var defaultServicePoint = ServicePointRequest.builder()
      .id("789e4567-e89b-12d3-a456-426614174000")
      .name("Default Service Point")
      .build();
    when(dcbFeatureProperties.isDcbEntitiesRuntimeVerificationEnabled()).thenReturn(false);
    when(dcbServicePointService.getDefaultValue()).thenReturn(defaultServicePoint);
    // When
    var result = dcbEntityServiceFacade.findOrCreateServicePoint();
    // Then
    assertThat(result).isEqualTo(defaultServicePoint);
    verify(dcbFeatureProperties).isDcbEntitiesRuntimeVerificationEnabled();
    verify(dcbServicePointService).getDefaultValue();
    verify(dcbServicePointService, never()).findOrCreateEntity();
  }

  private static InventoryHolding dcbHolding() {
    return InventoryHolding.builder()
      .id(TEST_HOLDING_ID)
      .build();
  }

  private static Location dcbLocation() {
    return Location.builder()
      .id(TEST_LOCATION_ID)
      .build();
  }

  private static LoanType dcbLoanType() {
    return LoanType.builder()
      .id(TEST_LOAN_TYPE_ID)
      .build();
  }
}
