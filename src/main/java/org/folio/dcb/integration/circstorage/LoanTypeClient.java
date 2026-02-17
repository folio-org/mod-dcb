package org.folio.dcb.integration.circstorage;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.circstorage.model.LoanType;

@HttpExchange("loan-types")
public interface LoanTypeClient {

  @GetExchange(accept = APPLICATION_JSON_VALUE)
  ResultList<LoanType> findByQuery(@RequestParam("query") String query);

  @PostExchange
  LoanType createLoanType(@RequestBody LoanType loanType);
}

