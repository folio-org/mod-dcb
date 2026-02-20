package org.folio.dcb.service.impl;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.folio.dcb.utils.DCBConstants.DEFAULT_PERIOD;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.service.ServicePointExpirationPeriodService;
import org.folio.dcb.service.SettingService;
import org.folio.dcb.utils.CqlQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class ServicePointExpirationPeriodServiceImpl implements ServicePointExpirationPeriodService {

  private final JsonMapper jsonMapper;
  private final SettingService settingService;
  private final ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;

  @Override
  @Transactional(readOnly = true)
  public HoldShelfExpiryPeriod getShelfExpiryPeriod(String settingKey) {
    var settingsByKey = settingService.findByQuery(CqlQuery.exactMatch("key", settingKey).getQuery(), 1, 0);
    return emptyIfNull(settingsByKey.getResult()).stream()
      .filter(Objects::nonNull)
      .findFirst()
      .flatMap(this::extractHoldShelfExpiryPeriod)
      .orElseGet(this::findShelfExpiryPeriodInDatabase);
  }

  private Optional<HoldShelfExpiryPeriod> extractHoldShelfExpiryPeriod(Setting setting) {
    try {
      var convertedValue = jsonMapper.convertValue(setting.getValue(), HoldShelfExpiryPeriod.class);
      return Optional.ofNullable(convertedValue)
        .filter(value -> allNotNull(value.getDuration(), value.getIntervalId()));
    } catch (Exception e) {
      log.warn("extractHoldShelfExpiryPeriod:: failed to extract setting value: {}", setting, e);
      return Optional.empty();
    }
  }

  private HoldShelfExpiryPeriod findShelfExpiryPeriodInDatabase() {
    List<ServicePointExpirationPeriodEntity> periodList = servicePointExpirationPeriodRepository.findAll();
    if (CollectionUtils.isEmpty(periodList)) {
      log.debug("getShelfExpiryPeriod:: default hold shelf expire period will be set: {}", DEFAULT_PERIOD);
      return DEFAULT_PERIOD;
    } else {
      var customPeriod = getCustomPeriod(periodList.getFirst());
      log.debug("getShelfExpiryPeriod:: custom hold shelf expire period will be set: {}", customPeriod);
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
