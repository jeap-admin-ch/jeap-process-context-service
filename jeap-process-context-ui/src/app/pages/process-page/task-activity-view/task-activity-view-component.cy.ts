import {MatTableModule} from '@angular/material/table';
import {MatSortModule} from '@angular/material/sort';
import {MatIconModule} from '@angular/material/icon';
import {TranslateModule} from '@ngx-translate/core';
import {TaskActivityViewComponent} from './task-activity-view.component';
import {TaskDTO, TaskState} from 'src/app/shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from "@oblique/oblique";

const tasks: TaskDTO[] = [
	{
		originTaskId: '1',
		name: { en: 'Task 1', de: 'Aufgabe 1', fr: 'Tâche 1', it: 'Compito 1' },
		state: TaskState.PLANNED,
		createdAt: '2024-06-01T10:00:00Z',
		plannedAt: '2024-06-01T09:00:00Z',
		completedAt: null,
		lifecycle: 'DYNAMIC',
		cardinality: 'SINGLE',
		plannedBy: [],
		completedBy: null,
		taskData: []
	},
	{
		originTaskId: '2',
		name: { en: 'Task 2', de: 'Aufgabe 2', fr: 'Tâche 2', it: 'Compito 2' },
		state: TaskState.COMPLETED,
		createdAt: '2024-06-02T10:00:00Z',
		plannedAt: '2024-06-02T09:00:00Z',
		completedAt: '2024-06-03T10:00:00Z',
		lifecycle: 'DYNAMIC',
		cardinality: 'SINGLE',
		plannedBy: [],
		completedBy: [],
		taskData: []
	}
];

describe('TaskActivityViewComponent', () => {
	it('renders tasks', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [MatTableModule, MatSortModule, MatIconModule, TranslateModule.forRoot()],
			providers: [ provideObliqueTestingConfiguration()],
			componentProperties: { tasks }
		});

		cy.get('table').should('exist');
		cy.get('tr[mat-row]').should('have.length', 2);
		cy.contains('Aufgabe 1');
		cy.contains('Aufgabe 2');
		cy.get('mat-icon[svgIcon="calendar"]').should('exist');
		cy.get('mat-icon[svgIcon="checkmark_circle"]').should('exist');
	});

	it('shows no tasks message when empty', () => {
		cy.mount(TaskActivityViewComponent, {
			imports: [MatTableModule, MatSortModule, MatIconModule, TranslateModule.forRoot()],
			componentProperties: {
				tasks: []
			}
		});
		cy.contains('none.tasks');
	});
});
