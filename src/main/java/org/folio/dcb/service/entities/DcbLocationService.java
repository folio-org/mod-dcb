package org.folio.dcb.service.entities;

import static java.util.Collections.singletonList;
import static org.folio.dcb.utils.DCBConstants.CAMPUS_ID;
import static org.folio.dcb.utils.DCBConstants.CODE;
import static org.folio.dcb.utils.DCBConstants.INSTITUTION_ID;
import static org.folio.dcb.utils.DCBConstants.LIBRARY_ID;
import static org.folio.dcb.utils.DCBConstants.LOCATION_ID;
import static org.folio.dcb.utils.DCBConstants.NAME;
import static org.folio.dcb.utils.DCBConstants.SERVICE_POINT_ID;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.integration.invstorage.LocationsClient;
import org.folio.dcb.integration.invstorage.model.Location;
import org.folio.dcb.utils.CqlQuery;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbLocationService implements DcbEntityService<Location> {

  private final LocationsClient locationsClient;
  private final DcbCampusService dcbCampusService;
  private final DcbLibraryService dcbLibraryService;
  private final DcbInstitutionService dcbInstitutionService;
  private final DcbServicePointService dcbServicePointService;

  @Override
  public Optional<Location> findDcbEntity() {
    var query = CqlQuery.exactMatchByName(NAME).getQuery();
    var locationsByQuery = locationsClient.findByQuery(query);
    return findFirstValue(locationsByQuery);
  }

  @Override
  public Location createDcbEntity() {
    var institutionId = dcbInstitutionService.findOrCreateEntity().getId();
    var campusId = dcbCampusService.findOrCreateEntity().getId();
    var libraryId = dcbLibraryService.findOrCreateEntity().getId();
    var servicePointId = dcbServicePointService.findOrCreateEntity().getId();
    return createLocation(libraryId, campusId, institutionId, servicePointId);
  }

  @Override
  public Location getDefaultValue() {
    return getDcbLocation(LIBRARY_ID, CAMPUS_ID, INSTITUTION_ID, SERVICE_POINT_ID);
  }

  private Location createLocation(String libraryId, String campusId,
    String institutionId, String servicePointId) {
    log.debug("createLocation:: creating a new DCB Location");
    var dcbLocation = getDcbLocation(libraryId, campusId, institutionId, servicePointId);

    var createdLocation = locationsClient.createLocation(dcbLocation);
    log.info("createLocation:: DCB Location created");
    return createdLocation;
  }

  private static Location getDcbLocation(String libraryId, String campusId,
    String institutionId, String servicePointId) {
    return Location.builder()
      .id(LOCATION_ID)
      .campusId(campusId)
      .libraryId(libraryId)
      .institutionId(institutionId)
      .primaryServicePoint(servicePointId)
      .code(CODE)
      .name(NAME)
      .servicePointIds(singletonList(servicePointId))
      .build();
  }
}
