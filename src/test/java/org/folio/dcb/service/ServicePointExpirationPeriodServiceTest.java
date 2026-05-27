package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.domain.dto.DcbTransaction.RoleEnum.LENDER;
import static org.folio.dcb.domain.dto.IntervalIdEnum.HOURS;
import static org.folio.dcb.domain.dto.IntervalIdEnum.MINUTES;
import static org.folio.dcb.domain.dto.IntervalIdEnum.MONTHS;
import static org.folio.dcb.service.ServicePointExpirationPeriodService.getSettingsKey;
import static org.folio.spring.model.ResultList.asSinglePage;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingScope;
import org.folio.dcb.domain.entity.ServicePointExpirationPeriodEntity;
import org.folio.dcb.repository.ServicePointExpirationPeriodRepository;
import org.folio.dcb.service.impl.ServicePointExpirationPeriodServiceImpl;
import org.folio.dcb.utils.DCBConstants;
import org.folio.dcb.utils.JsonTestUtils;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicePointExpirationPeriodServiceTest {

  private static final String SETTING_KEY = "test.hold-shelf-expiry-period";

  @InjectMocks private ServicePointExpirationPeriodServiceImpl servicePointExpirationPeriodService;
  @Mock private ServicePointExpirationPeriodRepository servicePointExpirationPeriodRepository;
  @Mock private SettingService settingService;
  @Spy private ObjectMapper jsonMapper = JsonTestUtils.objectMapper;

  @Test
  void getShelfExpiryPeriod_positive_settingFound() {
    var holdShelfExpiryPeriod = new HoldShelfExpiryPeriod().duration(15).intervalId(MINUTES);
    var setting = setting(jsonMapper.valueToTree(holdShelfExpiryPeriod));
    when(settingService.findByQuery("key==\"%s\"".formatted(SETTING_KEY), 1, 0)).thenReturn(asSinglePage(setting));

    var actual = servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY);

    assertThat(actual).isEqualTo(holdShelfExpiryPeriod);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidSettingDataSource")
  void getShelfExpiryPeriod_parameterized_validSettingNotFound(@SuppressWarnings("unused") String name, Setting setting) {
    var settingKey = getSettingsKey(LENDER.getValue());
    when(settingService.findByQuery("key==\"%s\"".formatted(settingKey), 1, 0)).thenReturn(asSinglePage(setting));
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(Collections.emptyList());

    var actual = servicePointExpirationPeriodService.getShelfExpiryPeriod(settingKey);

    assertThat(settingKey).isEqualTo("lender.hold-shelf-expiry-period");
    assertThat(actual).isEqualTo(DCBConstants.DEFAULT_PERIOD);
  }

  @ParameterizedTest
  @MethodSource("databaseShelfExpiryDataSource")
  void getShelfExpiryPeriod_positive_settingFoundInDatabase(List<ServicePointExpirationPeriodEntity> periods,
    HoldShelfExpiryPeriod expected) {
    when(settingService.findByQuery("key==\"%s\"".formatted(SETTING_KEY), 1, 0)).thenReturn(ResultList.empty());
    when(servicePointExpirationPeriodRepository.findAll()).thenReturn(periods);
    var actual = servicePointExpirationPeriodService.getShelfExpiryPeriod(SETTING_KEY);
    assertThat(actual).isEqualTo(expected);
  }

  private static Stream<Arguments> databaseShelfExpiryDataSource() {
    return Stream.of(
      arguments(List.of(), DCBConstants.DEFAULT_PERIOD),
      arguments(servicePointExpirationPeriodList(2, MONTHS), holdShelfPeriod(2, MONTHS)),
      arguments(servicePointExpirationPeriodList(3, HOURS), holdShelfPeriod(3, HOURS)),
      arguments(servicePointExpirationPeriodList(4, MINUTES), holdShelfPeriod(4, MINUTES))
    );
  }

  private static Stream<Arguments> invalidSettingDataSource() {
    var invalidContentNode = JsonTestUtils.objectMapper.createObjectNode().put("invalidKey", "invalidValue");
    return Stream.of(
      arguments("null value", null),
      arguments("duration is null and intervalId is null", setting(holdShelfPeriodNode(null, null))),
      arguments("intervalId is null", setting(holdShelfPeriodNode(2L, null))),
      arguments("duration is null", setting(holdShelfPeriodNode(null, "Minutes"))),
      arguments("intervalId in lowercase", setting(holdShelfPeriodNode(10L, "minutes"))),
      arguments("intervalId in uppercase", setting(holdShelfPeriodNode(10L, "MINUTES"))),
      arguments("duration is max long value", setting(holdShelfPeriodNode(Long.MAX_VALUE, "Minutes"))),
      arguments("node with invalid content", setting(invalidContentNode)),
      arguments("null in value", setting(null)),
      arguments("boolean in value", setting(false)),
      arguments("empty string in value", setting("")),
      arguments("string in value", setting("invalid-content")),
      arguments("integer in value", setting(100)),
      arguments("json string in value", setting("{\"duration\":\"10\",\"intervalId\":\"Minutes\"}")),
      arguments("long in value", setting(Long.MIN_VALUE)),
      arguments("invalid map in value", setting(Map.of("invalidKey", "invalidValue"))),
      arguments("array in value", setting(List.of(holdShelfPeriodNode(1L, "Days"), holdShelfPeriodNode(2L, "Days"))))
    );
  }

  private static HoldShelfExpiryPeriod holdShelfPeriod(int duration, IntervalIdEnum intervalId) {
    return HoldShelfExpiryPeriod.builder()
      .duration(duration)
      .intervalId(intervalId)
      .build();
  }

  private static ObjectNode holdShelfPeriodNode(Long duration, String intervalId) {
    return JsonTestUtils.objectMapper.createObjectNode()
      .put("duration", duration)
      .put("intervalId", intervalId);
  }

  private static List<ServicePointExpirationPeriodEntity> servicePointExpirationPeriodList(
    int duration, IntervalIdEnum intervalId) {
    return List.of(ServicePointExpirationPeriodEntity.builder()
      .duration(duration)
      .intervalId(intervalId)
      .build());
  }

  public static Setting setting(Object value) {
    return new Setting()
      .id(UUID.randomUUID())
      .scope(SettingScope.MOD_DCB)
      .key(SETTING_KEY)
      .value(value);
  }
}
