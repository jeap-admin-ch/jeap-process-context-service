import {StartPageComponent} from './start-page.component';
import {TranslateModule} from '@ngx-translate/core';
import {MockComponent, MockProvider} from 'ng-mocks';
import {ProcessService} from '../../shared/processservice/process.service';
import {of} from 'rxjs';
import {MatPaginator} from '@angular/material/paginator';

import {mockProcessList} from '../../../../cypress/fixtures/MockData';
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
			providers: [MockProvider(ProcessService, { findProcesses: () => of(mockProcessList)})],
			schemas: [CUSTOM_ELEMENTS_SCHEMA]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'findProcesses')
				.as('processService')
				.returns(of(mockProcessList));
		});

		cy.get('form').should('exist');
		cy.get('table').find('tr').should('have.length', 3); // 1 header + 2 data rows

		cy.get('.reload-button').contains(/reload|search/i).as('reloadButton');
		cy.get('@reloadButton').first().click();
		cy.get('@processService').should('be.called');
	});
});
