package org.folio.dcb.controller;

import org.folio.dcb.domain.dto.TransactionStatus;
import org.folio.dcb.domain.entity.Transactions;
import org.folio.dcb.repository.TransactionsRepository;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TransactionApiControllerTest {
  public static final String TENANT = "test";
  @Autowired
  protected MockMvc mockMvc;
  @MockBean
  private TransactionsRepository transactionsRepository;

  @Test
  public void getTransactionStatusSuccessTest() throws Exception {
    var transactionIdUnique = UUID.randomUUID();
    when(transactionsRepository.findById(transactionIdUnique))
      .thenReturn(Optional.of(getDefaultTransactionsInstance(transactionIdUnique)));

    mockMvc.perform(get(format("/transactions/%s/status", transactionIdUnique))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  public void getNoTransactionContentFoundExceptionTest() throws Exception {
    var transactionIdUnique = UUID.randomUUID();
    when(transactionsRepository.findById(transactionIdUnique))
      .thenReturn(Optional.empty());

    mockMvc.perform(get(format("/transactions/%s/status", transactionIdUnique))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  private Transactions getDefaultTransactionsInstance(UUID transactionIdUnique) {
    Transactions trn = new Transactions();
    trn.setId(transactionIdUnique);
    trn.setStatus(TransactionStatus.StatusEnum.OPEN);

    return trn;
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));

    return httpHeaders;
  }

}
