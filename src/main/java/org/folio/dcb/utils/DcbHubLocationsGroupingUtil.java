package org.folio.dcb.utils;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dcb.domain.dto.DcbLocation;
import org.folio.dcb.integration.dcb.model.AgencyKey;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DcbHubLocationsGroupingUtil {

  /**
   * Groups the provided list of {@link DcbLocation} by their agency.
   *
   * <p>Each resulting map entry key is an {@link AgencyKey} (agency code and name) and the value
   * is a list of {@link DcbLocation} containing location code and name. Entries with
   * missing or null required fields (null location, null location code/name, null agency,
   * or null agency code/name) are skipped and a warning is logged for each skipped item.
   *
   * @param locations the list of locations to group; may be null or empty
   * @return a {@code LinkedHashMap} preserving insertion order that maps {@link AgencyKey} to
   *         a list of {@link DcbLocation}; returns an empty map if {@code locations}
   *         is null or empty
   */
  public static Map<AgencyKey, List<DcbLocation>> groupByAgency(List<DcbLocation> locations) {
    if (CollectionUtils.isEmpty(locations)) {
      return Collections.emptyMap();
    }

    var result = new LinkedHashMap<AgencyKey, List<DcbLocation>>();
    for (var location : locations) {
      if (location == null) {
        log.warn("groupByAgency:: Location is null, skipping it...");
        continue;
      }

      if (isAnyBlank(location.getCode(), location.getName())) {
        log.warn("groupByAgency:: Location code or name is blank, skipping it...");
        continue;
      }

      var agency = location.getAgency();
      if (agency == null) {
        log.warn("groupByAgency:: Agency is null for location, skipping it...");
        continue;
      }

      var agencyCode = agency.getCode();
      var agencyName = agency.getName();

      if (isAnyBlank(agencyCode, agencyName)) {
        log.warn("groupByAgency:: Agency code or name is blank for location, skipping it...");
        continue;
      }

      var agencyKey = new AgencyKey(agencyCode, agencyName);
      result.computeIfAbsent(agencyKey, k -> new ArrayList<>()).add(location);
    }

    return result;
  }
}
