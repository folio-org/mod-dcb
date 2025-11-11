package org.folio.dcb.service.entities;

import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.folio.dcb.utils.DCBConstants.CAMPUS_ID;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.LIBRARY_ID;
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
public class DcbLibraryService implements DcbEntityService<LocationUnit> {

  private final DcbCampusService dcbCampusService;
  private final LocationUnitClient locationUnitClient;

  @Override
  public Optional<LocationUnit> findDcbEntity() {
    var librariesByQuery = locationUnitClient.findLibrariesByQuery(exactMatchByName(NAME));
    return findFirstValue(librariesByQuery);
  }

  @Override
  public LocationUnit createDcbEntity() {
    var dcbCampus = dcbCampusService.findOrCreateEntity();
    log.debug("createDcbEntity:: creating DCB library");
    var dcbLibrary = getDcbLibrary(dcbCampus.getId());

    var createdUmbrellaLibrary = locationUnitClient.createLibrary(dcbLibrary);
    log.info("createDcbEntity:: DCB library created");
    return createdUmbrellaLibrary;
  }

  @Override
  public LocationUnit getDefaultValue() {
    return getDcbLibrary(CAMPUS_ID);
  }

  private static LocationUnit getDcbLibrary(String campusId) {
    return LocationUnit.builder()
      .campusId(campusId)
      .id(LIBRARY_ID)
      .name(NAME)
      .code(CODE)
      .build();
  }
}
