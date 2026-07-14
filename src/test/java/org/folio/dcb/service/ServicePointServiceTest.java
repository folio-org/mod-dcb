package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.IntervalIdEnum.MONTHS;
import static org.folio.dcb.utils.DcbConstants.DEFAULT_PERIOD;
import static org.folio.dcb.utils.EntityUtils.createDcbTransactionByRole;
import static org.folio.dcb.utils.EntityUtils.createServicePointRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.DcbTransaction.RoleEnum;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.integration.invstorage.ServicePointClient;
import org.folio.dcb.service.impl.ServicePointServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicePointServiceTest {

  private static final String SETTING_KEY = "lender.hold-shelf-expiry-period";

  @InjectMocks private ServicePointServiceImpl servicePointService;
  @Mock private ServicePointClient servicePointClient;
  @Mock private CalendarService calendarService;
  @Mock private ServicePointExpirationPeriodService servicePointExpirationPeriodService;

  @Test
  void createServicePointIfNotExists_positive_notFoundByQuery() {
    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.empty());
    when(servicePointClient.createServicePoint(any())).thenReturn(createServicePointRequest());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(DEFAULT_PERIOD);

    var response = servicePointService.createServicePointIfNotExists(createDcbTransactionByRole(RoleEnum.LENDER));

    assertThat(response.getId()).isNotNull();
    verify(servicePointClient).createServicePoint(any());
    verify(servicePointClient).findByQuery(any());
    verify(calendarService).addServicePointIdToDefaultCalendar(UUID.fromString(response.getId()));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(any());
  }

  @Test
  void createServicePointIfNotExists_positive_foundByQuery() {
    var servicePointId = UUID.randomUUID().toString();
    var servicePointRequest = createServicePointRequest();
    servicePointRequest.setId(servicePointId);
    var holdShelfExpiryPeriod = HoldShelfExpiryPeriod.builder()
      .duration(2)
      .intervalId(MONTHS)
      .build();

    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.asSinglePage(servicePointRequest));
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(holdShelfExpiryPeriod);

    var response = servicePointService.createServicePointIfNotExists(createDcbTransactionByRole(RoleEnum.LENDER));

    assertThat(response.getId()).isNotNull().isEqualTo(servicePointId);
    assertThat(response.getHoldShelfExpiryPeriod())
      .satisfies(expiryPeriod -> assertThat(expiryPeriod.getDuration()).isEqualTo(2))
      .satisfies(expiryPeriod -> assertThat(expiryPeriod.getIntervalId()).isEqualTo(MONTHS));
    verify(servicePointClient).updateServicePointById(any(), any());
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
  void createServicePointIfNotExists_parameterized(RoleEnum role, String expectedSettingKey) {
    // TestMate-0bddc1ec31baedc07ec2ca420ab0335e
    when(servicePointClient.findByQuery(any())).thenReturn(ResultList.empty());
    when(servicePointClient.createServicePoint(any())).thenReturn(createServicePointRequest());
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(expectedSettingKey)).thenReturn(DEFAULT_PERIOD);

    var dcbTransaction = createDcbTransactionByRole(role);
    servicePointService.createServicePointIfNotExists(dcbTransaction);

    verify(servicePointExpirationPeriodService).getShelfExpiryPeriod(expectedSettingKey);
    verify(servicePointClient).createServicePoint(any());
  }

  @Test
  void createServicePointIfNotExists_positive_multipleServicePointsFound() {
    // TestMate-1e269ad2996e9842558491ac08a9ea65
    var firstSpId = UUID.randomUUID().toString();
    var secondSpId = UUID.randomUUID().toString();
    var firstServicePoint = createServicePointRequest().id(firstSpId);
    var secondServicePoint = createServicePointRequest().id(secondSpId);
    var resultList = ResultList.asSinglePage(firstServicePoint, secondServicePoint);
    var expiryPeriod = HoldShelfExpiryPeriod.builder().duration(5).intervalId(IntervalIdEnum.DAYS).build();

    when(servicePointClient.findByQuery(any())).thenReturn(resultList);
    when(servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY)).thenReturn(expiryPeriod);

    var dcbTransaction = createDcbTransactionByRole(RoleEnum.LENDER);
    var result = servicePointService.createServicePointIfNotExists(dcbTransaction);

    assertThat(result.getId()).isEqualTo(firstSpId);
    verify(servicePointClient).updateServicePointById(eq(firstSpId), any());
    verify(servicePointClient, never()).updateServicePointById(eq(secondSpId), any());
    verify(calendarService).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(firstSpId));
    verify(calendarService, never()).associateServicePointIdWithDefaultCalendarIfAbsent(UUID.fromString(secondSpId));
    verify(calendarService, never()).addServicePointIdToDefaultCalendar(any());
  }
}
