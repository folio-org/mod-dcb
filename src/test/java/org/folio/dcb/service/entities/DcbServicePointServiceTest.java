package org.folio.dcb.service.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.service.impl.ServicePointServiceImpl.HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.InventoryServicePointClient;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbServicePointServiceTest {

  private static final String TEST_SERVICE_POINT_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";
  private static final String TEST_NAME = "DCB";
  private static final String TEST_CODE = "000";

  @InjectMocks private DcbServicePointService dcbServicePointService;
  @Mock private InventoryServicePointClient servicePointClient;
  @Mock private ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(servicePointClient, servicePointExpirationPeriodService);
  }

  @Test
  void findDcbEntity_positive_shouldReturnServicePointWhenExists() {
    var foundServicePoints = ResultList.asSinglePage(dcbServicePoint());
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(servicePointClient.findByQuery(expectedQuery)).thenReturn(foundServicePoints);

    var result = dcbServicePointService.findDcbEntity();

    assertThat(result).contains(dcbServicePoint());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    when(servicePointClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbServicePointService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateServicePointWithExpiryPeriod() {
    var expiryPeriod = dcbExpiryPeriod();
    var expectedServicePoint = dcbServicePoint();

    when(servicePointExpirationPeriodService.getShelfExpiryPeriod()).thenReturn(expiryPeriod);
    when(servicePointClient.createServicePoint(expectedServicePoint)).thenReturn(expectedServicePoint);

    var result = dcbServicePointService.createDcbEntity();

    assertThat(result).isEqualTo(dcbServicePoint());
    verify(servicePointExpirationPeriodService).getShelfExpiryPeriod();
    verify(servicePointClient).createServicePoint(expectedServicePoint);
  }

  @Test
  void getDefaultValue_positive_shouldReturnServicePointWithDefaultValues() {
    var result = dcbServicePointService.getDefaultValue();
    assertThat(result).isEqualTo(dcbServicePoint());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingServicePoint() {
    var expectedQuery = exactMatchByName(TEST_NAME);
    var servicePointsResult = ResultList.asSinglePage(dcbServicePoint());
    when(servicePointClient.findByQuery(expectedQuery)).thenReturn(servicePointsResult);

    var result = dcbServicePointService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbServicePoint());
    verify(servicePointExpirationPeriodService, never()).getShelfExpiryPeriod();
    verify(servicePointClient, never()).createServicePoint(any());
  }

  private static ServicePointRequest dcbServicePoint() {
    return ServicePointRequest.builder()
      .id(TEST_SERVICE_POINT_ID)
      .name(TEST_NAME)
      .code(TEST_CODE)
      .discoveryDisplayName(TEST_NAME)
      .pickupLocation(true)
      .holdShelfExpiryPeriod(dcbExpiryPeriod())
      .holdShelfClosedLibraryDateManagement(HOLD_SHELF_CLOSED_LIBRARY_DATE_MANAGEMENT)
      .build();
  }

  private static HoldShelfExpiryPeriod dcbExpiryPeriod() {
    return HoldShelfExpiryPeriod.builder()
      .duration(10)
      .intervalId(IntervalIdEnum.DAYS)
      .build();
  }
}
