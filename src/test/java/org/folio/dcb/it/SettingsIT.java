package org.folio.dcb.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dcb.utils.EntityUtils.LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID;
import static org.folio.dcb.utils.EntityUtils.REQUEST_USER_ID;
import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.folio.dcb.utils.EntityUtils.lenderHoldShelfExpirationSetting;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.springframework.test.json.JsonCompareMode.LENIENT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.dcb.domain.dto.HoldShelfExpiryPeriod;
import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.folio.dcb.domain.dto.Metadata;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingsCollection;
import org.folio.dcb.it.base.BaseTenantIntegrationTest;
import org.folio.dcb.support.types.IntegrationTest;
import org.folio.dcb.utils.EntityUtils;
import org.folio.dcb.utils.SettingsApiHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
class SettingsIT extends BaseTenantIntegrationTest {

  @Autowired private JsonMapper jsonMapper;
  protected static SettingsApiHelper settingsApiHelper;

  @BeforeAll
  static void beforeAll() {
    SettingsIT.settingsApiHelper = new SettingsApiHelper(mockMvc);
  }

  @AfterAll
  static void afterAll() {
    SettingsIT.settingsApiHelper = null;
  }

  @Test
  void getSettingById_positive() throws Exception {
    var id = UUID.randomUUID();
    var expectedSetting = lenderHoldShelfExpirationSetting()
      .id(id)
      .version(0)
      .metadata(new Metadata()
        .createdByUserId(REQUEST_USER_ID)
        .updatedByUserId(REQUEST_USER_ID));

    testJdbcHelper.saveDcbSetting(TEST_TENANT, expectedSetting);

    settingsApiHelper.getById(id.toString())
      .andExpect(content().json(asJsonString(expectedSetting), LENIENT))
      .andExpect(jsonPath("$._version").value(0))
      .andExpect(jsonPath("$.version").doesNotExist())
      .andExpect(jsonPath("$.metadata.createdByUserId").value(REQUEST_USER_ID))
      .andExpect(jsonPath("$.metadata.createdDate").exists())
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(REQUEST_USER_ID))
      .andExpect(jsonPath("$.metadata.updatedDate").exists());
  }

  @Test
  void getSettingById_negative_notFoundById() throws Exception {
    var id = UUID.randomUUID().toString();
    settingsApiHelper.getByIdAttempt(id)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message").value("Setting not found by id: " + id))
      .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
      .andExpect(jsonPath("$.errors[0].type").value(-1));
  }

  @Test
  void findByQuery_positive_emptyResult() throws Exception {
    settingsApiHelper.findByQuery("cql.allRecords=1", 100, 0)
      .andExpect(jsonPath("$.items").isArray())
      .andExpect(jsonPath("$.items").isEmpty())
      .andExpect(jsonPath("$.totalRecords").value(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "cql.allRecords=1",
    "id==\"d115a0b6-133d-4148-ac1a-b48c2aa57f57\"",
    "key==\"lender.hold-shelf-expiry-period\"",
  })
  void findByQuery_positive_parameterized(String query) throws Exception {
    var expectedSetting = lenderHoldShelfExpirationSetting()
      .version(0)
      .metadata(new Metadata()
        .createdByUserId(REQUEST_USER_ID)
        .updatedByUserId(REQUEST_USER_ID));

    testJdbcHelper.saveDcbSetting(TEST_TENANT, expectedSetting);
    var expectedResult = new SettingsCollection()
      .items(List.of(expectedSetting))
      .totalRecords(1);

    settingsApiHelper.findByQuery(query, 100, 0)
      .andExpect(content().json(asJsonString(expectedResult), LENIENT))
      .andExpect(jsonPath("$.items[0].metadata.createdDate").exists())
      .andExpect(jsonPath("$.items[0].metadata.updatedDate").exists())
      .andExpect(jsonPath("$.totalRecords").value(1));
  }

  @Test
  void createAndUpdate_positive() throws Exception {
    var setting = EntityUtils.lenderHoldShelfExpirationSetting();
    var createdSettingJson = settingsApiHelper.post(setting)
      .andExpect(content().json(asJsonString(setting), LENIENT))
      .andExpect(jsonPath("$._version").value(0))
      .andExpect(jsonPath("$.metadata.createdByUserId").value(REQUEST_USER_ID))
      .andExpect(jsonPath("$.metadata.createdDate").exists())
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(REQUEST_USER_ID))
      .andExpect(jsonPath("$.metadata.updatedDate").exists())
      .andReturn().getResponse().getContentAsString();

    var createdSetting = jsonMapper.readValue(createdSettingJson, Setting.class);
    assertThat(createdSetting.getMetadata()).isNotNull();
    var createdDate = createdSetting.getMetadata().getCreatedDate();
    var updatedDate = createdSetting.getMetadata().getUpdatedDate();

    assertThat(createdDate).isEqualTo(updatedDate);

    var otherUserId = UUID.randomUUID().toString();
    var newValue = new HoldShelfExpiryPeriod().duration(5).intervalId(IntervalIdEnum.DAYS);
    var newSettingValue = lenderHoldShelfExpirationSetting(newValue);
    settingsApiHelper.putById(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID, newSettingValue, otherUserId);

    var updatedSettingJson = settingsApiHelper.getById(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID)
      .andExpect(jsonPath("$._version").value(1))
      .andExpect(jsonPath("$.metadata.createdByUserId").value(REQUEST_USER_ID))
      .andExpect(jsonPath("$.metadata.createdDate").exists())
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(otherUserId))
      .andExpect(jsonPath("$.metadata.updatedDate").exists())
      .andReturn().getResponse().getContentAsString();

    var updatedSetting = jsonMapper.readValue(updatedSettingJson, Setting.class);
    var updatedSettingMetadata = updatedSetting.getMetadata();
    assertThat(updatedSettingMetadata).isNotNull();
    assertThat(updatedSettingMetadata.getCreatedDate()).isEqualTo(createdDate);
    assertThat(updatedSettingMetadata.getUpdatedDate()).isAfter(updatedSettingMetadata.getCreatedDate());
    assertThat(updatedSettingMetadata.getUpdatedDate()).isAfter(createdDate);
  }

  @Test
  void updateSetting_positive() throws Exception {
    var setting = lenderHoldShelfExpirationSetting();
    testJdbcHelper.saveDcbSetting(TEST_TENANT, setting);

    var otherUserId = UUID.randomUUID().toString();
    var newValue = new HoldShelfExpiryPeriod().duration(1).intervalId(IntervalIdEnum.DAYS);
    var newSetting = lenderHoldShelfExpirationSetting(newValue);
    settingsApiHelper.putById(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID, newSetting, otherUserId);

    settingsApiHelper.getByIdAttempt(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID)
      .andExpect(content().json(asJsonString(newSetting), LENIENT))
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(otherUserId));
  }

  @Test
  void updateSetting_negative_notFoundEntity() throws Exception {
    var id = UUID.randomUUID();
    var newValue = new HoldShelfExpiryPeriod().duration(1).intervalId(IntervalIdEnum.DAYS);
    var newSetting = lenderHoldShelfExpirationSetting(newValue).id(id);
    settingsApiHelper.putByIdAttempt(id.toString(), newSetting, REQUEST_USER_ID)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message").value("Setting not found by id: " + id))
      .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
      .andExpect(jsonPath("$.errors[0].type").value(-1));
  }

  @Test
  void updateSetting_negative_keyModificationNotAllowed() throws Exception {
    var setting = lenderHoldShelfExpirationSetting();
    testJdbcHelper.saveDcbSetting(TEST_TENANT, setting);

    var otherUserId = UUID.randomUUID().toString();
    var newValue = new HoldShelfExpiryPeriod().duration(1).intervalId(IntervalIdEnum.DAYS);
    var newSetting = lenderHoldShelfExpirationSetting(newValue).key("modifiedKey");
    settingsApiHelper.putByIdAttempt(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID, newSetting, otherUserId)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].message").value("Setting key cannot be modified: " + setting.getKey()))
      .andExpect(jsonPath("$.errors[0].code").value("VALIDATION_ERROR"))
      .andExpect(jsonPath("$.errors[0].type").value(-1));

    settingsApiHelper.getByIdAttempt(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID)
      .andExpect(content().json(asJsonString(setting), LENIENT))
      .andExpect(jsonPath("$.metadata.updatedByUserId").value(REQUEST_USER_ID));
  }

  @Test
  void deleteSetting_positive() throws Exception {
    var expectedSetting = lenderHoldShelfExpirationSetting();
    testJdbcHelper.saveDcbSetting(TEST_TENANT, expectedSetting);
    settingsApiHelper.deleteById(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID);
    settingsApiHelper.getByIdAttempt(LENDER_HOLD_SHELF_EXPIRATION_SETTING_ID)
      .andExpect(status().isNotFound());
  }

  @Test
  void deleteSetting_negative_notFound() throws Exception {
    var id = UUID.randomUUID().toString();
    settingsApiHelper.deleteByIdAttempt(id)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.errors[0].message").value("Setting not found by id: " + id))
      .andExpect(jsonPath("$.errors[0].code").value("NOT_FOUND_ERROR"))
      .andExpect(jsonPath("$.errors[0].type").value(-1));
  }
}
