package org.folio.dcb.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import org.folio.common.utils.tls.FeignClientTlsUtils;
import org.folio.dcb.integration.keycloak.config.DcbHubKeycloakProperties;
import org.springframework.context.annotation.Bean;

public class DcbClientConfiguration {

  /**
   * Feign {@link OkHttpClient} based client.
   *
   * @param okHttpClient - {@link OkHttpClient} from spring context
   * @return created feign {@link Client} object
   */
  @Bean
  public Client feignClient(DcbHubKeycloakProperties dcbHubKeycloakProperties, okhttp3.OkHttpClient okHttpClient) {
    return FeignClientTlsUtils.getOkHttpClient(okHttpClient, dcbHubKeycloakProperties.getTls());
  }
}
