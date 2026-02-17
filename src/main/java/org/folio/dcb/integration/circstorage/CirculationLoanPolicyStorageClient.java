package org.folio.dcb.integration.circstorage;

import org.folio.dcb.domain.dto.LoanPolicy;
import org.folio.dcb.domain.dto.LoanPolicyCollection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("loan-policy-storage")
public interface CirculationLoanPolicyStorageClient {

  @GetExchange("/loan-policies")
  LoanPolicyCollection findByQuery(@RequestParam("query") String query);

  @GetExchange("/loan-policies/{id}")
  LoanPolicy getById(@PathVariable String id);
}
