package org.folio.dcb.utils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.folio.dcb.domain.DcbAgencyKey;
import org.folio.dcb.domain.dto.DcbAgency;
import org.folio.dcb.domain.dto.DcbLocation;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class DcbHubLocationsGroupingUtilTest {

  @Test
  void groupByAgency_positive_nullLocationsList() {
    var result = DcbHubLocationsGroupingUtil.groupByAgency(null);
    assertThat(result).isEmpty();
  }

  @Test
  void groupByAgency_positive_emptyLocationsList() {
    var result = DcbHubLocationsGroupingUtil.groupByAgency(Collections.emptyList());
    assertThat(result).isEmpty();
  }

  @Test
  void groupByAgency_positive_validLocations() {
    var locations = List.of(
      dcbLocation("LOC1", "Location 1", dcbAgency("AG1", "Agency 1")),
      dcbLocation("LOC2", "Location 2", dcbAgency("AG1", "Agency 1")),
      dcbLocation("LOC3", "Location 3", dcbAgency("AG2", "Agency 2"))
    );

    var result = DcbHubLocationsGroupingUtil.groupByAgency(locations);

    assertThat(result).hasSize(2);
    DcbAgencyKey agency1Key = new DcbAgencyKey("AG1", "Agency 1");
    DcbAgencyKey agency2Key = new DcbAgencyKey("AG2", "Agency 2");

    assertThat(result.get(agency1Key)).hasSize(2);
    assertThat(result.get(agency2Key)).hasSize(1);
  }

  @DisplayName("groupByAgency_positive_invalidValues")
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideInvalidLocations")
  void groupByAgency_positive_invalidValues(
    @SuppressWarnings("unused") String name, List<DcbLocation> locations) {

    var result = DcbHubLocationsGroupingUtil.groupByAgency(locations);
    assertThat(result).isEmpty();
  }

  @Test
  void groupByAgency_positive_shouldPreserveInsertionOrder() {
    var locations = List.of(
      dcbLocation("LOC1", "Location 1", dcbAgency("AG1", "Agency 1")),
      dcbLocation("LOC2", "Location 2", dcbAgency("AG2", "Agency 2")),
      dcbLocation("LOC3", "Location 3", dcbAgency("AG3", "Agency 3"))
    );

    var result = DcbHubLocationsGroupingUtil.groupByAgency(locations);

    var keys = result.keySet().stream().toList();
    assertThat(keys).containsExactly(
      new DcbAgencyKey("AG1", "Agency 1"),
      new DcbAgencyKey("AG2", "Agency 2"),
      new DcbAgencyKey("AG3", "Agency 3")
    );
  }

  private static Stream<Arguments> provideInvalidLocations() {
    return Stream.of(
      arguments("null value", singletonList(null)),
      arguments("null loc name", List.of(dcbLocation(null, "LOC-1", dcbAgency("AG1", "Agency 1")))),
      arguments("null loc code", List.of(dcbLocation("Test Location", null, dcbAgency("AG1", "Agency 1")))),
      arguments("null agency", List.of(dcbLocation("Test Location", "LOC-1", null))),
      arguments("null agency code", List.of(dcbLocation("Test Location", "LOC-1", dcbAgency(null, "Agency 1")))),
      arguments("null agency name", List.of(dcbLocation("Test Location", "LOC-1", dcbAgency("AG1", null))))
    );
  }

  private static DcbLocation dcbLocation(String code, String name, DcbAgency agency) {
    return new DcbLocation().code(code).name(name).agency(agency);
  }

  private static DcbAgency dcbAgency(String code, String name) {
    return new DcbAgency().name(name).code(code);
  }
}
