package org.folio.dcb.integration.keycloak;

import java.time.Instant;
import java.util.Base64;

import org.folio.dcb.exception.TokenParsingException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtTokenParser {
  private final ObjectMapper objectMapper;

  public Instant parseExpirationDate(String token) {
    var exp = parseExpiration(token);
    return Instant.ofEpochSecond(exp);
  }

  private long parseExpiration(String token) {
    if (token == null) {
      throw new TokenParsingException("Failed to find auth token in request.");
    }

    var split = token.split("\\.");
    if (split.length < 2 || split.length > 3) {
      throw new TokenParsingException("Invalid amount of segments in JWT token.");
    }

    try {
      var payload = objectMapper.readTree(new String(Base64.getUrlDecoder().decode(split[1])));
      return payload.get("exp").asLong();
    } catch (Exception e) {
      log.warn("Failed to parse token", e);
      throw new TokenParsingException("Invalid token.");
    }
  }
}
