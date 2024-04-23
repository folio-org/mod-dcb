package org.folio.dcb.config;

import lombok.RequiredArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider", modifyOnCreate = false)
@RequiredArgsConstructor
public class JpaAuditingConfig implements AuditorAware<UUID>{

  private final FolioExecutionContext folioExecutionContext;

  @Override
  public Optional<UUID> getCurrentAuditor() {
    return Optional.ofNullable(folioExecutionContext.getUserId());
  }

  @Bean
  public DateTimeProvider dateTimeProvider() {
    return () -> Optional.of(OffsetDateTime.now());
  }

}
