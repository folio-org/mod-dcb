package org.folio.dcb.integration.kafka;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dcb.integration.kafka.model.EventData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TransactionHelperTest {

    @Test
  void parseLoanEventShouldCorrectMapCheckOutAndCheckInActions() {
    // TestMate-bae863f11690bb7c8697340f2df3b944
    // Given
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String checkOutPayload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedout"
          }
        }
      }
      """.formatted(itemId);
    String checkInPayload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "checkedin"
          }
        }
      }
      """.formatted(itemId);
    String unknownActionPayload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "action": "other"
          }
        }
      }
      """.formatted(itemId);
    // When
    EventData checkOutResult = TransactionHelper.parseLoanEvent(checkOutPayload);
    EventData checkInResult = TransactionHelper.parseLoanEvent(checkInPayload);
    EventData unknownResult = TransactionHelper.parseLoanEvent(unknownActionPayload);
    // Then
    assertThat(checkOutResult.getType()).isEqualTo(EventData.EventType.CHECK_OUT);
    assertThat(checkInResult.getType()).isEqualTo(EventData.EventType.CHECK_IN);
    assertThat(unknownResult.getType()).isNull();
  }

    @Test
  void parseLoanEventShouldDetermineDcbStatusBasedOnIsDcbField() {
    // TestMate-cfdf83db95a1c3b069df2d438e83c809
    // Given
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String payloadMissingIsDcb = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s"
          }
        }
      }
      """.formatted(itemId);
    String payloadIsDcbTrue = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "isDcb": true
          }
        }
      }
      """.formatted(itemId);
    String payloadIsDcbFalse = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "isDcb": false
          }
        }
      }
      """.formatted(itemId);
    // When
    EventData resultMissing = TransactionHelper.parseLoanEvent(payloadMissingIsDcb);
    EventData resultTrue = TransactionHelper.parseLoanEvent(payloadIsDcbTrue);
    EventData resultFalse = TransactionHelper.parseLoanEvent(payloadIsDcbFalse);
    // Then
    assertThat(resultMissing.isDcb()).isTrue();
    assertThat(resultTrue.isDcb()).isTrue();
    assertThat(resultFalse.isDcb()).isFalse();
  }

    @Test
  void parseLoanEventShouldPopulateLoanStatusWhenPresent() {
    // TestMate-bcbe206fc559de7d897da32b27a7e5ab
    // Given
    String itemId = "8db107f5-12aa-479f-9c07-39e7c9cf2e4d";
    String expectedStatus = "Open - Checked out";
    String payload = """
      {
        "type": "UPDATED",
        "data": {
          "new": {
            "itemId": "%s",
            "status": {
              "name": "%s"
            }
          }
        }
      }
      """.formatted(itemId, expectedStatus);
    // When
    EventData result = TransactionHelper.parseLoanEvent(payload);
    // Then
    assertThat(result.getItemId()).isEqualTo(itemId);
    assertThat(result.getLoanStatus()).isEqualTo(expectedStatus);
  }

}
