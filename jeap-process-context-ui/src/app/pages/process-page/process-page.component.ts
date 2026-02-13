import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ProcessDTO, TaskDTO} from '../../shared/processservice/process.model';
import {ActivatedRoute} from '@angular/router';
import {ProcessService} from '../../shared/processservice/process.service';
import {Subscription, take} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {MatRadioChange} from '@angular/material/radio';
import {ProcessRelationsListenerService} from '../../shared/process-relations-listener.service';
import {TaskTemplateViewComponent} from './task-template-view/task-template-view.component';

@Component({
	selector: 'app-exam-page',
	templateUrl: './process-page.component.html',
	styleUrls: ['./process-page.component.css'],
	standalone: false
})
export class ProcessPageComponent implements OnInit, OnDestroy, AfterViewInit {
	process: ProcessDTO | undefined;
	snapshot: boolean | undefined;
	processId!: string;
	userTaskViewType: string;
	selectedTask: TaskDTO | null = null; // The selected task
	@ViewChild(TaskTemplateViewComponent) taskTemplateViewComponent: TaskTemplateViewComponent;

	private readonly clickSubscription: Subscription;
	private readonly localStorageKey = 'task-view-preference';
	private readonly defaultTaskViewType = '1';

	constructor(
		private readonly route: ActivatedRoute,
		private readonly processService: ProcessService,
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
				this.snapshot = value.snapshot;
				this.processRelationsClickListener.triggerTableRefresh();
				this.selectedTask = this.process.tasks[0];
				if (this.taskTemplateViewComponent) {
					this.taskTemplateViewComponent.resetTaskIndex();
				}
			});
	}

	taskViewRadioButtonGroupChange(data: MatRadioChange) {
		localStorage.setItem(this.localStorageKey, data.value);
		this.selectedTask = this.process?.tasks?.length ? this.process.tasks[0] : null;
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

	// Called when a task is selected from the list
	onTaskSelected(task: TaskDTO): void {
		this.selectedTask = task;
	}
}
