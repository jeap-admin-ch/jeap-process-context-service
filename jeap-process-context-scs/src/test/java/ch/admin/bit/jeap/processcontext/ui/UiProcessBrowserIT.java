package ch.admin.bit.jeap.processcontext.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Browser coverage for process search, navigation, details, tasks, data, messages and relations. */
class UiProcessBrowserIT extends UiBrowserTestBase {

    @Test
    void startPage_listsAndFiltersProcesses_andNavigatesToDetails() {
        createStartedProcess();

        page.navigate(APP_URL + "startpage");

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Willkommen"))).isVisible();
        Locator processRow = page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(originProcessId));
        assertThat(processRow).containsText("domainEventTriggersProcessInstantiation");
        assertThat(processRow).containsText("Gestartet");

        page.getByLabel("Process Data").fill("value-that-does-not-exist");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Suchen")).click();
        assertThat(page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(originProcessId))).hasCount(0);

        page.getByLabel("Process Data").fill("");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Suchen")).click();
        processRow = page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText(originProcessId));
        processRow.click();
        page.waitForURL("**/process/" + originProcessId);
        assertThat(page.getByText(originProcessId, new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    @Test
    void processIdForm_opensExistingProcess_andRejectsUnknownProcess() {
        createStartedProcess();
        page.navigate(APP_URL + "startpage");

        assertThat(page.getByRole(AriaRole.ROW)
                .filter(new Locator.FilterOptions().setHasText(originProcessId))).isVisible();
        Locator processIdInput = page.getByLabel("ProcessOriginId");
        processIdInput.fill(originProcessId);
        assertThat(processIdInput).hasValue(originProcessId);
        processIdInput.press("Enter");
        page.waitForURL("**/process/" + originProcessId);

        page.navigate(APP_URL + "startpage");
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Suchen"))).isEnabled();
        Locator unknownProcessInput = page.getByLabel("ProcessOriginId");
        unknownProcessInput.fill("unknown-process-id");
        unknownProcessInput.press("Enter");
        assertThat(unknownProcessInput).hasAttribute("aria-invalid", "true");
    }

    @Test
    void processInstanceByIdView_redirectsToExistingProcess_andPrefillsUnknownId() {
        createStartedProcess();

        page.navigate(APP_URL + "views/process-instance-by-id?originProcessId=" + originProcessId);
        page.waitForURL("**/process/" + originProcessId);
        assertThat(page.getByText(originProcessId, new Page.GetByTextOptions().setExact(true))).isVisible();

        page.navigate(APP_URL + "views/process-instance-by-id?originProcessId=unknown-process-id");
        page.waitForURL("**/startpage?processIdQuery=unknown-process-id");
        assertThat(page.getByLabel("ProcessOriginId")).hasValue("unknown-process-id");
    }

    @Test
    void processDetails_showsLifecycleTasksProcessDataMessagesAndEmptyRelations() {
        createStartedProcess();
        completeProcess();

        page.navigate(APP_URL + "process/" + originProcessId);

        assertThat(page.getByText("domainEventTriggersProcessInstantiation").first()).isVisible();
        assertThat(page.getByText("Beendet", new Page.GetByTextOptions().setExact(true)).first()).isVisible();
        assertThat(page.getByText("task.mandatory").first()).isVisible();

        page.getByRole(AriaRole.RADIO, new Page.GetByRoleOptions().setName("Aktivierte")).click();
        assertThat(page.getByText("task.mandatory").first()).isVisible();

        openPanel("Process Data");
        assertThat(page.getByText("correlationProcessData", new Page.GetByTextOptions().setExact(true))).isVisible();

        openPanel("Messages");
        assertThat(page.getByText("Test1CreatingProcessInstanceEvent", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByText("Test2Event", new Page.GetByTextOptions().setExact(true))).isVisible();

        openPanel("Relations");
        assertThat(page.getByText("Keine", new Page.GetByTextOptions().setExact(true)).last()).isVisible();
    }

    private void openPanel(String heading) {
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(heading)).click();
    }
}
