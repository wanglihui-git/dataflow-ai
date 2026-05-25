package com.dataflow.ai.business.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.permission")
public class PermissionProperties {

    private boolean enabled = true;
    private String defaultMaskType = "PARTIAL";
}
