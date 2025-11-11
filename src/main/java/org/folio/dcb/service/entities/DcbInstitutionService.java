package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
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
public class DcbInstitutionService implements DcbEntityService<LocationUnit> {

  private final LocationUnitClient locationUnitClient;

  @Override
  public Optional<LocationUnit> findDcbEntity() {
    var librariesByQuery = locationUnitClient.findInstitutionsByQuery(exactMatchByName(NAME));
    return findFirstValue(librariesByQuery);
  }

  @Override
  public LocationUnit createDcbEntity() {
    log.debug("createDcbEntity:: Creating a new DCB Institution");
    var dcbInstitution = getDcbInstitution();

    var createdInstitution = locationUnitClient.createInstitution(dcbInstitution);
    log.info("createDcbEntity:: DCB institution created");
    return createdInstitution;
  }

  @Override
  public LocationUnit getDefaultValue() {
    return getDcbInstitution();
  }

  private static LocationUnit getDcbInstitution() {
    return LocationUnit.builder()
      .id(INSTITUTION_ID)
      .name(NAME)
      .code(CODE)
      .build();
  }
}
