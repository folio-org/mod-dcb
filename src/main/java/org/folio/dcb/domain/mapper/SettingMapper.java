package org.folio.dcb.domain.mapper;

import org.apache.commons.lang3.Strings;
import org.folio.dcb.domain.dto.Setting;
import org.folio.dcb.domain.dto.SettingScope;
import org.folio.dcb.domain.entity.SettingEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class SettingMapper {

  protected JsonMapper jsonMapper;

  @Mapping(target = "value", expression = "java(jsonMapper.readTree(entity.getValue()))")
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
  @Mapping(target = "value", expression = "java(jsonMapper.writeValueAsString(dto.getValue()))")
  public abstract SettingEntity convert(Setting dto);

  @Autowired
  private void setJsonMapper(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public static SettingScope parseSettingScopeFromString(String scope) {
    for (var value : SettingScope.values()) {
      if (Strings.CI.equals(value.getValue(), scope)) {
        return value;
      }
    }

    return null;
  }
}
