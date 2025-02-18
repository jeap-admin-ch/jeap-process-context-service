package ch.admin.bit.jeap.processcontext.ui.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class ConfigurationDTO {
    private String applicationUrl;
    private String logoutRedirectUri;
    private boolean mockPams;
    private String pamsEnvironment;
    private List<String> tokenAwarePatterns;
    private String authority;
    private String redirectUrl;
    private String clientId;
    private boolean useAutoLogin;

    private boolean silentRenew;
    private String systemName;
    private boolean renewUserInfoAfterTokenRenew;
}
