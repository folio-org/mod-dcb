package org.folio.dcb.integration.dcb.config;

import feign.Client;
import feign.okhttp.OkHttpClient;
import org.folio.common.utils.tls.FeignClientTlsUtils;
import org.springframework.context.annotation.Bean;

public class DcbHubClientConfiguration {

  /**
   * Feign {@link OkHttpClient} based client.
   *
   * @param okHttpClient - {@link OkHttpClient} from spring context
   * @return created feign {@link Client} object
   */
  @Bean
  public Client feignClient(DcbHubProperties dcbHubProperties, okhttp3.OkHttpClient okHttpClient) {
    return FeignClientTlsUtils.getOkHttpClient(okHttpClient, dcbHubProperties.getTls());
  }
}
