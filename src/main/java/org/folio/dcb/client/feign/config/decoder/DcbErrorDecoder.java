package org.folio.dcb.client.feign.config.decoder;

import feign.codec.ErrorDecoder;
import lombok.extern.log4j.Log4j2;
import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
public class DcbErrorDecoder implements ErrorDecoder {
  @Override
  public Exception decode(String methodKey, feign.Response response) {

    String requestUrl = response.request().url();
    HttpStatus responseStatus = HttpStatus.valueOf(response.status());

    StringBuilder message = new StringBuilder();
    try (InputStream bodyIs = response.body().asInputStream()) {
      message = new StringBuilder(new String(bodyIs.readAllBytes()));
    } catch (IOException e) {
      log.debug("Error during reading response body", e);
    }

    if (requestUrl.contains("service-points") && responseStatus == HttpStatus.UNPROCESSABLE_ENTITY && message.toString().contains("Service Point Exists")) {
      log.debug("Service point already exists");
      return new ResourceAlreadyExistException("Service point already exists");
    }

    log.debug("Error during request to {}. Response status: {}. Response body: {}", requestUrl, responseStatus, message);
    return new RuntimeException(String.format("Error during request to %s. Response status: %s. Response body: %s", requestUrl, responseStatus, message));
  }
}
