package org.folio.dcb.service;

import static org.folio.dcb.service.impl.ServicePointExpirationPeriodServiceImpl.DEFAULT_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.service.impl.ServicePointExpirationPeriodServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ServicePointExpirationPeriodServiceTest {

  @InjectMocks
  private ServicePointExpirationPeriodServiceImpl servicePointExpirationPeriodService;

  @Mock
  private ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;

  @ParameterizedTest
  @MethodSource
  void getShelfExpiryPeriodTest(List<ServicePointExpirationPeriodEntity> periods,
    HoldShelfExpiryPeriod expected) {
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(periods);
    HoldShelfExpiryPeriod actual = servicePointExpirationPeriodService.getShelfExpiryPeriod();
    assertEquals(expected, actual);
  }

  private static Stream<Arguments> getShelfExpiryPeriodTest() {
    return Stream.of(
      Arguments.of(List.of(), DEFAULT_PERIOD),
      Arguments.of(null, DEFAULT_PERIOD),
      Arguments.of(buildServicePointExpirationPeriodList(2, IntervalIdEnum.MONTHS),
        buildExpectedHoldShelfPeriod(2, IntervalIdEnum.MONTHS)),
      Arguments.of(buildServicePointExpirationPeriodList(3, IntervalIdEnum.HOURS),
        buildExpectedHoldShelfPeriod(3, IntervalIdEnum.HOURS)),
      Arguments.of(buildServicePointExpirationPeriodList(4, IntervalIdEnum.MINUTES),
        buildExpectedHoldShelfPeriod(4, IntervalIdEnum.MINUTES))

    );
  }

  private static HoldShelfExpiryPeriod buildExpectedHoldShelfPeriod(int duration,
    IntervalIdEnum intervalId) {
    return HoldShelfExpiryPeriod.builder()
      .duration(duration)
      .intervalId(intervalId)
      .build();
  }

  private static List<ServicePointExpirationPeriodEntity> buildServicePointExpirationPeriodList(
    int duration,
    IntervalIdEnum intervalId) {
    return List.of(ServicePointExpirationPeriodEntity.builder()
      .duration(duration)
      .intervalId(intervalId)
      .build());
  }
}
