import {TranslateModule} from '@ngx-translate/core';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {of} from 'rxjs';
import {MockProvider} from 'ng-mocks';
import {RelationsComponent} from './relations.component';
import {ProcessService} from '../../../shared/processservice/process.service';
import {PageResponse, RelationDTO} from '../../../shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

const mockPage: PageResponse<RelationDTO> = {
	content: [
		{subjectType: 'PERSON', subjectId: 'sub-1', objectType: 'DOSSIER', objectId: 'obj-1', predicateType: 'OWNS', createdAt: '2024-01-15T10:00:00Z'},
		{subjectType: 'PERSON', subjectId: 'sub-2', objectType: 'CASE', objectId: 'obj-2', predicateType: 'MANAGES', createdAt: '2024-01-16T10:00:00Z'}
	],
	page: {totalElements: 2, totalPages: 1, number: 0, size: 10}
};

describe('RelationsComponent', () => {
	it('renders relations', () => {
		cy.mount(RelationsComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getRelations: () => of(mockPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.get('table').should('exist');
		cy.get('tr[mat-row]').should('have.length', 2);
		cy.contains('PERSON');
		cy.contains('sub-1');
		cy.contains('DOSSIER');
		cy.contains('OWNS');
	});

	it('shows empty message when no relations', () => {
		const emptyPage: PageResponse<RelationDTO> = {
			content: [],
			page: {totalElements: 0, totalPages: 0, number: 0, size: 10}
		};
		cy.mount(RelationsComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getRelations: () => of(emptyPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('i18n.none');
	});
});
