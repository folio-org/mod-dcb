package org.folio.dcb.integration.invstorage;

import org.folio.dcb.domain.dto.MaterialTypeCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("material-types")
public interface MaterialTypeClient {

  @GetExchange
  MaterialTypeCollection fetchMaterialTypeByQuery(@RequestParam("query") String query);
}
