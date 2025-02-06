package org.folio.dcb.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dcb.domain.dto.IntervalIdEnum;
import org.junit.jupiter.api.Test;

class IntervalIdEnumConverterTest {

  private final IntervalIdEnumConverter converter = new IntervalIdEnumConverter();


  @Test
  void convertToDatabaseColumnTest() {
    assertEquals("Days", converter.convertToDatabaseColumn(IntervalIdEnum.DAYS));
  }

  @Test
  void convertToEntityAttributeTest() {
    assertEquals(IntervalIdEnum.DAYS , converter.convertToEntityAttribute("Days"));
  }
}