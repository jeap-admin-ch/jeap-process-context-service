import {TranslateModule} from '@ngx-translate/core';
import {TaskTemplateViewComponent} from './task-template-view.component';
import {MatTableModule as MatTableModule} from '@angular/material/table';
import {TaskTestFixtures} from '../../../shared/test/task-test-fixtures';
import {RouterModule} from "@angular/router";
import {provideObliqueTestingConfiguration} from "@oblique/oblique";

// eslint-disable-next-line max-lines-per-function
describe('process-page-component.cy.ts', () => {
	it('All Tasks', () => {
		cy.mount(TaskTemplateViewComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule],
			declarations: [],
			providers: [provideObliqueTestingConfiguration()],
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

});
