package org.folio.dcb.utils;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class TestCirculationEventHelper {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @SneakyThrows
  public void sendMessage(ProducerRecord<String, Object> producerRecord) {
    log.info("Sending message to topic: {}", producerRecord.topic());
    kafkaTemplate.send(producerRecord).get(5, TimeUnit.SECONDS);
  }
}
