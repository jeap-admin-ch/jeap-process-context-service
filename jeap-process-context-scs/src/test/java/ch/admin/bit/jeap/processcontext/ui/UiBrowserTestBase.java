package ch.admin.bit.jeap.processcontext.ui;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import ch.admin.bit.jeap.processcontext.event.test1.Test1CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.testevent.Test1CreatingProcessInstanceEventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.test.mock.OidcAuthorizationMockServer;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;

/**
 * Base for end-to-end browser tests of the Angular UI served by the running process-context application.
 * Authentication uses the real OIDC authorization-code flow against {@link OidcAuthorizationMockServer}.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(DisableJeapPermitAllSecurityConfiguration.class)
@ActiveProfiles("ui-e2e")
public abstract class UiBrowserTestBase extends ProcessInstanceMockS3ITBase {

    protected static final String APP_URL = "http://localhost:8303/process-context/";
    protected static final String VIEW_ROLE = "jme_@processinstance_#view";
    protected static final List<String> VIEW_ROLES = List.of(VIEW_ROLE);
    protected static final List<String> UNRELATED_ROLES = List.of("jme_@other_#none");

    private static final int OIDC_PORT = 8305;
    private static final String OIDC_BASE_PATH = "/oidc-mock";
    private static final String CLIENT_ID = "process-context-service";
    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final String UNRELATED_PROFILE = "unrelated";
    private static final Map<List<String>, String> PROFILE_BY_ROLES = Map.of(
            VIEW_ROLES, "default",
            UNRELATED_ROLES, UNRELATED_PROFILE);

    private static OidcAuthorizationMockServer oidcMockServer;
    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @DynamicPropertySource
    static void oidcProperties(DynamicPropertyRegistry registry) {
        String issuer = "http://localhost:" + OIDC_PORT + OIDC_BASE_PATH;
        registry.add("jeap.security.oauth2.resourceserver.authorization-server.issuer", () -> issuer);
        registry.add("jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri",
                () -> issuer + "/.well-known/jwks.json");
    }

    @BeforeAll
    static void startBrowser() {
        // The tests deliberately use the Chrome installation available on developer and CI machines.
        // Prevent Playwright from downloading its additional bundled Chromium during a test run.
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome"));
    }

    @AfterAll
    static void stopBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
        // Keep the OIDC mock and its signing key stable for all browser test classes sharing the
        // Spring application context. The resource server caches the issuer's JWKS.
    }

    @BeforeEach
    void openAuthenticatedPage() {
        openPageWithRoles(VIEW_ROLES);
    }

    @AfterEach
    void closePage() {
        closeContext();
    }

    protected void openPageWithRoles(List<String> roles) {
        String profile = PROFILE_BY_ROLES.get(roles);
        if (profile == null) {
            throw new IllegalArgumentException("No OIDC role profile configured for " + roles);
        }
        ensureOidcMockServerStarted();
        oidcMockServer.reset();
        oidcMockServer.setActiveProfile(profile);
        closeContext();

        context = browser.newContext(new Browser.NewContextOptions().setLocale("de-CH"));
        context.route("http://localhost:" + OIDC_PORT + OIDC_BASE_PATH + "/**", route -> {
            // Do not follow the authorization endpoint's redirect here. The browser must process the
            // 302 itself so that the callback page is loaded from the application origin (port 8303).
            var response = route.fetch(new Route.FetchOptions().setMaxRedirects(0));
            var headers = new HashMap<>(response.headers());
            headers.put("access-control-allow-origin", "http://localhost:8303");
            headers.put("access-control-allow-methods", "GET, POST, OPTIONS");
            headers.put("access-control-allow-headers", "content-type, x-requested-with");
            route.fulfill(new Route.FulfillOptions().setResponse(response).setHeaders(headers));
        });
        page = context.newPage();
        page.setDefaultTimeout(20_000);
        PlaywrightAssertions.setDefaultAssertionTimeout(15_000);
        page.onConsoleMessage(message -> log.info("Browser console {}: {}", message.type(), message.text()));
        page.onPageError(error -> log.warn("Browser page error: {}", error));
        page.onResponse(response -> {
            if (response.url().contains("/api/") || response.url().contains(OIDC_BASE_PATH)
                    || response.status() >= 400) {
                log.info("Browser response: {} {} {}", response.status(), response.request().method(), response.url());
            }
        });

        // Complete the OIDC callback before handing the page to a test. The authorization server
        // redirects to the configured application root, not to the originally requested deep link.
        // Tests can therefore navigate deterministically only after this initial login has finished.
        page.navigate(APP_URL);
        if (roles.equals(VIEW_ROLES)) {
            page.waitForURL("**/startpage**");
        } else {
            page.waitForURL("**/Forbidden**");
        }
    }

    protected void createStartedProcess() {
        Test1CreatingProcessInstanceEvent event = Test1CreatingProcessInstanceEventBuilder
                .createForProcessId(originProcessId)
                .build();
        sendSync("topic.test1creatingprocessinstance", event);
        await("process instance has been created")
                .atMost(TIMEOUT)
                .until(() -> processInstanceRepository.findByOriginProcessId(originProcessId).isPresent());
    }

    protected void completeProcess() {
        Test2Event event = Test2EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test2", event);
        await("process instance has been completed")
                .atMost(TIMEOUT)
                .until(() -> processInstanceRepository.findByOriginProcessId(originProcessId)
                        .map(process -> process.getState() == ProcessState.COMPLETED)
                        .orElse(false));
    }

    private static synchronized void ensureOidcMockServerStarted() {
        if (oidcMockServer != null) {
            return;
        }
        oidcMockServer = OidcAuthorizationMockServer
                .builder(OIDC_PORT, OIDC_BASE_PATH, "http://localhost:8303")
                .withDefaultClientId(CLIENT_ID)
                .withSubject(SUBJECT)
                .withGivenName("E2E")
                .withFamilyName("Testuser")
                .withName("E2E Testuser")
                .withUserRoles(VIEW_ROLES)
                .withRoleProfile(UNRELATED_PROFILE, UNRELATED_ROLES)
                .build();
        oidcMockServer.start();
    }

    private void closeContext() {
        if (context != null) {
            context.close();
            context = null;
        }
    }
}
