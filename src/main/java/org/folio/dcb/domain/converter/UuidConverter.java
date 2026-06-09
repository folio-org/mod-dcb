package org.folio.dcb.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter
public class UuidConverter implements AttributeConverter<String, UUID> {

  @Override
  public UUID convertToDatabaseColumn(String str) {
    return str == null ? null : UUID.fromString(str);
  }

  @Override
  public String convertToEntityAttribute(UUID uuid) {
    return uuid == null ? null : uuid.toString();
  }
}
