package org.folio.dcb.client.feign.config;

import feign.codec.ErrorDecoder;
import org.folio.dcb.client.feign.config.decoder.DcbErrorDecoder;
import org.springframework.context.annotation.Bean;

public class DcbClientConfiguration {

  @Bean
  public ErrorDecoder errorDecoder() {
    return new DcbErrorDecoder();
  }

}
