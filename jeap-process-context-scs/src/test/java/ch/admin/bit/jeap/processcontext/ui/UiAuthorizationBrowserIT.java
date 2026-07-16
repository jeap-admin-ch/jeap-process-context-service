package ch.admin.bit.jeap.processcontext.ui;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Browser tests for route guards and token-aware REST calls. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UiAuthorizationBrowserIT extends UiBrowserTestBase {

    @Test
    @Order(2)
    void startPage_withoutViewRole_redirectsToForbidden() {
        openPageWithRoles(UNRELATED_ROLES);

        page.navigate(APP_URL + "startpage");

        page.waitForURL("**/Forbidden**");
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Nicht autorisierter Zugriff"))).isVisible();
    }

    @Test
    @Order(3)
    void processDetails_withoutViewRole_redirectsToForbidden() {
        createStartedProcess();
        openPageWithRoles(UNRELATED_ROLES);

        page.navigate(APP_URL + "process/" + originProcessId);

        page.waitForURL("**/Forbidden**");
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Nicht autorisierter Zugriff"))).isVisible();
    }

    @Test
    @Order(1)
    void viewRole_canOpenGuardedRoute_andCallProtectedBackend() {
        createStartedProcess();

        page.navigate(APP_URL + "process/" + originProcessId);

        page.waitForURL("**/process/" + originProcessId);
        assertThat(page.getByText(originProcessId, new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByText("domainEventTriggersProcessInstantiation").first()).isVisible();
    }
}
