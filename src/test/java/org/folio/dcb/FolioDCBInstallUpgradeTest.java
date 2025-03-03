package org.folio.dcb;

import org.folio.dcb.controller.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FolioDCBInstallUpgradeTest extends BaseIT {

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
