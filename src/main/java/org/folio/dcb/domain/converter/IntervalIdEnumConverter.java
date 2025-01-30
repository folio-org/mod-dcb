package org.folio.dcb.domain.converter;

import java.util.Arrays;

import org.folio.dcb.domain.dto.IntervalIdEnum;

import jakarta.persistence.AttributeConverter;

public class IntervalIdEnumConverter implements AttributeConverter<IntervalIdEnum, String> {

  @Override
  public String convertToDatabaseColumn(IntervalIdEnum intervalIdEnum) {
    return intervalIdEnum.getValue();
  }

  @Override
  public IntervalIdEnum convertToEntityAttribute(String str) {
    return Arrays.stream(IntervalIdEnum.values())
      .filter(value -> value.getValue().equals(str))
      .findFirst()
      .orElse(null);
  }
}
