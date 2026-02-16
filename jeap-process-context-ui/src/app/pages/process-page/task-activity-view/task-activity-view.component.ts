import {AfterViewInit, Component, EventEmitter, HostListener, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild} from '@angular/core';
import {TaskDTO, TaskState} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';
import {MatSort} from '@angular/material/sort';
import {MatTableDataSource} from '@angular/material/table';

@Component({
	selector: 'app-task-activity-view',
	templateUrl: './task-activity-view.component.html',
	styleUrls: ['./task-activity-view.component.css'],
	standalone: false,
	host: {tabindex: '0', style: 'outline: none;'}
})
export class TaskActivityViewComponent implements OnInit, AfterViewInit, OnChanges {
	@Input() tasks: TaskDTO[] = [];
	@Output() taskSelected = new EventEmitter<TaskDTO>();

	sortedTasks: TaskDTO[] = [];
	selectedTaskIndex: number = -1;
	dataSource: MatTableDataSource<any>;

	@ViewChild(MatSort) sort: MatSort;

	constructor(readonly translate: TranslateService) {}

	ngOnInit(): void {
		this.refreshTable();
		if (this.sortedTasks.length > 0) {
			this.selectedTaskIndex = 0;
			this.taskSelected.emit(this.sortedTasks[0]);
		}
	}

	ngAfterViewInit(): void {
		this.dataSource.sort = this.sort;
	}

	refreshTable(): void {
		this.sortedTasks = this.generateActivityTasks(this.tasks);
		this.dataSource = new MatTableDataSource(this.sortedTasks);
	}

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['tasks']) {
			this.refreshTable();
		}
	}

	private generateActivityTasks(tasks: TaskDTO[]): TaskDTO[] {
		const filteredTasks: TaskDTO[] = tasks.filter(task => {
			return (task.state === TaskState.PLANNED && task.lifecycle === 'DYNAMIC') || task.state === TaskState.COMPLETED;
		});

		const plannedTasks: TaskDTO[] = filteredTasks.filter(task => {
			return task.state !== TaskState.COMPLETED;
		});

		const completedTasks: TaskDTO[] = filteredTasks.filter(task => {
			return task.state === TaskState.COMPLETED;
		});

		plannedTasks.sort((taskA, taskB) => {
			const dateA = new Date(taskA.createdAt);
			const dateB = new Date(taskB.createdAt);
			return dateB.getTime() - dateA.getTime();
		});

		completedTasks.sort((taskA, taskB) => {
			if (taskA.completedAt === null || taskB.completedAt === null) {
				return 0;
			}
			const dateA = new Date(taskA.completedAt);
			const dateB = new Date(taskB.completedAt);
			return dateB.getTime() - dateA.getTime();
		});

		return [...plannedTasks, ...completedTasks];
	}

	selectTask(task: TaskDTO) {
		this.selectedTaskIndex = this.sortedTasks.indexOf(task);
		this.taskSelected.emit(task);
	}

	@HostListener('keydown', ['$event'])
	onKeyDown(event: KeyboardEvent) {
		if (!this.sortedTasks.length) return;
		if (event.key === 'ArrowDown') {
			event.preventDefault();
			const nextIndex = Math.min(this.selectedTaskIndex + 1, this.sortedTasks.length - 1);
			this.selectedTaskIndex = nextIndex;
			this.taskSelected.emit(this.sortedTasks[nextIndex]);
		} else if (event.key === 'ArrowUp' && this.selectedTaskIndex > 0) {
			event.preventDefault();
			const prevIndex = this.selectedTaskIndex - 1;
			this.selectedTaskIndex = prevIndex;
			this.taskSelected.emit(this.sortedTasks[prevIndex]);
		}
	}
}
