package org.folio.dcb.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.folio.dcb.utils.EntityUtils.DCB_TRANSACTION_ID;
import static org.folio.dcb.utils.EntityUtils.createDcbItem;
import static org.folio.dcb.utils.EntityUtils.createDcbPatron;
import static org.folio.dcb.utils.EntityUtils.createDcbTransaction;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionApiControllerTest extends BaseIT {

  @Test
  void createLendingCirculationRequestTest() throws Exception {
    this.mockMvc.perform(
        post("/transactions/" + DCB_TRANSACTION_ID)
          .content(asJsonString(createDcbTransaction()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("CREATED"))
      .andExpect(jsonPath("$.item").value(createDcbItem()))
      .andExpect(jsonPath("$.patron").value(createDcbPatron()));
  }

}
