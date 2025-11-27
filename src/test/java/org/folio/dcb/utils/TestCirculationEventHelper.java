package org.folio.dcb.utils;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class TestCirculationEventHelper {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @SneakyThrows
  public void sendMessage(ProducerRecord<String, Object> record) {
    kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
  }
}
