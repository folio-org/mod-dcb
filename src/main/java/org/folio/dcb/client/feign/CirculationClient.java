package org.folio.dcb.client.feign;

import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@FeignClient(name = "circulation", configuration = FeignClientConfiguration.class)
public interface CirculationClient {
  @PostMapping("/requests")
  CirculationRequest createRequest(@RequestBody CirculationRequest circulationRequest);

  @PostMapping("/check-in-by-barcode")
  @ResponseStatus(HttpStatus.OK)
  void checkInByBarcode(@RequestBody CheckInRequest checkInRequest);

  @PostMapping("/check-out-by-barcode")
  @ResponseStatus(HttpStatus.OK)
  void checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);
}
