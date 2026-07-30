package ch.admin.bit.jeap.processcontext.ui.configuration;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * Is the application integrated with PAMS/ePortal. Set to false for deployments without PAMS: the ePortal
     * service navigation of the UI header is then not contacted at all and its PAMS-backed controls are hidden.
     */
    private boolean pamsEnabled = true;
    /**
     * Should PAMS mock be used. Implied when PAMS is disabled, see {@link #isMockPamsEffective()}.
     */
    private boolean mockPams;
    /**
     * Pams Environment to be used. Not required when PAMS is disabled.
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

    /**
     * Whether the UI should treat the PAMS session as always active instead of reading it from the ePortal
     * service navigation. Disabling PAMS implies mocking it: there is no PAMS session to read, and without
     * this the UI would wait forever for a login state the service navigation never reports.
     */
    public boolean isMockPamsEffective() {
        return !pamsEnabled || mockPams;
    }

    /**
     * The PAMS environment to serve to the UI, {@code null} if PAMS is disabled. A configured environment is
     * deliberately not passed on in that case, as it would make the UI contact the ePortal backend again.
     */
    public String getEffectivePamsEnvironment() {
        return pamsEnabled ? pamsEnvironment : null;
    }

    @PostConstruct
    void logPamsConfiguration() {
        if (!pamsEnabled) {
            log.info("PAMS integration is disabled: the UI will not contact the ePortal service navigation, " +
                    "will hide the header controls served by PAMS and will treat the PAMS session as mocked.");
        }
    }
}
