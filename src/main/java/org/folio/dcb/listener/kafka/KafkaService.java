package org.folio.dcb.listener.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import static org.folio.dcb.listener.CirculationCheckInEventListener.CHECK_IN_LISTENER_ID;

@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaService {
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @RequiredArgsConstructor
  @AllArgsConstructor
  @Getter
  public enum Topic {
    CHECK_IN("CHECK_IN");
    private String nameSpace;
    private final String topicName;
  }

  /**
   * Restarts kafka event listeners in mod-dcb application.
   */
  public void restartEventListeners() {
    restartEventListener(CHECK_IN_LISTENER_ID);
  }

  private void restartEventListener(String listenerId) {
    log.info("Restarting kafka consumer to start listening topics [id: {}]", listenerId);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(listenerId);
    if (listenerContainer != null) {
      listenerContainer.stop();
      listenerContainer.start();
    } else {
      log.error("Listener container not found [id: {}]", listenerId);
    }
  }
}
