package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.LoanTypeCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loan-types", configuration = FeignClientConfiguration.class)
public interface LoanTypeClient {
    @GetMapping
    LoanTypeCollection fetchLoanTypeByQuery(@RequestParam("query") String query);
  }

