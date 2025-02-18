package ch.admin.bit.jeap.processcontext.ui.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.List;

/**
 * Configuration properties that will be forwarded to the UI
 */
@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.frontend")
@Data
public class FrontendConfigProperties {
    /**
     * Authentication server to be used.
     */
    private URI stsServer;
    /**
     * URL of the application for the redirect URI after a login.
     */
    private URI applicationUrl;
    /**
     * URL to go to after a logout.
     */
    private URI logoutRedirectUri;
    /**
     * Should PAMS mock be used.
     */
    private boolean mockPams;
    /**
     * Pams Environment to be used.
     */
    private String pamsEnvironment;
    /**
     * List of backend where to a token shall be send.
     */
    private List<String> tokenAwarePattern;
    /**
     * Oidc client id
     */
    String clientId;
    /**
     * Should silent renew be used (currently only >= REF)
     */
    boolean silentRenew;
    /**
     * Default system name for authorization filter
     */
    String systemName;
    /**
     * Should automatically login, when PAMS session is not active
     */
    boolean autoLogin;
    /**
     * Should new claim be submitted after token was renewed (e.g. silent renew)
     */
    boolean renewUserInfoAfterTokenRenew;
}
