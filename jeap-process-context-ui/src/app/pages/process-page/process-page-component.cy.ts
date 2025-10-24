import {ProcessPageComponent} from './process-page.component';
import {TranslateModule} from '@ngx-translate/core';

import {of} from 'rxjs';
import {ProcessService} from '../../shared/processservice/process.service';
import {LogDeepLinkService} from '../../shared/logdeeplink/logdeeplink.service';
import {MockProvider} from 'ng-mocks';

import {mockProcess_1} from '../../../../cypress/fixtures/MockData'
import {RouterModule} from "@angular/router";
import {TaskTemplateViewComponent} from "./task-template-view/task-template-view.component";
import {provideObliqueTestingConfiguration} from "@oblique/oblique";

describe('process-page-component.cy.ts', () => {

	it('mounts', () => {
		cy.mount(ProcessPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot()],
			declarations: [TaskTemplateViewComponent],
			providers: [
				provideObliqueTestingConfiguration(),
				MockProvider(ProcessService, {getProcess: () => of(mockProcess_1)}),
				MockProvider(LogDeepLinkService, {})
			]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'getProcess').returns(of(mockProcess_1));
		});

		cy.contains('Race Across Switzerland');
		cy.contains('XXX');
	});
});
