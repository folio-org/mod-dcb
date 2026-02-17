package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.ResultList;
import org.folio.dcb.integration.invstorage.model.InstanceType;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("instance-types")
public interface InstanceTypeClient {

  @GetExchange
  ResultList<InstanceType> findByQuery(@RequestParam("query") String query);

  @PostExchange
  void createInstanceType(@RequestBody InstanceType instanceType);

}
