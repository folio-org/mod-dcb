package org.folio.dcb;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import org.folio.dcb.support.postgres.PostgresContainerExtension;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FolioDcbApplicationIT {

  private static final Logger LOG = LoggerFactory.getLogger(FolioDcbApplicationIT.class);
  /** Container logging, requires log4j-slf4j2-impl in test scope */
  private static final boolean IS_LOG_ENABLED = true;
  public static final String TENANT = "diku";
  private static final Network NETWORK = Network.newNetwork();

  private static final KafkaContainer KAFKA =
    new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
      .withNetwork(NETWORK)
      .withNetworkAliases("ourkafka")
      .withStartupAttempts(3);

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>(PostgresContainerExtension.IMAGE_NAME)
      .withNetwork(NETWORK)
      .withNetworkAliases("mypostgres")
      .withUsername("username")
      .withPassword("password")
      .withDatabaseName("postgres");

  @Container
  private static final NginxContainer<?> OKAPI =  // mock okapi and other modules
    new NginxContainer<>("nginx:alpine-slim")
      .withNetwork(NETWORK)
      .withNetworkAliases("okapi")
      .withCopyToContainer(Transferable.of("""
          server {
            default_type application/json;
            return 201 '{"totalRecords": 1}';
          }
          """), "/etc/nginx/conf.d/default.conf");

  @Container
  private static final GenericContainer<?> MOD_DCB =
    new GenericContainer<>(
        new ImageFromDockerfile("mod-dcb").withFileFromPath(".", Path.of(".")))
      .dependsOn(KAFKA, POSTGRES, OKAPI)
      .withNetwork(NETWORK)
      .withNetworkAliases("mod-dcb")
      .withExposedPorts(8081)
      .withEnv("DB_HOST", "mypostgres")
      .withEnv("DB_PORT", "5432")
      .withEnv("DB_USERNAME", "username")
      .withEnv("DB_PASSWORD", "password")
      .withEnv("DB_DATABASE", "postgres")
      .withEnv("KAFKA_HOST", "ourkafka")
      .withEnv("FOLIO_SYSTEMUSER_ENABLED", "false");

  @BeforeAll
  static void beforeAll() {
    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + MOD_DCB.getHost() + ":" + MOD_DCB.getFirstMappedPort();
    if (IS_LOG_ENABLED) {
      MOD_DCB.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams().withPrefix("DCB"));
      OKAPI.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams().withPrefix("OKAPI"));
    }
  }

  @BeforeEach
  void beforeEach() {
    RestAssured.requestSpecification = null;  // unset X-Okapi-Tenant etc.
  }

  @Test
  void health() {
    // don't set headers like X-Okapi-Tenant

    when()
      .get("/admin/health")
      .then()
      .statusCode(200)
      .body("status", is("UP"))
      .contentType(ContentType.JSON);
  }

  @Test
  void installAndUpgrade() {
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader(XOkapiHeaders.URL, "http://okapi:80")
      .addHeader(XOkapiHeaders.TENANT, TENANT)
      .setContentType(ContentType.JSON)
      .build();

    // install from scratch
    postTenant("{ \"module_to\": \"999999.0.0\" }");

    // migrate from 0.0.0 to current version, installation and migration should be idempotent
    postTenant("{ \"module_to\": \"999999.0.0\", \"module_from\": \"0.0.0\" }");
  }

  private void postTenant(String body) {
    given()
      .body(body)
      .when()
      .post("/_/tenant")
      .then()
      .statusCode(204);
  }
}
