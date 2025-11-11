package org.folio.dcb.service.entities;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.dcb.utils.CqlQuery.exactMatchById;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.dcb.client.feign.CancellationReasonClient;
import org.folio.dcb.client.feign.CancellationReasonClient.CancellationReason;
import org.folio.spring.model.ResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DcbCancellationReasonServiceTest {

  private static final String TEST_CANCELLATION_REASON_ID = "50ed35b2-1397-4e83-a76b-642adf91ca2a";
  private static final String TEST_CANCELLATION_REASON_NAME = "DCB Cancelled";

  @InjectMocks private DcbCancellationReasonService dcbCancellationReasonService;
  @Mock private CancellationReasonClient cancellationReasonClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(cancellationReasonClient);
  }

  @Test
  void findDcbEntity_positive_shouldReturnCancellationReasonWhenExists() {
    var foundReasons = ResultList.asSinglePage(dcbCancellationReason());
    var expectedQuery = exactMatchById(TEST_CANCELLATION_REASON_ID);
    when(cancellationReasonClient.findByQuery(expectedQuery)).thenReturn(foundReasons);

    var result = dcbCancellationReasonService.findDcbEntity();

    assertThat(result).contains(dcbCancellationReason());
  }

  @Test
  void findDcbEntity_positive_shouldReturnEmptyWhenNotExists() {
    var expectedQuery = exactMatchById(TEST_CANCELLATION_REASON_ID);
    when(cancellationReasonClient.findByQuery(expectedQuery)).thenReturn(ResultList.empty());
    var result = dcbCancellationReasonService.findDcbEntity();
    assertThat(result).isEmpty();
  }

  @Test
  void createDcbEntity_positive_shouldCreateCancellationReason() {
    var expectedReason = dcbCancellationReason();
    when(cancellationReasonClient.createCancellationReason(expectedReason)).thenReturn(expectedReason);
    var result = dcbCancellationReasonService.createDcbEntity();
    assertThat(result).isEqualTo(dcbCancellationReason());
  }

  @Test
  void getDefaultValue_positive_shouldReturnCancellationReasonWithDefaultValues() {
    var result = dcbCancellationReasonService.getDefaultValue();
    assertThat(result).isEqualTo(dcbCancellationReason());
  }

  @Test
  void findOrCreateEntity_positive_shouldReturnExistingCancellationReason() {
    var expectedQuery = exactMatchById(TEST_CANCELLATION_REASON_ID);
    var reasonsResult = ResultList.asSinglePage(dcbCancellationReason());
    when(cancellationReasonClient.findByQuery(expectedQuery)).thenReturn(reasonsResult);

    var result = dcbCancellationReasonService.findOrCreateEntity();

    assertThat(result).isEqualTo(dcbCancellationReason());
    verify(cancellationReasonClient, never()).createCancellationReason(any());
  }

  private static CancellationReason dcbCancellationReason() {
    return CancellationReason.builder()
      .id(TEST_CANCELLATION_REASON_ID)
      .description(TEST_CANCELLATION_REASON_NAME)
      .name(TEST_CANCELLATION_REASON_NAME)
      .build();
  }
}
