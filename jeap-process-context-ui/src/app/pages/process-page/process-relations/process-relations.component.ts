import {AfterViewInit, Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild} from '@angular/core';
import {ProcessRelationDTO} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {ProcessRelationsListenerService} from '../../../shared/process-relations-listener.service';

@Component({
    selector: 'app-process-relations',
    templateUrl: './process-relations.component.html',
    styleUrls: [],
    standalone: false
})
export class ProcessRelationsComponent implements OnInit, AfterViewInit, OnChanges {
	@Input() processRelations: ProcessRelationDTO[];

	dataSource: MatTableDataSource<ProcessRelationDTO>;

	readonly COLUMN_NAME_RELATION = 'relation';
	readonly COLUMN_NAME_TYPE = 'type';
	readonly COLUMN_NAME_ID = 'id';
	readonly COLUMN_NAME_STATE = 'state';

	displayedColumns: string[] = [this.COLUMN_NAME_RELATION, this.COLUMN_NAME_TYPE, this.COLUMN_NAME_ID, this.COLUMN_NAME_STATE];

	@ViewChild(MatPaginator) paginator: MatPaginator;

	constructor(readonly translate: TranslateService, private readonly processRelationsClickListener: ProcessRelationsListenerService) {}

	ngOnInit(): void {
		this.dataSource = new MatTableDataSource(this.processRelations);
	}

	ngAfterViewInit() {
		this.dataSource.paginator = this.paginator;
	}

	ngOnChanges(changes: SimpleChanges): void {
		this.dataSource = new MatTableDataSource(this.processRelations);
	}

	emitClick(processId: string): void {
		this.processRelationsClickListener.emitClickEvent(processId);
	}
}
