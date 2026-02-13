import {TranslateModule} from '@ngx-translate/core';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatTooltipModule} from '@angular/material/tooltip';
import {of} from 'rxjs';
import {MockProvider} from 'ng-mocks';
import {MessagesComponent} from './messages.component';
import {ProcessService} from '../../../shared/processservice/process.service';
import {LogDeepLinkService} from '../../../shared/logdeeplink/logdeeplink.service';
import {MessageDTO, PageResponse} from '../../../shared/processservice/process.model';
import {provideObliqueTestingConfiguration} from '@oblique/oblique';

const mockPage: PageResponse<MessageDTO> = {
	content: [
		{
			id: 'msg-1',
			name: 'EventCreated',
			createdAt: '2024-01-15T10:00:00Z',
			receivedAt: '2024-01-15T10:00:01Z',
			relatedOriginTaskIds: [],
			messageData: [{key: 'dataKey', value: 'dataValue', role: 'SYSTEM'}],
			messageDataTruncated: false,
			traceId: 'trace-abc-123'
		}
	],
	page: {totalElements: 1, totalPages: 1, number: 0, size: 10}
};

const mockPageTruncated: PageResponse<MessageDTO> = {
	content: [
		{
			id: 'msg-2',
			name: 'TruncatedEvent',
			createdAt: '2024-01-15T10:00:00Z',
			receivedAt: '2024-01-15T10:00:01Z',
			relatedOriginTaskIds: [],
			messageData: [{key: 'dataKey', value: 'dataValue', role: 'SYSTEM'}],
			messageDataTruncated: true,
			traceId: 'trace-abc-456'
		}
	],
	page: {totalElements: 1, totalPages: 1, number: 0, size: 10}
};

describe('MessagesComponent', () => {
	it('renders messages', () => {
		cy.mount(MessagesComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule, MatTooltipModule],
			providers: [
				provideObliqueTestingConfiguration(),
				MockProvider(ProcessService, {getMessages: () => of(mockPage)}),
				MockProvider(LogDeepLinkService, {getLogDeepLink: () => of('')})
			],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('EventCreated');
		cy.contains('trace-abc-123');
		cy.contains('dataKey');
		cy.contains('dataValue');
	});

	it('shows truncation warning when messageDataTruncated is true', () => {
		cy.mount(MessagesComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule, MatTooltipModule],
			providers: [
				provideObliqueTestingConfiguration(),
				MockProvider(ProcessService, {getMessages: () => of(mockPageTruncated)}),
				MockProvider(LogDeepLinkService, {getLogDeepLink: () => of('')})
			],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('i18n.process.message.messageDataTruncated');
	});

	it('shows empty message when no messages', () => {
		const emptyPage: PageResponse<MessageDTO> = {
			content: [],
			page: {totalElements: 0, totalPages: 0, number: 0, size: 10}
		};
		cy.mount(MessagesComponent, {
			imports: [TranslateModule.forRoot(), MatTableModule, MatPaginatorModule, MatTooltipModule],
			providers: [
				provideObliqueTestingConfiguration(),
				MockProvider(ProcessService, {getMessages: () => of(emptyPage)}),
				MockProvider(LogDeepLinkService, {getLogDeepLink: () => of('')})
			],
			componentProperties: {originProcessId: 'test-id'}
		});

		cy.contains('i18n.none');
	});
});
