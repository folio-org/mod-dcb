package org.folio.dcb.client.feign.config.decoder;

import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DcbErrorDecoderTest{
  @Test
  void shouldReturnExceptionWhenServicePointExistsResponseTest(){
    String url = "http://service-points";
    String message = "Service Point Exists";
    int status = 422;
    DcbErrorDecoder dcbErrorDecoder = new DcbErrorDecoder();
    assertThrows(ResourceAlreadyExistException.class, () -> {
      throw dcbErrorDecoder.decode("POST", generateResponse(url, message, status));
    });
  }

  feign.Response generateResponse(String url,String message, int status){
    return feign.Response.builder()
      .status(status)
      .request(feign.Request.create(feign.Request.HttpMethod.GET, url, new java.util.HashMap<>(), null, new feign.RequestTemplate()))
      .body(message, java.nio.charset.StandardCharsets.UTF_8)
      .build();
  }
}
