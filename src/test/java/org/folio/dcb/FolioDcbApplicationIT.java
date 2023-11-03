package org.folio.dcb;


import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

@Testcontainers
public class FolioDcbApplicationIT {

  private static final Logger LOG = LoggerFactory.getLogger(FolioDcbApplicationIT.class);
  /** Container logging, requires log4j-slf4j-impl in test scope */
  private static final boolean IS_LOG_ENABLED = false;
  protected static final String TOKEN = "test_token";
  public static final String TENANT = "diku";
  private static final Network NETWORK = Network.newNetwork();

  @Container
  public static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:12-alpine")
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
      .dependsOn(POSTGRES)
      .withNetwork(NETWORK)
      .withNetworkAliases("mod-dcb")
      .withExposedPorts(8087);

  @BeforeAll
  static void beforeAll() {
    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + MOD_DCB.getHost() + ":" + MOD_DCB.getFirstMappedPort();
    if (IS_LOG_ENABLED) {
      MOD_DCB.followOutput(new Slf4jLogConsumer(LOG).withSeparateOutputStreams());
    }
  }

  @BeforeEach
  void beforeEach() {
    RestAssured.requestSpecification = null;  // unset X-Okapi-Tenant etc.
  }

  @Test
  void health() {
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader(XOkapiHeaders.URL, "http://mod-dcb:8087")  // returns 404 for all other APIs
      .addHeader(XOkapiHeaders.TENANT, TENANT)
      .addHeader(XOkapiHeaders.TOKEN, TOKEN)
      .addHeader(XOkapiHeaders.USER_ID, "08d51c7a-0f36-4f3d-9e35-d285612a23df")
      .setContentType(ContentType.JSON)
      .build();

    when().
      get("/admin/health")
      .then()
      .statusCode(200)
      .body(is("UP"))
      .contentType(ContentType.JSON);
  }

}
