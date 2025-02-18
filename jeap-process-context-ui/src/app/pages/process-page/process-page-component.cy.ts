import {ProcessPageComponent} from './process-page.component';
import {TranslateModule} from '@ngx-translate/core';

import {of} from 'rxjs';
import {ProcessService} from '../../shared/processservice/process.service';
import {LogDeepLinkService} from '../../shared/logdeeplink/logdeeplink.service';
import {MockProvider} from 'ng-mocks';

// @ts-ignore
import processData from '../../../../cypress/fixtures/process_1.json';
import {getTranslationServiceMockProvider} from '../../../../cypress/support/mockproviderFunctions';
import {RouterModule} from "@angular/router";
import {TaskTemplateViewComponent} from "./task-template-view/task-template-view.component";

describe('process-page-component.cy.ts', () => {

	it('mounts', () => {
		cy.mount(ProcessPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot()],
			declarations: [TaskTemplateViewComponent],
			providers: [
				MockProvider(ProcessService, {getProcess: () => of(processData)}),
				getTranslationServiceMockProvider('de'),
				MockProvider(LogDeepLinkService, {})
			]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'getProcess').returns(of(processData));
		});

		cy.contains('Race Across Switzerland');
		cy.contains('XXX');
	});

	it('load fr', () => {
		cy.mount(ProcessPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot()],
			declarations: [TaskTemplateViewComponent],
			providers: [
				MockProvider(ProcessService, {getProcess: () => of(processData)}),
				getTranslationServiceMockProvider('fr'),
				MockProvider(LogDeepLinkService, {})
			]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'getProcess').returns(of(processData));
		});

		cy.contains('[FR] Race Across Switzerland');
		cy.contains('XXX');
	});

	it('load it', () => {
		cy.mount(ProcessPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot()],
			declarations: [TaskTemplateViewComponent],
			providers: [
				MockProvider(ProcessService, {getProcess: () => of(processData)}),
				getTranslationServiceMockProvider('it'),
				MockProvider(LogDeepLinkService, {})
			]
		}).then(wrapper => {
			cy.stub((wrapper.component as any).processService, 'getProcess').returns(of(processData));
		});

		cy.contains('[IT] Race Across Switzerland');
		cy.contains('XXX');
	});
});
