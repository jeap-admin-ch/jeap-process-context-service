package ch.admin.bit.jeap.processcontext.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser tests with the PAMS integration disabled, which is how the tests are configured
 * (jeap.processcontext.frontend.pams-enabled=false in application-ui-e2e.yml).
 */
class UiWithoutPamsBrowserIT extends UiBrowserTestBase {

    @Test
    void uiWithPamsDisabled_doesNotContactEportal() {
        page.navigate(APP_URL + "startpage");

        // wait for the application to be fully loaded before asserting on the requests it has sent
        assertThat(languageSelection()).isVisible();

        org.assertj.core.api.Assertions.assertThat(ePortalRequests).isEmpty();
    }

    @Test
    void uiWithPamsDisabled_hidesLoginAndProfileButKeepsLanguageSelection() {
        page.navigate(APP_URL + "startpage");

        assertThat(languageSelection()).isVisible();
        assertThat(loginLink()).not().isVisible();
        assertThat(profileButton()).not().isVisible();
    }

    @Test
    void uiWithPamsDisabled_showsProcessList() {
        createStartedProcess();

        page.navigate(APP_URL + "startpage");

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Willkommen"))).isVisible();
        Locator processRow = page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(originProcessId));
        assertThat(processRow).containsText("domainEventTriggersProcessInstantiation");
    }

    /**
     * The language selection of Oblique's service navigation. Its label is rendered by Oblique itself and is
     * therefore not part of the application's i18n files, so the element id assigned by Oblique is used.
     */
    private Locator languageSelection() {
        return page.locator("#ob-language-dropdown");
    }

    /**
     * The login link of Oblique's service navigation, which without a reachable ePortal backend would be
     * rendered in a disabled state. Located by the element id assigned by Oblique, see languageSelection().
     */
    private Locator loginLink() {
        return page.locator("#ob-service-navigation-authentication-link-to-login");
    }

    /**
     * The profile button of Oblique's service navigation. Located by the element id assigned by Oblique, see
     * languageSelection().
     */
    private Locator profileButton() {
        return page.locator("#service-navigation-toggle-profile-icon-button");
    }
}
