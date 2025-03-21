package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.LoanPolicy;
import org.folio.dcb.domain.dto.LoanPolicyCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-policy-storage", configuration = FeignClientConfiguration.class)
public interface CirculationLoanPolicyStorageClient {
  @GetMapping("/loan-policies")
  LoanPolicyCollection fetchLoanPolicyByQuery(@RequestParam("query") String query);

  @GetMapping("/loan-policies/{id}")
  LoanPolicy fetchLoanPolicyById(@RequestParam("id") String id);
}
