package ch.admin.bit.jeap.processcontext.ui.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuration")
@RequiredArgsConstructor
@Slf4j
class ConfigurationController {
    private final FrontendConfigProperties frontendConfigProperties;

    private final LogDeepLinkProperties logDeepLinkProperties;

    @GetMapping
    public ConfigurationDTO getConfiguration() {
        return ConfigurationDTO.builder()
                .applicationUrl(frontendConfigProperties.getApplicationUrl().toString())
                .pamsEnvironment(frontendConfigProperties.getPamsEnvironment())
                .authority(frontendConfigProperties.getStsServer().toString())
                .redirectUrl(frontendConfigProperties.getApplicationUrl().toString())
                .logoutRedirectUri(frontendConfigProperties.getLogoutRedirectUri().toString())
                .mockPams(frontendConfigProperties.isMockPams())
                .tokenAwarePatterns(frontendConfigProperties.getTokenAwarePattern())
                .useAutoLogin(frontendConfigProperties.isAutoLogin())
                .clientId(frontendConfigProperties.getClientId())
                .silentRenew(frontendConfigProperties.isSilentRenew())
                .systemName(frontendConfigProperties.getSystemName())
                .renewUserInfoAfterTokenRenew(frontendConfigProperties.isRenewUserInfoAfterTokenRenew())
                .build();
    }

    @Schema(description = "Returns the Version")
    @GetMapping("/version")
    public String getVersion() {
        VersionDetector versionDetector = new VersionDetector();
        return versionDetector.getVersion();
    }

    @Schema(description = "Returns the custom Log deeplink template")
    @GetMapping("/log-deeplink")
    public String getLogDeepLink() {
        return logDeepLinkProperties.getBaseUrl();
    }

}
