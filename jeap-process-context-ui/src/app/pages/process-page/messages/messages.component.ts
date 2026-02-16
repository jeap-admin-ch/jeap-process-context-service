import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {MessageDTO} from '../../../shared/processservice/process.model';
import {MatPaginator} from '@angular/material/paginator';
import {ProcessService} from '../../../shared/processservice/process.service';
import {LogDeepLinkService} from '../../../shared/logdeeplink/logdeeplink.service';
import {Subscription} from 'rxjs';

@Component({
	selector: 'app-messages',
	templateUrl: './messages.component.html',
	styles: [':host { display: block; width: 100%; }', 'table { width: 100%; }'],
	standalone: false
})
export class MessagesComponent implements OnInit, OnChanges, OnDestroy {
	@Input() originProcessId: string;
	@Input() reloadTrigger: number;

	messages: MessageDTO[] = [];
	totalElements = 0;
	logDeepLink: string;
	logDeepLinkTemplate: string;

	displayedColumns: string[] = ['name', 'createdAt', 'receivedAt', 'traceId', 'relatedOriginTaskIds', 'messageData'];

	private _paginator: MatPaginator;
	private paginatorSubscription: Subscription;

	@ViewChild(MatPaginator)
	set paginator(paginator: MatPaginator) {
		if (paginator && paginator !== this._paginator) {
			this._paginator = paginator;
			this.paginatorSubscription?.unsubscribe();
			this.paginatorSubscription = paginator.page.subscribe(() => {
				this.loadMessages(paginator.pageIndex);
			});
		}
	}

	constructor(
		private readonly processService: ProcessService,
		private readonly logDeepLinkService: LogDeepLinkService
	) {}

	ngOnInit(): void {
		this.loadMessages(0);
		this.logDeepLinkService.getLogDeepLink().subscribe(template => {
			this.logDeepLinkTemplate = template;
		});
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['originProcessId'] && !changes['originProcessId'].firstChange) {
			this.loadMessages(0);
		} else if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
			this.loadMessages(this._paginator?.pageIndex ?? 0);
		}
	}

	ngOnDestroy(): void {
		this.paginatorSubscription?.unsubscribe();
	}

	openDeepLink(traceId: string): void {
		this.logDeepLink = this.logDeepLinkService.replaceTraceId(this.logDeepLinkTemplate, traceId);
	}

	private loadMessages(pageIndex: number): void {
		const pageSize = 10;
		this.processService.getMessages(this.originProcessId, pageIndex, pageSize).subscribe(response => {
			this.messages = response.content;
			this.totalElements = response.page.totalElements;
		});
	}
}
