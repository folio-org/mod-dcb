package org.folio.dcb.service.impl;

import static org.folio.dcb.utils.DCBConstants.DEFAULT_SERVICE_POINT_PERIOD_DURATION;
import static org.folio.dcb.utils.DCBConstants.DEFAULT_SERVICE_POINT_PERIOD_INTERVAL;

import java.util.List;

import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @RequiredArgsConstructor
@Service
public class ServicePointExpirationPeriodServiceImpl
  implements ServicePointExpirationPeriodService {

  public static final HoldShelfExpiryPeriod DEFAULT_PERIOD = HoldShelfExpiryPeriod.builder()
    .duration(DEFAULT_SERVICE_POINT_PERIOD_DURATION)
    .intervalId(DEFAULT_SERVICE_POINT_PERIOD_INTERVAL)
    .build();
  private final ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;

  @Override
  public HoldShelfExpiryPeriod getShelfExpiryPeriod() {
    List<ServicePointExpirationPeriodEntity> periodList = servicePointExpirationPeriodRepository.findAll();
    if (CollectionUtils.isEmpty(periodList)) {
      log.info("getShelfExpiryPeriod:: default hold shelf expire period will be set: {}", DEFAULT_PERIOD);
      return DEFAULT_PERIOD;
    } else {
      var customPeriod = getCustomPeriod(periodList.get(0));
      log.info("getShelfExpiryPeriod:: custom hold shelf expire period will be set: {}", customPeriod);
      return customPeriod;
    }
  }

  private HoldShelfExpiryPeriod getCustomPeriod(ServicePointExpirationPeriodEntity period) {
    return HoldShelfExpiryPeriod.builder()
      .duration(period.getDuration())
      .intervalId(period.getIntervalId())
      .build();
  }
}
