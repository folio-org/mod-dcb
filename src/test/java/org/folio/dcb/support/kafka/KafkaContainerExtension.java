package org.folio.dcb.support.kafka;

import static org.folio.dcb.utils.EntityUtils.TEST_TENANT;
import static org.testcontainers.utility.DockerImageName.parse;

import java.util.List;
import java.util.Properties;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Log4j2
public class KafkaContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String SPRING_PROPERTY_NAME = "spring.kafka.bootstrap-servers";
  private static final DockerImageName KAFKA_IMAGE = parse("apache/kafka-native:3.8.0");
  private static final KafkaContainer CONTAINER = new KafkaContainer(KAFKA_IMAGE)
    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
    .withStartupAttempts(3);

  protected static final List<String> TENANT_TOPICS = List.of(
    "folio.%s.circulation.request".formatted(TEST_TENANT),
    "folio.%s.circulation.loan".formatted(TEST_TENANT),
    "folio.%s.circulation.check-in".formatted(TEST_TENANT)
  );

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_PROPERTY_NAME, CONTAINER.getBootstrapServers());
    createTopics(TENANT_TOPICS);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_PROPERTY_NAME);
    deleteTopics(TENANT_TOPICS);
  }

  @SneakyThrows
  public static void createTopics(List<String> topicNames) {
    var newTopics = topicNames.stream()
      .map(topicName -> new NewTopic(topicName, 1, (short) 1))
      .toList();

    try (var adminClient = getAdminClient()) {
      adminClient.createTopics(newTopics);
    }
  }

  @SneakyThrows
  public static void deleteTopics(List<String> topicNames) {
    try (var adminClient = getAdminClient()) {
      adminClient.deleteTopics(topicNames);
    }
  }

  public static AdminClient getAdminClient() {
    var props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
    props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
    return KafkaAdminClient.create(props);
  }

  public static String getBootstrapServers() {
    if (!CONTAINER.isRunning()) {
      log.error("Kafka container is not running. Returning empty bootstrap servers.");
      return "";
    }
    return CONTAINER.getBootstrapServers();
  }
}
