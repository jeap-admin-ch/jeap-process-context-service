import {ProcessPageComponent} from './process-page.component';
import {TranslateModule} from '@ngx-translate/core';

import {of} from 'rxjs';
import {ProcessService} from '../../shared/processservice/process.service';
import {LogDeepLinkService} from '../../shared/logdeeplink/logdeeplink.service';
import {MockProvider} from 'ng-mocks';

import {mockMessagesPage, mockProcess_1, mockProcessDataPage, mockProcessRelationsPage, mockRelationsPage} from '../../../../cypress/fixtures/MockData';
import {ActivatedRoute} from '@angular/router';
import {RouterModule} from '@angular/router';
import {TaskTemplateViewComponent} from './task-template-view/task-template-view.component';
import {ProcessRelationsComponent} from './process-relations/process-relations.component';
import {RelationsComponent} from './relations/relations.component';
import {ProcessDataComponent} from './process-data/process-data.component';
import {MessagesComponent} from './messages/messages.component';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

describe('process-page-component.cy.ts', () => {
	it('mounts', () => {
		cy.mount(ProcessPageComponent, {
			imports: [RouterModule.forRoot([]), TranslateModule.forRoot({defaultLanguage: 'de'})],
			declarations: [TaskTemplateViewComponent, ProcessRelationsComponent, RelationsComponent, ProcessDataComponent, MessagesComponent],
			providers: [
				provideObliqueTestingConfiguration(),
				MockProvider(ProcessService, {
					getProcess: () => of(mockProcess_1),
					getProcessRelations: () => of(mockProcessRelationsPage),
					getRelations: () => of(mockRelationsPage),
					getProcessData: () => of(mockProcessDataPage),
					getMessages: () => of(mockMessagesPage)
				}),
				MockProvider(LogDeepLinkService, {getLogDeepLink: () => of('')}),
				{
					provide: ActivatedRoute,
					useValue: {params: of({id: mockProcess_1.originProcessId})}
				}
			]
		});

		cy.contains('Race Across Switzerland');
		cy.contains('XXX');
	});
});
