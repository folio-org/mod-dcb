package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.HoldingsStorageClient.Holding;
import org.folio.dcb.client.feign.LoanTypeClient.LoanType;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.dcb.config.DcbFeatureProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  private static Holding dcbHolding() {
    return Holding.builder()
      .id(TEST_HOLDING_ID)
      .build();
  }

  private static LocationDTO dcbLocation() {
    return LocationDTO.builder()
      .id(TEST_LOCATION_ID)
      .build();
  }

  private static LoanType dcbLoanType() {
    return LoanType.builder()
      .id(TEST_LOAN_TYPE_ID)
      .build();
  }
}
