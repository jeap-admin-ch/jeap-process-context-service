import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ProcessDTO, ProcessRelationDTO, TaskDTO} from '../../shared/processservice/process.model';
import {ActivatedRoute} from '@angular/router';
import {ProcessService} from '../../shared/processservice/process.service';
import {Subscription, take} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {MatRadioChange} from '@angular/material/radio';
import {ProcessRelationsListenerService} from '../../shared/process-relations-listener.service';
import {LogDeepLinkService} from '../../shared/logdeeplink/logdeeplink.service';
import {TaskTemplateViewComponent} from "./task-template-view/task-template-view.component";

@Component({
    selector: 'app-exam-page',
    templateUrl: './process-page.component.html',
    styleUrls: ['./process-page.component.css'],
    standalone: false
})
export class ProcessPageComponent implements OnInit, OnDestroy, AfterViewInit {
	process: ProcessDTO | undefined;
	snapshot: boolean | undefined;
	processRelations: ProcessRelationDTO[];
	processId!: string;
	userTaskViewType: string;
	logDeepLink: string;
	logDeepLinkTemplate: string;
	selectedTask: TaskDTO | null = null; // The selected task
	@ViewChild(TaskTemplateViewComponent) taskTemplateViewComponent: TaskTemplateViewComponent;

	private readonly clickSubscription: Subscription;
	private readonly localStorageKey = 'task-view-preference';
	private readonly defaultTaskViewType = '1';

	constructor(
		private readonly route: ActivatedRoute,
		private readonly processService: ProcessService,
		private readonly logDeepLinkService: LogDeepLinkService,
		readonly translate: TranslateService,
		readonly processRelationsClickListener: ProcessRelationsListenerService
	) {
		this.clickSubscription = this.processRelationsClickListener.clickEvent$.subscribe((processId: string) => {
			this.processId = processId;
			this.reload();
		});
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			this.processId = params['id'];
			this.reload();
		});
		this.logDeepLinkService.getLogDeepLink().subscribe(template => {
			this.logDeepLinkTemplate = template;
		});
		this.loadTaskViewPreferences();
	}

	ngAfterViewInit() {
		// Sicherstellen, dass taskTemplateViewComponent initialisiert ist
		if (this.taskTemplateViewComponent) {
			this.taskTemplateViewComponent.resetTaskIndex();
		}
	}

	ngOnDestroy(): void {
		this.clickSubscription.unsubscribe();
	}
	reload() {
		this.processService
			.getProcess(this.processId)
			.pipe(take(1))
			.subscribe(value => {
				this.process = value;
				this.snapshot = value.snapshot
				this.processRelations = value.processRelations;
				this.processRelationsClickListener.triggerTableRefresh();
				this.selectedTask = this.process.tasks[0];
				if (this.taskTemplateViewComponent) {
					this.taskTemplateViewComponent.resetTaskIndex();
				}
			});
	}

	taskViewRadioButtonGroupChange(data: MatRadioChange) {
		localStorage.setItem(this.localStorageKey, data.value);
		this.selectedTask = null;
	}

	private loadTaskViewPreferences(): void {
		this.userTaskViewType = this.getTaskViewType();
	}

	private getTaskViewType(): string {
		const taskViewType = localStorage.getItem(this.localStorageKey);
		if (taskViewType == null) {
			localStorage.setItem(this.localStorageKey, this.defaultTaskViewType);
			return this.defaultTaskViewType;
		}
		return taskViewType;
	}

	openDeepLink(traceId: string) {
		this.logDeepLink = this.logDeepLinkService.replaceTraceId(this.logDeepLinkTemplate, traceId);
	}

	// Called when a task is selected from the list
	onTaskSelected(task: TaskDTO): void {
		this.selectedTask = task;
	}

}
