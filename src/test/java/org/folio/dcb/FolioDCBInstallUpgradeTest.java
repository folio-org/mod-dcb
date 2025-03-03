package org.folio.dcb;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.folio.dcb.controller.BaseIT;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FolioDCBInstallUpgradeTest extends BaseIT {

  @Test
  void installAndUpgrade() throws Exception {

    // install from scratch
    postTenant("{ \"module_to\": \"999999.0.0\" }");

    // migrate from 0.0.0 to current version, installation and migration should be idempotent
    postTenant("{ \"module_to\": \"999999.0.0\", \"module_from\": \"0.0.0\" }");
  }

  private void postTenant(String body) throws Exception {
    mockMvc.perform(post("/_/tenant")
        .content(body)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }


}
