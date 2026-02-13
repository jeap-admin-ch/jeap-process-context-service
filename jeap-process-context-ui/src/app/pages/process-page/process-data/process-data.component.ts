import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {ProcessDataDTO} from '../../../shared/processservice/process.model';
import {MatPaginator} from '@angular/material/paginator';
import {ProcessService} from '../../../shared/processservice/process.service';
import {ProcessRelationsListenerService} from '../../../shared/process-relations-listener.service';
import {Subscription} from 'rxjs';

@Component({
	selector: 'app-process-data',
	templateUrl: './process-data.component.html',
	styles: [':host { display: block; width: 100%; }', 'table { width: 100%; }'],
	standalone: false
})
export class ProcessDataComponent implements OnInit, OnChanges, OnDestroy {
	@Input() originProcessId: string;

	processData: ProcessDataDTO[] = [];
	totalElements = 0;

	displayedColumns: string[] = ['key', 'value', 'role'];

	private _paginator: MatPaginator;
	private paginatorSubscription: Subscription;

	@ViewChild(MatPaginator)
	set paginator(paginator: MatPaginator) {
		if (paginator && paginator !== this._paginator) {
			this._paginator = paginator;
			this.paginatorSubscription?.unsubscribe();
			this.paginatorSubscription = paginator.page.subscribe(() => {
				this.loadProcessData(paginator.pageIndex);
			});
		}
	}

	private refreshSubscription: Subscription;

	constructor(
		private readonly processService: ProcessService,
		private readonly processRelationsClickListener: ProcessRelationsListenerService
	) {}

	ngOnInit(): void {
		this.loadProcessData(0);
		this.refreshSubscription = this.processRelationsClickListener.refreshTable$.subscribe(() => {
			this.loadProcessData(this._paginator?.pageIndex ?? 0);
		});
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['originProcessId'] && !changes['originProcessId'].firstChange) {
			this.loadProcessData(0);
		}
	}

	ngOnDestroy(): void {
		this.refreshSubscription?.unsubscribe();
		this.paginatorSubscription?.unsubscribe();
	}

	private loadProcessData(pageIndex: number): void {
		const pageSize = 10;
		this.processService.getProcessData(this.originProcessId, pageIndex, pageSize).subscribe(response => {
			this.processData = response.content;
			this.totalElements = response.page.totalElements;
		});
	}
}
