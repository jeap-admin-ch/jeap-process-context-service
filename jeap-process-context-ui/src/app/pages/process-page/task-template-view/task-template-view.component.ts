import {Component, EventEmitter, HostListener, Input, Output} from '@angular/core';
import {TaskDTO} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';

@Component({
	selector: 'app-task-template-view',
	templateUrl: './task-template-view.component.html',
	styleUrls: ['./task-template-view.component.css'],
	standalone: false,
	host: {tabindex: '0', style: 'outline: none;'}
})
export class TaskTemplateViewComponent {
	@Input() tasks: TaskDTO[] = [];
	@Output() taskSelected = new EventEmitter<TaskDTO>();
	selectedTaskIndex: number = 0;

	constructor(readonly translate: TranslateService) {}

	selectTask(index: number) {
		this.selectedTaskIndex = index;
		this.taskSelected.emit(this.tasks[index]);
	}

	resetTaskIndex() {
		this.selectedTaskIndex = 0;
	}

	@HostListener('keydown', ['$event'])
	onKeyDown(event: KeyboardEvent) {
		if (event.key === 'ArrowDown' && this.selectedTaskIndex < this.tasks.length - 1) {
			event.preventDefault();
			this.selectTask(this.selectedTaskIndex + 1);
		} else if (event.key === 'ArrowUp' && this.selectedTaskIndex > 0) {
			event.preventDefault();
			this.selectTask(this.selectedTaskIndex - 1);
		}
	}
}
