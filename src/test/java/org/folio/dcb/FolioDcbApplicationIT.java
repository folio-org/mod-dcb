package org.folio.dcb;


import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.extension.EnablePostgres;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@org.testcontainers.junit.jupiter.Testcontainers
@WireMockTest(httpPort = 9999)

class FolioDcbApplicationIT {

  private static final Logger LOG = LoggerFactory.getLogger(FolioDcbApplicationIT.class);
  /** Container logging, requires log4j-slf4j2-impl in test scope */
  private static final boolean IS_LOG_ENABLED = true;
  public static final String TENANT = "diku";
  private static final Network NETWORK = Network.newNetwork();

  private static final KafkaContainer KAFKA =
    new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.1"))
      .withNetwork(NETWORK)
      .withNetworkAliases("ourkafka");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:16-alpine")
      .withNetwork(NETWORK)
      .withNetworkAliases("mypostgres")
      .withExposedPorts(5432)
      .withUsername("username")
      .withPassword("password")
      .withDatabaseName("postgres");

  @Container
  private static final GenericContainer<?> MOD_DCB =
    new GenericContainer<>(
      new ImageFromDockerfile("mod-dcb").withFileFromPath(".", Path.of(".")))
      .dependsOn(KAFKA, POSTGRES)
      .withNetwork(NETWORK)
      .withNetworkAliases("mod-dcb")
      .withExposedPorts(8081)
      .withAccessToHost(true)
      .withEnv("DB_HOST", "mypostgres")
      .withEnv("DB_PORT", "5432")
      .withEnv("DB_USERNAME", "username")
      .withEnv("DB_PASSWORD", "password")
      .withEnv("DB_DATABASE", "postgres")
      .withEnv("KAFKA_HOST", "ourkafka")
      .withEnv("SYSTEM_USER_NAME", "dcb-system-user")
      .withEnv("SYSTEM_USER_PASSWORD", "dcb-system-user");

  @BeforeAll
  static void beforeAll() {
    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + MOD_DCB.getHost() + ":" + MOD_DCB.getFirstMappedPort();
    if (IS_LOG_ENABLED) {
      MOD_DCB.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams());
    }

    // Okapi Mock
    Testcontainers.exposeHostPorts(9999);
    WireMock.stubFor(WireMock.get("/users?query=username%3D%3Ddcb-system-user")
      .willReturn(WireMock.ok().withBody("{\"users\":[{\"username\":\"dcb-system-user\"}]}")));
  }

  @BeforeEach
  void beforeEach() {
    RestAssured.requestSpecification = null;  // unset X-Okapi-Tenant etc.
  }

  @Test
  void health() {
    // don't set headers like X-Okapi-Tenant

    when().
      get("/admin/health")
      .then()
      .statusCode(200)
      .body("status", is("UP"))
      .contentType(ContentType.JSON);
  }

  @Test
  void installAndUpgrade() {
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader(XOkapiHeaders.URL, "http://host.testcontainers.internal:9999")
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
