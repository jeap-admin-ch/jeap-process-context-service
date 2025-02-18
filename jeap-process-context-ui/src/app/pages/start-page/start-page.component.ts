import {AfterViewInit, ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ProcessService} from '../../shared/processservice/process.service';
import {MatSort} from '@angular/material/sort';
import {ProcessInstanceLightDto, ProcessInstanceListDto} from '../../shared/processservice/process.model';
import {Observable} from 'rxjs';
import {startWith, switchMap} from 'rxjs/operators';
import {FormBuilder, Validators} from '@angular/forms';
import {MatTableDataSource} from '@angular/material/table';
import {MatPaginator as MatPaginator} from '@angular/material/paginator';
import {TranslateService} from '@ngx-translate/core';

@Component({
	selector: 'app-start-page',
	templateUrl: './start-page.component.html'
})
export class StartPageComponent implements AfterViewInit, OnInit {

	@ViewChild(MatSort) sort: MatSort;
	@ViewChild(MatPaginator) paginator!: MatPaginator;

	isLoadingResults = true;

	jumpToFormGroup = this.formBuilder.group({
			processIdControl: ['', [Validators.required,
				Validators.minLength(1)]]
		}
	);

	searchProcessDataFormGroup = this.formBuilder.group({
			processDataControl: ['']
		}
	);

	dataSource = new MatTableDataSource<ProcessInstanceLightDto>([]);
	pageSizeOptions = [10, 20, 30];

	data: ProcessInstanceLightDto[] = [];
	resultsLength = 0;

	readonly COLUMN_NAME_ORIGIN_PROCESS_ID = 'originProcessId';
	readonly COLUMN_NAME_CREATED_AT = 'createdAt';
	readonly COLUMN_NAME_PROCESS_TEMPLATE = 'processTemplate';
	readonly COLUMN_NAME_STATE = 'state';
	readonly COLUMN_NAME_LAST_MESSAGE_CREATED_AT = 'lastMessageCreatedAt';

	displayedColumns: string[] = [this.COLUMN_NAME_ORIGIN_PROCESS_ID, this.COLUMN_NAME_PROCESS_TEMPLATE,
		this.COLUMN_NAME_STATE, this.COLUMN_NAME_CREATED_AT, this.COLUMN_NAME_LAST_MESSAGE_CREATED_AT];


	constructor(private readonly route: ActivatedRoute,
				private readonly router: Router,
				private readonly processService: ProcessService,
				private readonly formBuilder: FormBuilder,
				readonly translate: TranslateService,
				private cd: ChangeDetectorRef
	) {	}

	ngOnInit(): void {
		this.dataSource.sort = this.sort;
		this.dataSource.paginator = this.paginator;
		const processIdQuery = this.route.snapshot.queryParams['processIdQuery'];
		if (!!processIdQuery) {
			this.jumpToFormGroup.controls.processIdControl.setValue(processIdQuery);
		}
	}

	ngAfterViewInit(): void {
		this.paginator.page.pipe(
			startWith({}),
			switchMap(() => {
				return this.loadProcessInstances(this.paginator.pageIndex);
			})
		).subscribe(processList => this.processInstancesLoaded(processList));
		// To avoid the ExpressionChangedAfterItHasBeenCheckedError: Find more at https://angular.io/errors/NG0100
		this.cd.detectChanges();
	}

	reload(): void {
		if (this.paginator.pageIndex > 0) {
			this.paginator.firstPage();
		} else {
			this.loadProcessInstances(0).subscribe(
				processList => this.processInstancesLoaded(processList));
		}
	}

	findProcessId(): void {
		let processId = this.jumpToFormGroup.get('processIdControl')!.value
		this.processService.getProcess(processId).subscribe(
			result => {
				this.router.navigate(['/process/' + result.originProcessId]);

			},
			error => {
				// @ts-ignore
				this.jumpToFormGroup.get('processIdControl').setErrors({'processNotFound': true});
				console.error('Could not get result', error);
			}
		);

	}

	private processInstancesLoaded(processInstancesList: ProcessInstanceListDto): void {
		this.isLoadingResults = false;
		this.resultsLength = processInstancesList.totalCount;
		this.data = processInstancesList.processInstanceLightDtoList;
	}

	private loadProcessInstances(pageIndex: number): Observable<ProcessInstanceListDto> {
		this.isLoadingResults = true;
		const pageSize = this.paginator.pageSize;
		// @ts-ignore
		const processData = this.searchProcessDataFormGroup.get('processDataControl').value;
		return this.processService.findProcesses(pageIndex, pageSize, processData);
	}
}
