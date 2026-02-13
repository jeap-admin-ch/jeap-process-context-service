import {TranslateModule} from '@ngx-translate/core';
import {TaskDetailViewComponent} from './task-detail-view.component';
import {TaskDetailsNodataRowComponent} from './task-details-nodata-row/task-details-nodata-row.component';
import {TaskDTO, TaskState} from '../../../shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

const taskWithData: TaskDTO = {
	originTaskId: 'task-1',
	name: {de: 'Test Aufgabe', fr: 'Tâche Test', it: 'Compito Test'},
	state: TaskState.PLANNED,
	createdAt: '2024-01-15T10:00:00Z',
	plannedAt: '2024-01-15T09:00:00Z',
	completedAt: null,
	lifecycle: 'STATIC',
	cardinality: 'SINGLE_INSTANCE',
	plannedBy: [{key: 'user1', value: 'User One', label: {de: 'Benutzer Eins'}}],
	completedBy: null,
	taskData: [{key: 'dataKey', value: 'dataValue', labels: {de: 'Daten Schlüssel'}}]
};

describe('TaskDetailViewComponent', () => {
	it('renders task details', () => {
		cy.mount(TaskDetailViewComponent, {
			imports: [TranslateModule.forRoot()],
			declarations: [TaskDetailsNodataRowComponent],
			providers: [provideObliqueTestingConfiguration()],
			componentProperties: {task: taskWithData}
		});

		cy.contains('Test Aufgabe');
		cy.contains('task-1');
		cy.contains('dataValue');
		cy.contains('User One');
	});

	it('renders nothing when task is null', () => {
		cy.mount(TaskDetailViewComponent, {
			imports: [TranslateModule.forRoot()],
			declarations: [TaskDetailsNodataRowComponent],
			providers: [provideObliqueTestingConfiguration()],
			componentProperties: {task: null}
		});

		cy.get('h5').should('not.exist');
		cy.get('table').should('not.exist');
	});
});
