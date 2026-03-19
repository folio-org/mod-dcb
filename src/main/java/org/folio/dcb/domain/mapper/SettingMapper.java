package org.folio.dcb.domain.mapper;

import org.apache.commons.lang3.StringUtils;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingScope;
import org.folio.dcb.domain.entity.SettingEntity;
import org.folio.dcb.utils.JsonUtils;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, imports = JsonUtils.class)
public abstract class SettingMapper {

  @Mapping(target = "value", expression = "java(JsonUtils.readTree(entity.getValue()))")
  @Mapping(target = "scope", expression = "java(parseSettingScopeFromString(entity.getScope()))")
  @Mapping(target = "metadata.createdDate", source = "entity.createdDate")
  @Mapping(target = "metadata.createdByUserId", source = "entity.createdBy")
  @Mapping(target = "metadata.updatedDate", source = "entity.updatedDate")
  @Mapping(target = "metadata.updatedByUserId", source = "entity.updatedBy")
  public abstract Setting convert(SettingEntity entity);

  @Mapping(target = "createdDate", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedDate", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "scope", expression = "java(dto.getScope() != null ? dto.getScope().getValue() : null)")
  @Mapping(target = "value", expression = "java(JsonUtils.writeValueAsString(dto.getValue()))")
  public abstract SettingEntity convert(Setting dto);

  /**
   * Parses a string value to its corresponding {@link SettingScope} enum. Returns null if no matching value is found.
   *
   * @param scope the string representation of the setting scope
   */
  public static SettingScope parseSettingScopeFromString(String scope) {
    for (var value : SettingScope.values()) {
      if (StringUtils.equalsIgnoreCase(value.getValue(), scope)) {
        return value;
      }
    }

    return null;
  }
}
