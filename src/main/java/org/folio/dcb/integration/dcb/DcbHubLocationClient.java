package org.folio.dcb.integration.dcb;

import org.folio.dcb.integration.dcb.config.DcbHubClientConfiguration;
import org.folio.dcb.integration.dcb.model.DcbHubLocationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
  name = "dcbHubLocationClient",
  url = "${application.dcb-hub.locations-url}",
  configuration = DcbHubClientConfiguration.class
)
public interface DcbHubLocationClient {

  @GetMapping("/locations")
  DcbHubLocationResponse getLocations(
    @RequestParam("number") int pageNumber,
    @RequestParam("size") int pageSize,
    @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
  );
}
