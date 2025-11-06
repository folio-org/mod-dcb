package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.dcb.domain.dto.LoanCollection;
import org.folio.dcb.domain.dto.RenewByIdRequest;
import org.folio.dcb.domain.dto.RenewByIdResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.folio.spring.model.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation", configuration = FeignClientConfiguration.class)
public interface CirculationClient {
  @PostMapping("/requests")
  CirculationRequest createRequest(@RequestBody CirculationRequest circulationRequest);

  @GetMapping("/requests")
  ResultList<CirculationRequest> findByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit);

  @PostMapping("/check-in-by-barcode")
  void checkInByBarcode(@RequestBody CheckInRequest checkInRequest);

  @PostMapping("/check-out-by-barcode")
  void checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);

  @PutMapping("/requests/{requestId}")
  CirculationRequest updateRequest(@PathVariable("requestId") String requestId,
    @RequestBody CirculationRequest circulationRequest);

  @GetMapping("/loans")
  LoanCollection fetchLoanByQuery(@RequestParam("query") String query);

  @PostMapping("/renew-by-id")
  RenewByIdResponse renewById(@RequestBody RenewByIdRequest renewByIdRequest);
}
