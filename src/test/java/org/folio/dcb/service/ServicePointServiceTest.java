package org.folio.dcb.service;

import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.integration.invstorage.ServicePointClient;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.folio.dcb.utils.DcbConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.folio.dcb.domain.dto.ServicePointRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  private static final String SETTING_KEY = "lender.hold-shelf-expiry-period";

    private static final String FIRST_SP_ID = "550e8400-e29b-41d4-a716-446655440000";

    private static final String SECOND_SP_ID = "660e8400-e29b-41d4-a716-446655441111";

  @InjectMocks private ServicePointServiceImpl servicePointService;
  @Mock private ServicePointClient servicePointClient;
  @Mock private CalendarService calendarService;
  @Mock private ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @Test
  void createServicePointIfNotExistsTest() {
    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.empty());
    when(servicePointClient.createServicePoint(any())).thenReturn(createServicePointRequest());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(DcbConstants.DEFAULT_PERIOD);
    var response = servicePointService.createServicePointIfNotExists(createDcbTransactionByRole(RoleEnum.LENDER));
    verify(servicePointClient).createServicePoint(any());
    verify(servicePointClient).findByQuery(any());
    verify(calendarService).addServicePointIdToDefaultCalendar(UUID.fromString(response.getId()));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(any());
  }

  @Test
  void createServicePointIfExistsTest() {
    var servicePointRequest = createServicePointRequest();
    var servicePointId = UUID.randomUUID().toString();
    servicePointRequest.setId(servicePointId);
    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.of(0, List.of(servicePointRequest)));
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(
      HoldShelfExpiryPeriod.builder()
        .duration(2)
        .intervalId(IntervalIdEnum.MONTHS)
        .build()
    );
    var response = servicePointService.createServicePointIfNotExists(createDcbTransactionByRole(RoleEnum.LENDER));
    assertEquals(servicePointId, response.getId());
    assertEquals(2, response.getHoldShelfExpiryPeriod().getDuration());
    assertEquals(IntervalIdEnum.MONTHS, response.getHoldShelfExpiryPeriod().getIntervalId());
    verify(servicePointClient, times(1)).updateServicePointById(any(), any());
    verify(servicePointClient).findByQuery(any());
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(
      UUID.fromString(response.getId()));
    verify(calendarService, never()).addServicePointIdToDefaultCalendar(any());
  }

    @ParameterizedTest
  @CsvSource({
    "LENDER, lender.hold-shelf-expiry-period",
    "BORROWING_PICKUP, borrowing-pickup.hold-shelf-expiry-period",
    "PICKUP, pickup.hold-shelf-expiry-period",
    "BORROWER, borrower.hold-shelf-expiry-period"
  })
  void testCreateServicePointIfNotExistsShouldUseRoleSpecificSettingKeys(RoleEnum role, String expectedSettingKey) {
    // TestMate-0bddc1ec31baedc07ec2ca420ab0335e
    // Given
    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.empty());
    when(servicePointClient.createServicePoint(any())).thenReturn(createServicePointRequest());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(expectedSettingKey)).thenReturn(DcbConstants.DEFAULT_PERIOD);
    var dcbTransaction = createDcbTransactionByRole(role);
    // When
    servicePointService.createServicePointIfNotExists(dcbTransaction);
    // Then
    verify(servicePointExpirationPeriodService).getShelfExpiryPeriod(expectedSettingKey);
    verify(servicePointClient).createServicePoint(any());
  }

    @Test
  void testCreateServicePointIfNotExistsWhenMultipleServicePointsFoundShouldProcessFirstOne() {
    // TestMate-1e269ad2996e9842558491ac08a9ea65
    // Given
    var dcbTransaction = createDcbTransactionByRole(RoleEnum.LENDER);
    var firstServicePoint = createServicePointRequest();
    firstServicePoint.setId(FIRST_SP_ID);
    var secondServicePoint = createServicePointRequest();
    secondServicePoint.setId(SECOND_SP_ID);
    var resultList = ResultList.of(2, List.of(firstServicePoint, secondServicePoint));
    var expiryPeriod = HoldShelfExpiryPeriod.builder()
      .duration(5)
      .intervalId(IntervalIdEnum.DAYS)
      .build();
    when(servicePointClient.findByQuery(any())).thenReturn(resultList);
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(expiryPeriod);
    // When
    var result = servicePointService.createServicePointIfNotExists(dcbTransaction);
    // Then
    assertThat(result.getId()).isEqualTo(FIRST_SP_ID);
    verify(servicePointClient).updateServicePointById(eq(FIRST_SP_ID), any());
    verify(servicePointClient, never()).updateServicePointById(eq(SECOND_SP_ID), any());
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(FIRST_SP_ID));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(SECOND_SP_ID));
    verify(calendarService, never()).addServicePointIdToDefaultCalendar(any());
  }
}
