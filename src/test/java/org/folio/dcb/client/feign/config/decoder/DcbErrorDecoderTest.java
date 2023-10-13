package org.folio.dcb.client.feign.config.decoder;

import org.folio.dcb.exception.ResourceAlreadyExistException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DcbErrorDecoderTest{
  @Test
  void shouldReturnResourceAlreadyExistExceptionWhenServicePointExistsResponseTest(){
    String url = "http://service-points";
    String message = "Service Point Exists";
    int status = 422;
    DcbErrorDecoder dcbErrorDecoder = new DcbErrorDecoder();
    assertEquals(ResourceAlreadyExistException.class, dcbErrorDecoder.decode("POST", generateResponse(url, message, status)).getClass());
  }

  @Test
  void shouldReturnExceptionTest(){
    String url = "http://service-points";
    String message = "Something wrong";
    int status = 404;
    DcbErrorDecoder dcbErrorDecoder = new DcbErrorDecoder();
    assertEquals(RuntimeException.class, dcbErrorDecoder.decode("POST", generateResponse(url, message, status)).getClass());
  }

  feign.Response generateResponse(String url,String message, int status){
    return feign.Response.builder()
      .status(status)
      .request(feign.Request.create(feign.Request.HttpMethod.GET, url, new java.util.HashMap<>(), null, new feign.RequestTemplate()))
      .body(message, java.nio.charset.StandardCharsets.UTF_8)
      .build();
  }
}
