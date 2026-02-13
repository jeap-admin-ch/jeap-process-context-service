import {TranslateModule} from '@ngx-translate/core';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {of} from 'rxjs';
import {MockProvider} from 'ng-mocks';
import {ProcessDataComponent} from './process-data.component';
import {ProcessService} from '../../../shared/processservice/process.service';
import {PageResponse, ProcessDataDTO} from '../../../shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

const mockPage: PageResponse<ProcessDataDTO> = {
	content: [
		{key: 'applicantName', value: 'Max Mustermann', role: 'APPLICANT'},
		{key: 'caseNumber', value: '12345', role: 'SYSTEM'}
	],
	page: {totalElements: 2, totalPages: 1, number: 0, size: 10}
};

describe('ProcessDataComponent', () => {
	it('renders process data', () => {
		cy.mount(ProcessDataComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getProcessData: () => of(mockPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.get('table').should('exist');
		cy.get('tr[mat-row]').should('have.length', 2);
		cy.contains('applicantName');
		cy.contains('Max Mustermann');
		cy.contains('APPLICANT');
		cy.contains('caseNumber');
		cy.contains('12345');
	});

	it('shows empty message when no data', () => {
		const emptyPage: PageResponse<ProcessDataDTO> = {
			content: [],
			page: {totalElements: 0, totalPages: 0, number: 0, size: 10}
		};
		cy.mount(ProcessDataComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule],
			providers: [provideObliqueTestingConfiguration(), MockProvider(ProcessService, {getProcessData: () => of(emptyPage)})],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('i18n.none');
	});
});
