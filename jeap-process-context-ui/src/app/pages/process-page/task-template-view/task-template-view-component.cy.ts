import {TranslateModule} from '@ngx-translate/core';
import {TaskTemplateViewComponent} from './task-template-view.component';
import {MatTableModule as MatTableModule} from '@angular/material/table';
import {TaskTestFixtures} from '../../../shared/test/task-test-fixtures';
import {RouterModule} from '@angular/router';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

describe('TaskTemplateViewComponent', () => {
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

	it('navigates tasks with arrow keys', () => {
		cy.mount(TaskTemplateViewComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule],
			providers: [provideObliqueTestingConfiguration()],
			componentProperties: {
				tasks: TaskTestFixtures.taskPlannedList
			}
		}).then(wrapper => {
			const component = wrapper.component;
			const emitted: any[] = [];
			component.taskSelected.subscribe((task: any) => emitted.push(task));

			// Initial state: index 0 selected
			expect(component.selectedTaskIndex).to.equal(0);

			// ArrowDown: 0 -> 1
			component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowDown'}));
			expect(component.selectedTaskIndex).to.equal(1);
			expect(emitted.length).to.equal(1);

			// ArrowDown: 1 -> 2
			component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowDown'}));
			expect(component.selectedTaskIndex).to.equal(2);
			expect(emitted.length).to.equal(2);

			// ArrowUp: 2 -> 1
			component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
			expect(component.selectedTaskIndex).to.equal(1);
			expect(emitted.length).to.equal(3);

			// ArrowUp at 0 stays at 0
			component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
			component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowUp'}));
			expect(component.selectedTaskIndex).to.equal(0);

			// ArrowDown to last, then ArrowDown again stays at last
			for (let i = 0; i < 10; i++) {
				component.onKeyDown(new KeyboardEvent('keydown', {key: 'ArrowDown'}));
			}
			expect(component.selectedTaskIndex).to.equal(TaskTestFixtures.taskPlannedList.length - 1);
		});
	});
});
