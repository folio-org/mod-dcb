package org.folio.dcb.listener.kafka;

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
  /**
   * Restarts kafka event listeners in mod-dcb application.
   */
  public void restartEventListeners() {
    restartEventListener();
  }

  private void restartEventListener() {
    log.info("restartEventListener:: Restarting kafka consumer to start listening topics [id: {}]", CHECK_IN_LISTENER_ID);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(CHECK_IN_LISTENER_ID);
    if (listenerContainer != null) {
      listenerContainer.stop();
      listenerContainer.start();
    } else {
      log.error("restartEventListener:: Listener container not found [id: {}]", CHECK_IN_LISTENER_ID);
    }
  }
}
