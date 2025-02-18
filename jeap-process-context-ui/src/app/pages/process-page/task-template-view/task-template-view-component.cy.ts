import {TranslateModule} from '@ngx-translate/core';
import {TaskTemplateViewComponent} from './task-template-view.component';
import {MatTableModule as MatTableModule} from '@angular/material/table';
import {TaskTestFixtures} from '../../../shared/test/task-test-fixtures';
import {getTranslationServiceMockProvider} from "../../../../../cypress/support/mockproviderFunctions";
import {RouterModule} from "@angular/router";

// eslint-disable-next-line max-lines-per-function
describe('process-page-component.cy.ts', () => {
	it('All Tasks', () => {
		cy.mount(TaskTemplateViewComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('de')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedList
			}
		});

		cy.get('table')
			.find('tr')
			.should('have.length', TaskTestFixtures.taskPlannedList.length + 1);
		cy.contains('Planned Static Task #1');
		cy.contains('Planned Dynamic Task #2');
		cy.contains('Planned Dynamic Task #3');
		cy.contains('Completed Task #4');
		cy.contains('Completed Task #5');
		cy.contains('Not Planned Task #6');
	});

	it('load fr', () => {
		cy.mount(TaskTemplateViewComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('fr')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedStaticList
			}
		});
		cy.contains('[fr] Planned Static Task #1');
		cy.contains('Planned Dynamic Task #2').should('not.exist');
	});

	it('load it', () => {
		cy.mount(TaskTemplateViewComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('it')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedStaticList
			}
		});
		cy.contains('[it] Planned Static Task #1');
		cy.contains('Planned Dynamic Task #2').should('not.exist');
	});
});
