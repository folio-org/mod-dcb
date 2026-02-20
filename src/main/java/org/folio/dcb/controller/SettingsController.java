package org.folio.dcb.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingsCollection;
import org.folio.dcb.rest.resource.SettingApi;
import org.folio.dcb.service.SettingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettingsController implements SettingApi {

  private final SettingService settingService;

  @Override
  public ResponseEntity<Setting> createDcbSetting(Setting setting) {
    var createdSetting = settingService.createSetting(setting);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSetting);
  }

  @Override
  public ResponseEntity<SettingsCollection> findDcbSettings(String query, Integer limit, Integer offset) {
    var foundSettings = settingService.findByQuery(query, limit, offset);
    return ResponseEntity.ok(new SettingsCollection()
      .items(foundSettings.getResult())
      .totalRecords(foundSettings.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Setting> getDcbSettingById(UUID id) {
    var settingById = settingService.getSettingById(id);
    return ResponseEntity.ok(settingById);
  }

  @Override
  public ResponseEntity<Void> deleteDcbSettingById(UUID id) {
    settingService.deleteSettingById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> updateDcbSettingById(UUID id, Setting setting) {
    settingService.updateSetting(setting);
    return ResponseEntity.noContent().build();
  }
}
