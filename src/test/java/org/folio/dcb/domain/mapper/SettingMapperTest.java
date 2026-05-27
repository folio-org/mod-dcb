package org.folio.dcb.domain.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.dcb.domain.dto.SettingScope;
import org.folio.dcb.support.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class SettingMapperTest {

  @ParameterizedTest
  @EnumSource(SettingScope.class)
  void parseSettingScopeFromString_parameterized_lowerCase(SettingScope scope) {
    var result = SettingMapper.parseSettingScopeFromString(scope.getValue().toLowerCase());
    assertThat(result).isEqualTo(scope);
  }

  @ParameterizedTest
  @EnumSource(SettingScope.class)
  void parseSettingScopeFromString_parameterized_upperCase(SettingScope scope) {
    var result = SettingMapper.parseSettingScopeFromString(scope.getValue().toUpperCase());
    assertThat(result).isEqualTo(scope);
  }

  @Test
  void parseSettingScopeFromString_negative_invalidValue() {
    var result = SettingMapper.parseSettingScopeFromString("invalid");
    assertThat(result).isNull();
  }
}
