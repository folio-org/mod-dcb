package org.folio.dcb.client.feign;

import org.folio.dcb.client.feign.config.DcbClientConfiguration;
import org.folio.dcb.domain.dto.CheckInRequest;
import org.folio.dcb.domain.dto.CheckOutRequest;
import org.folio.dcb.domain.dto.CirculationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "circulation", configuration = DcbClientConfiguration.class)
public interface CirculationClient {
  @PostMapping("/requests")
  CirculationRequest createRequest(@RequestBody CirculationRequest circulationRequest);

  @PostMapping("/check-in-by-barcode")
  void checkInByBarcode(@RequestBody CheckInRequest checkInRequest);

  @PostMapping("/check-out-by-barcode")
  void checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);
}
