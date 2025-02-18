import {TranslateModule} from '@ngx-translate/core';
import {MatTableModule as MatTableModule} from '@angular/material/table';
import {TaskTestFixtures} from '../../../shared/test/task-test-fixtures';
import {TaskActivityViewComponent} from './task-activity-view.component';
import {getTranslationServiceMockProvider} from '../../../../../cypress/support/mockproviderFunctions';
import {RouterModule} from "@angular/router";

// eslint-disable-next-line max-lines-per-function
describe('task-activity-view-component.cy.ts', () => {
	it('no Tasks', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedStaticList
			}
		});

		cy.contains('i18n.none.tasks');
	});

	it('Planned and Completed Task', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('de')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedList
			}
		});
		cy.get('table').find('tr').should('have.length', 5);
		cy.contains('Planned Static Task #1').should('not.exist');
		cy.contains('Planned Dynamic Task #2');
		cy.contains('Planned Dynamic Task #3');
		cy.contains('Completed Task #4');
		cy.contains('Completed Task #5');
		cy.contains('Not Planned Task #6').should('not.exist');
	});

	it('load fr', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('fr')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedDynamicList
			}
		});
		cy.contains('[fr] Planned Dynamic Task #2');
		cy.contains('[fr] Planned Dynamic Task #3');
	});

	it('load it', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [getTranslationServiceMockProvider('it')],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedDynamicList
			}
		});
		cy.contains('[it] Planned Dynamic Task #2');
		cy.contains('[it] Planned Dynamic Task #3');
	});
});
