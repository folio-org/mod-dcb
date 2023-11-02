package org.folio.dcb;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.*;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class FolioDcbApplicationIntegrationTest {

  private static final Network network = Network.newNetwork();

  private static final Logger LOGGER = LoggerFactory.getLogger(FolioDcbApplicationIntegrationTest.class);

  @ClassRule
  public static final WireMockClassRule okapi = new WireMockClassRule();

  @ClassRule
  public static final GenericContainer<?> module =
    new GenericContainer<>(
      new ImageFromDockerfile("mod-dcb").withFileFromPath(".", Path.of(".")))
      .withNetwork(network)
      .withExposedPorts(8081)
      .withAccessToHost(true)
      .withEnv("DB_HOST", "postgres")
      .withEnv("DB_PORT", "5432")
      .withEnv("DB_USERNAME", "username")
      .withEnv("DB_PASSWORD", "password")
      .withEnv("DB_DATABASE", "postgres")
      .withEnv("SYSTEM_USER_NAME", "dcb-system-user")
      .withEnv("SYSTEM_USER_PASSWORD", "dcb-system-user");

  @ClassRule
  public static final PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:12-alpine")
      .withNetwork(network)
      .withNetworkAliases("postgres")
      .withExposedPorts(5432)
      .withUsername("username")
      .withPassword("password")
      .withDatabaseName("postgres");

  @BeforeClass
  public static void beforeClass() {
    okapi.start();
    Testcontainers.exposeHostPorts(okapi.port());
    okapi.stubFor(get("/users?query=username=dcb-system-user").willReturn(okJson("{\"users\":[]}")));
    okapi.stubFor(post("/users").willReturn(created()));
    okapi.stubFor(post("/authn/credentials").willReturn(created()));
    okapi.stubFor(get(urlPathMatching("/perms/users/.*")).willReturn(notFound()));
    okapi.stubFor(post("/perms/users").willReturn(created()));

    RestAssured.reset();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + module.getHost() + ":" + module.getFirstMappedPort();
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader("X-Okapi-Tenant", "testtenant")
      .addHeader("X-Okapi-Url", "http://host.testcontainers.internal:" + okapi.port())
      .setContentType(ContentType.JSON)
      .build();

    module.followOutput(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());
  }

  @Test
  public void health() {
    when().
      get("/admin/health").
      then().
      statusCode(200).
      assertThat().
      body(containsString("UP"));
  }

  @Test
  public void installAndUpgrade() {
    postTenant(new JsonObject().put("module_to", "999999.0.0"));
    // migrate from 0.0.0 to test that migration is idempotent
    postTenant(new JsonObject().put("module_to", "999999.0.0").put("module_from", "0.0.0"));

    smokeTest();
  }


  private void postTenant(JsonObject body) {
      given().
        body(body.encodePrettily()).
        when().
        post("/_/tenant").
        then().
        statusCode(204);
  }

  private void smokeTest() {
    when().
      get("/transactions/0112cb41-f107-4845-8231-26bf1309c4d4/status").
      then().
      statusCode(404).
      assertThat().
      body(containsString("DCB Transaction was not found by id= 0112cb41-f107-4845-8231-26bf1309c4d4"));
  }

}
