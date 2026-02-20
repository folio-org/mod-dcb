package org.folio.dcb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.Metadata;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingScope;
import org.folio.dcb.domain.entity.SettingEntity;
import org.folio.dcb.domain.mapper.SettingMapper;
import org.folio.dcb.repository.SettingRepository;
import org.folio.dcb.support.types.UnitTest;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

  private static final UUID SETTING_ID_1 = UUID.randomUUID();
  private static final UUID SETTING_ID_2 = UUID.randomUUID();
  private static final String SETTING_KEY_1 = "dcb.setting#1";
  private static final String SETTING_KEY_2 = "dcb.setting#2";
  private static final String SETTING_VALUE_1 = "value#1";
  private static final String SETTING_VALUE_2 = "value#2";
  private static final String USER_ID = UUID.randomUUID().toString();

  @InjectMocks private SettingService settingService;
  @Mock private SettingMapper settingMapper;
  @Mock private SettingRepository settingRepository;

  @Test
  void createSetting_positive() {
    var setting = setting();
    when(settingRepository.existsById(SETTING_ID_1)).thenReturn(false);
    when(settingRepository.existsByKey(SETTING_KEY_1)).thenReturn(false);
    when(settingMapper.convert(setting)).thenReturn(settingEntity());
    when(settingRepository.save(settingEntity())).thenReturn(settingEntity());
    when(settingMapper.convert(settingEntity())).thenReturn(setting());

    var result = settingService.createSetting(setting);

    assertThat(result).isEqualTo(setting());
  }

  @Test
  void createSetting_negative_existsById() {
    var setting = setting();
    when(settingRepository.existsById(SETTING_ID_1)).thenReturn(true);

    assertThatThrownBy(() -> settingService.createSetting(setting))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Setting already exists with id: " + SETTING_ID_1);
    verify(settingRepository, never()).save(any());
  }

  @Test
  void createSetting_negative_existsByKey() {
    var setting = setting();
    when(settingRepository.existsById(SETTING_ID_1)).thenReturn(false);
    when(settingRepository.existsByKey(SETTING_KEY_1)).thenReturn(true);

    assertThatThrownBy(() -> settingService.createSetting(setting))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Setting with key already exists: " + SETTING_KEY_1);
    verify(settingRepository, never()).save(any());
  }

  @Test
  void getSettingById_positive_returnsSetting() {
    var entity = settingEntity();
    var expectedSetting = setting();
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.of(entity));
    when(settingMapper.convert(entity)).thenReturn(expectedSetting);

    var result = settingService.getSettingById(SETTING_ID_1);

    assertThat(result).isEqualTo(expectedSetting);
  }

  @Test
  void getSettingById_negative_settingNotFound() {
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> settingService.getSettingById(SETTING_ID_1))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Setting not found by id: " + SETTING_ID_1);
  }

  @Test
  void findByQuery_positive_returnsSettingsCollection() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var query = "cql.allRecords=1";
    var entity1 = settingEntity(id1, "dcb.setting#1", "value#1");
    var entity2 = settingEntity(id2, "dcb.setting#2", "value#2");
    var setting1 = setting(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
    var setting2 = setting(SETTING_ID_2, SETTING_KEY_2, SETTING_VALUE_2);
    var pageable = OffsetRequest.of(0, 100);
    var pageResult = new PageImpl<>(List.of(entity1, entity2), pageable, 2);

    when(settingRepository.findByQuery(query, pageable)).thenCallRealMethod();
    when(settingRepository.findByCql(query, pageable)).thenReturn(pageResult);
    when(settingMapper.convert(entity1)).thenReturn(setting1);
    when(settingMapper.convert(entity2)).thenReturn(setting2);

    var result = settingService.findByQuery(query, 100, 0);

    assertThat(result).isEqualTo(ResultList.asSinglePage(setting1, setting2));
  }

  @Test
  void findByQuery_positive_emptyQuery() {
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();
    var entity1 = settingEntity(id1, "dcb.setting#1", "value#1");
    var entity2 = settingEntity(id2, "dcb.setting#2", "value#2");
    var setting1 = setting(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
    var setting2 = setting(SETTING_ID_2, SETTING_KEY_2, SETTING_VALUE_2);
    var pageable = OffsetRequest.of(0, 100);
    var pageResult = new PageImpl<>(List.of(entity1, entity2), pageable, 2);

    when(settingRepository.findByQuery(null, pageable)).thenCallRealMethod();
    when(settingRepository.findAll(pageable)).thenReturn(pageResult);
    when(settingMapper.convert(entity1)).thenReturn(setting1);
    when(settingMapper.convert(entity2)).thenReturn(setting2);

    var result = settingService.findByQuery(null, 100, 0);

    assertThat(result).isEqualTo(ResultList.asSinglePage(setting1, setting2));
  }

  @Test
  void updateSetting_positive_updatesSetting() {
    var updatedSetting = setting(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
    var updatedEntity = settingEntity(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.of(settingEntity()));
    when(settingMapper.convert(updatedSetting)).thenReturn(updatedEntity);

    settingService.updateSetting(updatedSetting);

    verify(settingRepository).save(updatedEntity);
  }

  @Test
  void updateSetting_negative_settingNotFound() {
    var updatedSetting = setting(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> settingService.updateSetting(updatedSetting))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Setting not found by id: " + SETTING_ID_1);
  }

  @Test
  void updateSetting_negative_keyModification() {
    var updatedSetting = setting(SETTING_ID_1, SETTING_KEY_2, SETTING_VALUE_2);
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.of(settingEntity()));
    assertThatThrownBy(() -> settingService.updateSetting(updatedSetting))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Setting key cannot be modified: " + SETTING_KEY_1);
  }

  @Test
  void deleteSettingById_positive() {
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.of(settingEntity()));
    settingService.deleteSettingById(SETTING_ID_1);
    verify(settingRepository).deleteById(SETTING_ID_1);
  }

  @Test
  void deleteSettingById_negative_notFoundById() {
    when(settingRepository.findById(SETTING_ID_1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> settingService.deleteSettingById(SETTING_ID_1))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Setting not found by id: " + SETTING_ID_1);
  }

  private static Setting setting() {
    return setting(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
  }

  private static Setting setting(UUID id, String key, String value) {
    var updatedDate = OffsetDateTime.of(LocalDate.ofYearDay(2025, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    return new Setting()
      .id(id)
      .scope(SettingScope.MOD_DCB)
      .key(key)
      .value(value)
      .version(1)
      .metadata(new Metadata()
        .createdByUserId(USER_ID)
        .createdDate(updatedDate)
        .updatedByUserId(USER_ID)
        .updatedDate(updatedDate));
  }

  private static SettingEntity settingEntity() {
    return settingEntity(SETTING_ID_1, SETTING_KEY_1, SETTING_VALUE_1);
  }

  private static SettingEntity settingEntity(UUID id, String key, String value) {
    var updatedDate = OffsetDateTime.of(LocalDate.ofYearDay(2025, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
    var entity = new SettingEntity();
    entity.setId(id);
    entity.setScope(SettingScope.MOD_DCB.getValue());
    entity.setKey(key);
    entity.setValue(value);
    entity.setVersion(1);
    entity.setCreatedBy(UUID.fromString(USER_ID));
    entity.setCreatedDate(updatedDate);
    entity.setUpdatedBy(UUID.fromString(USER_ID));
    entity.setUpdatedDate(updatedDate);
    return entity;
  }
}
