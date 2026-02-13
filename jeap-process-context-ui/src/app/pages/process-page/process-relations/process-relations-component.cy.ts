import {TranslateModule} from '@ngx-translate/core';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {RouterModule} from '@angular/router';
import {of} from 'rxjs';
import {MockProvider} from 'ng-mocks';
import {ProcessRelationsComponent} from './process-relations.component';
import {ProcessService} from '../../../shared/processservice/process.service';
import {PageResponse, ProcessRelationDTO} from '../../../shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

const mockPage: PageResponse<ProcessRelationDTO> = {
	content: [
		{relation: {de: 'Elternprozess'}, processName: {de: 'Prozess A'}, processId: 'pid-1', processState: 'STARTED'},
		{relation: {de: 'Kindprozess'}, processName: {de: 'Prozess B'}, processId: 'pid-2', processState: 'COMPLETED'}
	],
	page: {totalElements: 2, totalPages: 1, number: 0, size: 5}
};

describe('ProcessRelationsComponent', () => {
	it('renders process relations', () => {
		cy.mount(ProcessRelationsComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getProcessRelations: () => of(mockPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('Elternprozess');
		cy.contains('Prozess A');
		cy.contains('pid-1');
		cy.contains('Kindprozess');
		cy.contains('Prozess B');
		cy.contains('pid-2');
	});

	it('shows empty message when no relations', () => {
		const emptyPage: PageResponse<ProcessRelationDTO> = {
			content: [],
			page: {totalElements: 0, totalPages: 0, number: 0, size: 5}
		};
		cy.mount(ProcessRelationsComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getProcessRelations: () => of(emptyPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('i18n.none');
	});
});
