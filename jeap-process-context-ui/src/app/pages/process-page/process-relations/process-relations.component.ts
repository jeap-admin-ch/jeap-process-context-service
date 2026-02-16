import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {ProcessRelationDTO} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';
import {MatPaginator} from '@angular/material/paginator';
import {ProcessService} from '../../../shared/processservice/process.service';
import {Subscription} from 'rxjs';

@Component({
	selector: 'app-process-relations',
	templateUrl: './process-relations.component.html',
	styleUrls: [],
	standalone: false
})
export class ProcessRelationsComponent implements OnInit, OnChanges, OnDestroy {
	@Input() originProcessId: string;
	@Input() reloadTrigger: number;

	processRelations: ProcessRelationDTO[] = [];
	totalElements = 0;

	readonly COLUMN_NAME_RELATION = 'relation';
	readonly COLUMN_NAME_TYPE = 'type';
	readonly COLUMN_NAME_ID = 'id';
	readonly COLUMN_NAME_STATE = 'state';

	displayedColumns: string[] = [this.COLUMN_NAME_RELATION, this.COLUMN_NAME_TYPE, this.COLUMN_NAME_ID, this.COLUMN_NAME_STATE];

	private _paginator: MatPaginator;
	private paginatorSubscription: Subscription;

	@ViewChild(MatPaginator)
	set paginator(paginator: MatPaginator) {
		if (paginator && paginator !== this._paginator) {
			this._paginator = paginator;
			this.paginatorSubscription?.unsubscribe();
			this.paginatorSubscription = paginator.page.subscribe(() => {
				this.loadProcessRelations(paginator.pageIndex);
			});
		}
	}

	constructor(
		readonly translate: TranslateService,
		private readonly processService: ProcessService
	) {}

	ngOnInit(): void {
		this.loadProcessRelations(0);
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['originProcessId'] && !changes['originProcessId'].firstChange) {
			this.loadProcessRelations(0);
		} else if (changes['reloadTrigger'] && !changes['reloadTrigger'].firstChange) {
			this.loadProcessRelations(this._paginator?.pageIndex ?? 0);
		}
	}

	ngOnDestroy(): void {
		this.paginatorSubscription?.unsubscribe();
	}

	private loadProcessRelations(pageIndex: number): void {
		const pageSize = 5;
		this.processService.getProcessRelations(this.originProcessId, pageIndex, pageSize).subscribe(response => {
			this.processRelations = response.content;
			this.totalElements = response.page.totalElements;
		});
	}
}
