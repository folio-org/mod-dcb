package org.folio.dcb.utils;

import static org.folio.dcb.it.base.BaseIntegrationTest.defaultHeaders;
import static org.folio.dcb.utils.JsonTestUtils.asJsonString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.dcb.domain.dto.Setting;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RequiredArgsConstructor
public class SettingsApiHelper {

  private final MockMvc mockMvc;

  @SneakyThrows
  public ResultActions getById(String id) {
    return getByIdAttempt(id).andExpect(status().isOk());
  }

  @SneakyThrows
  public ResultActions getByIdAttempt(String id) {
    return mockMvc.perform(
      MockMvcRequestBuilders.get("/dcb/settings/{id}", id)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .accept(APPLICATION_JSON));
  }

  @SneakyThrows
  public ResultActions findByQuery(String query, int limit, int offset) {
    return findByQueryAttempt(query, limit, offset).andExpect(status().isOk());
  }

  @SneakyThrows
  public ResultActions findByQueryAttempt(String query, int limit, int offset) {
    return mockMvc.perform(
      MockMvcRequestBuilders.get("/dcb/settings")
        .queryParam("query", query)
        .queryParam("limit", String.valueOf(limit))
        .queryParam("offset", String.valueOf(offset))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .accept(APPLICATION_JSON));
  }

  @SneakyThrows
  public ResultActions post(Setting setting) {
    return postAttempt(setting).andExpect(status().isCreated());
  }

  @SneakyThrows
  public ResultActions postAttempt(Setting setting) {
    return mockMvc.perform(
      MockMvcRequestBuilders.post("/dcb/settings")
        .content(asJsonString(setting))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .accept(APPLICATION_JSON));
  }

  @SneakyThrows
  public void putById(String id, Setting setting, String userId) {
    putByIdAttempt(id, setting, userId).andExpect(status().isNoContent());
  }

  @SneakyThrows
  public ResultActions putByIdAttempt(String id, Setting setting, String userId) {
    return mockMvc.perform(
      MockMvcRequestBuilders.put("/dcb/settings/{id}", id)
        .content(asJsonString(setting))
        .headers(defaultHeaders(userId))
        .contentType(APPLICATION_JSON)
        .accept(APPLICATION_JSON));
  }

  @SneakyThrows
  public void deleteById(String id) {
    deleteByIdAttempt(id).andExpect(status().isNoContent());
  }

  @SneakyThrows
  public ResultActions deleteByIdAttempt(String id) {
    return mockMvc.perform(
      MockMvcRequestBuilders.delete("/dcb/settings/{id}", id)
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)
        .accept(APPLICATION_JSON));
  }
}
