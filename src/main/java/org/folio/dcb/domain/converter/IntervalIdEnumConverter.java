package org.folio.dcb.domain.converter;

import java.util.Arrays;

import org.folio.dcb.domain.dto.IntervalIdEnum;

import jakarta.persistence.AttributeConverter;

public class IntervalIdEnumConverter implements AttributeConverter<String, IntervalIdEnum> {

  @Override
  public IntervalIdEnum convertToDatabaseColumn(String str) {
    return Arrays.stream(IntervalIdEnum.values())
      .filter(value -> value.getValue().equals(str))
      .findFirst()
      .orElse(null);

  }

  @Override
  public String convertToEntityAttribute(IntervalIdEnum intervalIdEnum) {
    return intervalIdEnum.getValue();
  }
}
