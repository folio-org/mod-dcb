package org.folio.dcb.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.dcb.domain.entity.base.AuditableEntity;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.domain.Persistable;

@Data
@Entity
@Table(name = "settings")
@EqualsAndHashCode(callSuper = true)
public class SettingEntity extends AuditableEntity implements Persistable<UUID> {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String key;

  @Column
  private String scope;

  @Column(columnDefinition = "jsonb")
  @ColumnTransformer(write = "?::jsonb")
  private String value;

  @Column
  @Version
  private Integer version;

  @Override
  @Transient
  public boolean isNew() {
    return version == null;
  }

  @PrePersist
  private void initVersion() {
    version = 1;
  }
}
