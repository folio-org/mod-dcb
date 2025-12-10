package org.folio.dcb.controller;

import org.folio.dcb.domain.dto.RefreshShadowLocationResponse;
import org.folio.dcb.domain.dto.ShadowLocationRefreshBody;
import org.folio.dcb.rest.resource.RefreshShadowLocationsApi;
import org.folio.dcb.service.ShadowLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class RefreshShadowLocationController implements RefreshShadowLocationsApi {

  private final ShadowLocationService shadowLocationService;

  @Override
  public ResponseEntity<RefreshShadowLocationResponse> refreshShadowLocation(
    ShadowLocationRefreshBody requestBody) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(shadowLocationService.createShadowLocations(requestBody));
  }
}
