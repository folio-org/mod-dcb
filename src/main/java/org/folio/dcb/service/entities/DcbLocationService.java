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
import org.folio.dcb.client.feign.LocationsClient;
import org.folio.dcb.client.feign.LocationsClient.LocationDTO;
import org.folio.dcb.utils.CqlQuery;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DcbLocationService implements DcbEntityService<LocationDTO> {

  private final LocationsClient locationsClient;
  private final DcbCampusService dcbCampusService;
  private final DcbLibraryService dcbLibraryService;
  private final DcbInstitutionService dcbInstitutionService;
  private final DcbServicePointService dcbServicePointService;

  @Override
  public Optional<LocationDTO> findDcbEntity() {
    var locationsByQuery = locationsClient.findByQuery(CqlQuery.exactMatchByName(NAME));
    return findFirstValue(locationsByQuery);
  }

  @Override
  public LocationDTO createDcbEntity() {
    var institutionId = dcbInstitutionService.findOrCreateEntity().getId();
    var campusId = dcbCampusService.findOrCreateEntity().getId();
    var libraryId = dcbLibraryService.findOrCreateEntity().getId();
    var servicePointId = dcbServicePointService.findOrCreateEntity().getId();
    return createLocation(libraryId, campusId, institutionId, servicePointId);
  }

  @Override
  public LocationDTO getDefaultValue() {
    return getDcbLocation(LIBRARY_ID, CAMPUS_ID, INSTITUTION_ID, SERVICE_POINT_ID);
  }

  private LocationDTO createLocation(String libraryId, String campusId,
    String institutionId, String servicePointId) {
    log.info("createLocation:: creating a new DCB Location");
    var dcbLocation = getDcbLocation(libraryId, campusId, institutionId, servicePointId);

    var createdLocation = locationsClient.createLocation(dcbLocation);
    log.info("createLocation:: DCB Location created");
    return createdLocation;
  }

  private static LocationDTO getDcbLocation(String libraryId, String campusId,
    String institutionId, String servicePointId) {
    return LocationDTO.builder()
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
