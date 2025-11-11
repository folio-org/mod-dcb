package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.CAMPUS_ID;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.INSTITUTION_ID;
import static org.folio.dcb.utils.DCBConstants.NAME;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.client.feign.LocationUnitClient;
import org.folio.dcb.client.feign.LocationUnitClient.LocationUnit;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbCampusService implements DcbEntityService<LocationUnit> {

  private final LocationUnitClient locationUnitClient;
  private final DcbInstitutionService dcbInstitutionService;

  @Override
  public Optional<LocationUnit> findDcbEntity() {
    var librariesByQuery = locationUnitClient.findCampusesByQuery(exactMatchByName(NAME));
    return findFirstValue(librariesByQuery);
  }

  @Override
  public LocationUnit createDcbEntity() {
    var dcbInstitution = dcbInstitutionService.findOrCreateEntity();
    return createDcbCampus(dcbInstitution.getId());
  }

  private LocationUnit createDcbCampus(String institutionId) {
    log.debug("createDcbEntity:: creating a new DCB Campus");
    var dcbCampus = getDcbCampus(institutionId);

    var createdCampus = locationUnitClient.createCampus(dcbCampus);
    log.info("createDcbEntity:: DCB Campus created");
    return createdCampus;
  }

  @Override
  public LocationUnit getDefaultValue() {
    return getDcbCampus(INSTITUTION_ID);
  }

  private static LocationUnit getDcbCampus(String institutionId) {
    return LocationUnit.builder()
      .id(CAMPUS_ID)
      .institutionId(institutionId)
      .name(NAME)
      .code(CODE)
      .build();
  }
}
