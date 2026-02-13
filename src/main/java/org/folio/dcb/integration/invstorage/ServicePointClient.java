package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.domain.dto.ServicePointRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange("service-points")
public interface ServicePointClient {

  @GetExchange
  ResultList<ServicePointRequest> findByQuery(@RequestParam("query") String query);

  @PostExchange
  ServicePointRequest createServicePoint(@RequestBody ServicePointRequest pickupServicePoint);

  @PutExchange("/{servicePointId}")
  void updateServicePointById(
    @PathVariable String servicePointId,
    @RequestBody ServicePointRequest servicePointRequest);
}
