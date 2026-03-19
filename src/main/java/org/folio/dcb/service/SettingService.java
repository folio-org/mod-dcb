package org.folio.dcb.service;

import static org.springframework.beans.BeanUtils.copyProperties;

import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingsCollection;
import org.folio.dcb.domain.mapper.SettingMapper;
import org.folio.dcb.repository.SettingRepository;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.model.ResultList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class SettingService {

  private final SettingMapper settingMapper;
  private final SettingRepository settingRepository;

  /**
   * Create and persist a new {@link Setting} object.
   *
   * @param setting setting DTO to create
   * @return created setting DTO with any generated fields populated (for example id)
   */
  @Transactional
  public Setting create(Setting setting) {
    if (settingRepository.existsById(setting.getId())) {
      throw new IllegalArgumentException("Setting already exists with id: " + setting.getId());
    }

    if (settingRepository.existsByKey(setting.getKey())) {
      log.debug("createSetting:: Setting already exists by key: {}", setting.getKey());
      throw new IllegalArgumentException("Setting with key already exists: " + setting.getKey());
    }

    var entity = settingMapper.convert(setting.version(null));
    var savedEntity = settingRepository.save(entity);
    return settingMapper.convert(savedEntity);
  }

  /**
   * Retrieve a {@link Setting} object by its identifier.
   *
   * @param id UUID of the requested setting
   * @return the setting DTO
   * @throws NotFoundException when no setting with the given id exists
   */
  @Transactional(readOnly = true)
  public Setting getSettingById(UUID id) {
    return settingRepository.findById(id)
      .map(settingMapper::convert)
      .orElseThrow(() -> new NotFoundException("Setting not found by id: " + id));
  }

  /**
   * Find {@link Setting} objects by a query string with pagination.
   *
   * @param query  query string used to filter settings
   * @param limit  maximum number of items to return
   * @param offset zero-based offset into the result set
   * @return {@link SettingsCollection} containing the matched settings and total record information
   */
  @Transactional(readOnly = true)
  public ResultList<Setting> findByQuery(String query, int limit, int offset) {
    var pageable = OffsetRequest.of(offset, limit);
    var foundSettings = settingRepository.findByQuery(query, pageable).map(settingMapper::convert);
    return ResultList.of((int) foundSettings.getTotalElements(), foundSettings.getContent());
  }

  /**
   * Update an existing {@link Setting}. The setting must already exist.
   *
   * @param updatedSetting setting DTO with updated values
   * @throws NotFoundException when no setting with the given id exists
   */
  @Transactional
  public void update(UUID id, Setting updatedSetting) {
    if (!Objects.equals(updatedSetting.getId(), id)) {
      throw new IllegalArgumentException("Id cannot be modified: " + id);
    }

    var existingEntity = settingRepository.findById(id)
      .orElseThrow(() -> new NotFoundException("Setting not found by id: " + updatedSetting.getId()));

    if (!existingEntity.getKey().equals(updatedSetting.getKey())) {
      throw new IllegalArgumentException("Setting key cannot be modified: " + existingEntity.getKey());
    }

    var updatedEntity = settingMapper.convert(updatedSetting);
    copyProperties(updatedEntity, existingEntity, "id", "key", "createdDate", "createdBy", "updatedDate", "updatedBy");
    settingRepository.save(existingEntity);
    log.debug("updateSetting:: Setting was updated for key: {}", updatedEntity.getKey());
  }

  /**
   * Delete a {@link Setting} by its id.
   *
   * @param settingId id of the setting to delete
   * @throws NotFoundException when no setting with the given id exists
   */
  @Transactional
  public void deleteById(UUID settingId) {
    var entityToDelete = settingRepository.findById(settingId)
      .orElseThrow(() -> new NotFoundException("Setting not found by id: " + settingId));

    settingRepository.delete(entityToDelete);
    log.debug("deleteSettingById:: Setting was deleted for key: {}", entityToDelete.getKey());
  }
}
