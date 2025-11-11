package support.postgres;

import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainerExtension implements BeforeAllCallback, AfterAllCallback {

  public static final String IMAGE_NAME = getImageName(System.getenv());
  private static final String DEFAULT_IMAGE_NAME = "postgres:16-alpine";
  private static final String SPRING_DB_URL_PROPERTY = "spring.datasource.url";
  private static final String SPRING_DB_USERNAME_PROPERTY = "spring.datasource.username";
  private static final String SPRING_DB_PASSWORD_PROPERTY = "spring.datasource.password";

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(IMAGE_NAME)
    .withDatabaseName("folio_test")
    .withUsername("folio_admin")
    .withPassword("qwerty123");

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_DB_URL_PROPERTY, CONTAINER.getJdbcUrl());
    System.setProperty(SPRING_DB_USERNAME_PROPERTY, CONTAINER.getUsername());
    System.setProperty(SPRING_DB_PASSWORD_PROPERTY, CONTAINER.getPassword());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_DB_URL_PROPERTY);
    System.clearProperty(SPRING_DB_USERNAME_PROPERTY);
    System.clearProperty(SPRING_DB_PASSWORD_PROPERTY);
  }

  static String getImageName(Map<String, String> env) {
    return env.getOrDefault("TESTCONTAINERS_POSTGRES_IMAGE", DEFAULT_IMAGE_NAME);
  }
}
