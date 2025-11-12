package org.folio.dcb.service.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchByName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.dcb.client.feign.HoldingSourcesClient;
import org.folio.dcb.client.feign.HoldingSourcesClient.HoldingSource;
import org.folio.dcb.domain.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbHoldingSourceServiceTest {

  private static final String TEST_SOURCE = "FOLIO";
  private static final String TEST_HOLDING_SOURCE_NAME = "folio";

  @InjectMocks private DcbHoldingSourceService dcbHoldingSourceService;
  @Mock private HoldingSourcesClient holdingSourcesClient;
  @Captor private ArgumentCaptor<HoldingSource> holdingSourceCaptor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(holdingSourcesClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnHoldingSourceWhenExists() {
    var foundSources = ResultList.asSinglePage(dcbHoldingSource());
    var expectedQuery = exactMatchByName(TEST_SOURCE);
    when(holdingSourcesClient.findByQuery(expectedQuery)).thenReturn(foundSources);

    var result = dcbHoldingSourceService.findDcbEntity();

    assertThat(result).isPresent().get()
      .satisfies(value -> assertThat(value.getId()).isNotNull())
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(dcbHoldingSource());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchByName(TEST_SOURCE);
    when(holdingSourcesClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());

    var result = dcbHoldingSourceService.findDcbEntity();

    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateHoldingSource() {
    var expectedSource = dcbHoldingSource();
    when(holdingSourcesClient.createHoldingsRecordSource(holdingSourceCaptor.capture()))
      .thenReturn(expectedSource);

    var result = dcbHoldingSourceService.createDcbEntity();

    assertThat(result)
      .satisfies(value -> assertThat(value.getId()).isNotNull())
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(dcbHoldingSource());

    assertThat(holdingSourceCaptor.getValue())
      .isNotNull()
      .satisfies(actualSource -> assertThat(actualSource.getId()).isNotNull())
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(dcbHoldingSource());
    verify(holdingSourcesClient).createHoldingsRecordSource(any(HoldingSource.class));
  }

  @Test
  void getDefaultValue_positive_shouldReturnHoldingSourceWithDefaultValues() {
    var result = dcbHoldingSourceService.getDefaultValue();
    assertThat(result)
      .satisfies(actualSource -> assertThat(actualSource.getId()).isNotNull())
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(dcbHoldingSource());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingHoldingSource() {
    var expectedQuery = exactMatchByName(TEST_SOURCE);
    var sourcesResult = ResultList.asSinglePage(dcbHoldingSource());
    when(holdingSourcesClient.findByQuery(expectedQuery)).thenReturn(sourcesResult);

    var result = dcbHoldingSourceService.findOrCreateEntity();

    assertThat(result)
      .satisfies(actualSource -> assertThat(actualSource.getId()).isNotNull())
      .usingRecursiveComparison()
      .ignoringFields("id")
      .isEqualTo(dcbHoldingSource());
    verify(holdingSourcesClient, never()).createHoldingsRecordSource(any());
  }

  private static HoldingSource dcbHoldingSource() {
    return HoldingSource.builder()
      .id(UUID.randomUUID().toString())
      .name(TEST_SOURCE)
      .source(TEST_HOLDING_SOURCE_NAME)
      .build();
  }
}
