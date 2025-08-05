package org.folio.dcb.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.dcb.model.DcbHubLocationResponse;

public class DcbHubLocationsGroupingUtil {

  public static Map<AgencyKey, List<LocationCodeNamePair>> groupByAgency(List<DcbHubLocationResponse.Location> locations) {
    if (locations == null || locations.isEmpty()) {
      return Map.of(); // Return empty map if response or content is null
    }
    return locations.stream()
      .collect(Collectors.groupingBy(
        loc -> new AgencyKey(loc.getAgency().getCode(), loc.getAgency().getName()), // Key: code + name
        LinkedHashMap::new,
        Collectors.mapping(
          loc -> new LocationCodeNamePair(loc.getCode(), loc.getName()), // Value: code + name of location
          Collectors.toList()
        )
      ));
  }

  // Record for location code-name pairs
  public record LocationCodeNamePair(String code, String name) {}

  // Record for agency key (code + name)
  public record AgencyKey(String agencyCode, String agencyName) {}

  // Record container for location agencies IDs (institutionId, campusId, libraryId)
  public record LocationAgenciesIds(String institutionId, String campusId, String libraryId) {}

}