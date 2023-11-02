package org.folio.dcb.listener.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import static org.folio.dcb.listener.kafka.CirculationEventListener.CHECK_IN_LISTENER_ID;
import static org.folio.dcb.listener.kafka.CirculationEventListener.CHECK_OUT_LOAN_LISTENER_ID;

@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaService {
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  /**
   * Restarts kafka event listeners in mod-dcb application.
   */
  public void restartEventListeners() {
    restartEventListener(CHECK_OUT_LOAN_LISTENER_ID);
    restartEventListener(CHECK_IN_LISTENER_ID);
  }

  private void restartEventListener(String listenerId) {
    log.debug("restartEventListener:: Restarting kafka consumer to start listening topics [id: {}]", listenerId);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(listenerId);
    if (listenerContainer != null) {
      listenerContainer.stop();
      listenerContainer.start();
    } else {
      log.error("Listener container not found [id: {}]", listenerId);
    }
  }
}
