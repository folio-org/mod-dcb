package org.folio.dcb.integration.kafka;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.kafka.listener.MessageListenerContainer;
import static org.folio.dcb.integration.kafka.CirculationEventListener.CHECK_IN_LISTENER_ID;
import static org.folio.dcb.integration.kafka.CirculationEventListener.CHECK_OUT_LOAN_LISTENER_ID;
import static org.folio.dcb.integration.kafka.CirculationEventListener.REQUEST_LISTENER_ID;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @InjectMocks
  private KafkaService kafkaService;

  @Mock
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Test
  void restartEventListenersShouldRestartAllDefinedListeners() {
    // TestMate-45dd7ecc27dbf76d9b9b89c85033baae
    // Given
    MessageListenerContainer loanContainer = mock(MessageListenerContainer.class);
    MessageListenerContainer checkInContainer = mock(MessageListenerContainer.class);
    MessageListenerContainer requestContainer = mock(MessageListenerContainer.class);
    when(kafkaListenerEndpointRegistry.getListenerContainer(CHECK_OUT_LOAN_LISTENER_ID)).thenReturn(loanContainer);
    when(kafkaListenerEndpointRegistry.getListenerContainer(CHECK_IN_LISTENER_ID)).thenReturn(checkInContainer);
    when(kafkaListenerEndpointRegistry.getListenerContainer(REQUEST_LISTENER_ID)).thenReturn(requestContainer);
    InOrder inOrder = inOrder(loanContainer, checkInContainer, requestContainer);
    // When
    kafkaService.restartEventListeners();
    // Then
    inOrder.verify(loanContainer).stop();
    inOrder.verify(loanContainer).start();
    inOrder.verify(checkInContainer).stop();
    inOrder.verify(checkInContainer).start();
    inOrder.verify(requestContainer).stop();
    inOrder.verify(requestContainer).start();
  }

    @Test
  void restartEventListenersShouldHandleMissingContainersGracefully() {
    // TestMate-8b6eec42e68f12f44653dac2c23cba01
    // Given
    MessageListenerContainer loanContainer = mock(MessageListenerContainer.class);
    MessageListenerContainer requestContainer = mock(MessageListenerContainer.class);
    when(kafkaListenerEndpointRegistry.getListenerContainer(CHECK_OUT_LOAN_LISTENER_ID)).thenReturn(loanContainer);
    when(kafkaListenerEndpointRegistry.getListenerContainer(CHECK_IN_LISTENER_ID)).thenReturn(null);
    when(kafkaListenerEndpointRegistry.getListenerContainer(REQUEST_LISTENER_ID)).thenReturn(requestContainer);
    InOrder inOrder = inOrder(loanContainer, requestContainer);
    // When
    kafkaService.restartEventListeners();
    // Then
    inOrder.verify(loanContainer).stop();
    inOrder.verify(loanContainer).start();
    inOrder.verify(requestContainer).stop();
    inOrder.verify(requestContainer).start();
  }

}
