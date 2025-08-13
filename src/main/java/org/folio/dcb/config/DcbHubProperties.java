package org.folio.dcb.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "application.dcb-hub")
@Component
@Validated
@Getter
@Setter
public class DcbHubProperties {

    private String locationsUrl;

    @NotNull
    private Boolean fetchDcbLocationsEnabled;

    @AssertTrue(message = "dcb-hub.locations-url must be provided when dcb-hub.fetch-dcb-locations-enabled is true")
    public boolean isLocationsUrl() {
        if (Boolean.TRUE.equals(fetchDcbLocationsEnabled)) {
            return StringUtils.isNotBlank(locationsUrl);
        }
        return true;
    }
}