import {StartPageComponent} from './start-page.component';
import {TranslateModule} from '@ngx-translate/core';
import {MockComponent, MockProvider} from 'ng-mocks';
import {ProcessService} from '../../shared/processservice/process.service';
import {of} from 'rxjs';
import {MatPaginator} from '@angular/material/paginator';

// @ts-ignore
import processList from '../../../../cypress/fixtures/processList.json';
import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';

import {ReactiveFormsModule} from '@angular/forms';
import {MatTableModule as MatTableModule} from '@angular/material/table';
import {RouterModule} from "@angular/router";

describe('process-page-component.cy.ts', () => {
	it('mounts', () => {
		const mockMatPaginator = MockComponent(MatPaginator);

		cy.mount(StartPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot(), MatTableModule, ReactiveFormsModule],
			declarations: [mockMatPaginator],
			providers: [MockProvider(ProcessService, {findProcesses: () => of(processList)})],
			schemas: [CUSTOM_ELEMENTS_SCHEMA]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'findProcesses')
				.as('processService')
				.returns(of(processList));
		});

		cy.contains('i18n.process.openProcess');
		cy.get('table').find('tr').should('have.length', 3);
		cy.get('button').contains('i18n.process.reload').as('reloadButton');

		cy.get('@reloadButton').click();
		cy.get('@processService').should('be.called');

		cy.get('button').contains('i18n.process.search').as('searchButton');
		cy.get('@processService').should('be.called');
	});
});
