import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {RelationDTO} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';
import {MatPaginator} from '@angular/material/paginator';
import {ProcessService} from '../../../shared/processservice/process.service';
import {ProcessRelationsListenerService} from '../../../shared/process-relations-listener.service';
import {Subscription} from 'rxjs';

@Component({
	selector: 'app-relations',
	templateUrl: './relations.component.html',
	styles: [':host { display: block; width: 100%; }', 'table { width: 100%; }'],
	standalone: false
})
export class RelationsComponent implements OnInit, OnChanges, OnDestroy {
	@Input() originProcessId: string;

	relations: RelationDTO[] = [];
	totalElements = 0;

	displayedColumns: string[] = ['createdAt', 'predicateType', 'subjectType', 'subjectId', 'objectType', 'objectId'];

	private _paginator: MatPaginator;
	private paginatorSubscription: Subscription;

	@ViewChild(MatPaginator)
	set paginator(paginator: MatPaginator) {
		if (paginator && paginator !== this._paginator) {
			this._paginator = paginator;
			this.paginatorSubscription?.unsubscribe();
			this.paginatorSubscription = paginator.page.subscribe(() => {
				this.loadRelations(paginator.pageIndex);
			});
		}
	}

	private refreshSubscription: Subscription;

	constructor(
		readonly translate: TranslateService,
		private readonly processService: ProcessService,
		private readonly processRelationsClickListener: ProcessRelationsListenerService
	) {}

	ngOnInit(): void {
		this.loadRelations(0);
		this.refreshSubscription = this.processRelationsClickListener.refreshTable$.subscribe(() => {
			this.loadRelations(this._paginator?.pageIndex ?? 0);
		});
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['originProcessId'] && !changes['originProcessId'].firstChange) {
			this.loadRelations(0);
		}
	}

	ngOnDestroy(): void {
		this.refreshSubscription?.unsubscribe();
		this.paginatorSubscription?.unsubscribe();
	}

	private loadRelations(pageIndex: number): void {
		const pageSize = 10;
		this.processService.getRelations(this.originProcessId, pageIndex, pageSize).subscribe(response => {
			this.relations = response.content;
			this.totalElements = response.page.totalElements;
		});
	}
}
