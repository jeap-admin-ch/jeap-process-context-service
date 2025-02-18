import {Component, Input} from '@angular/core';
import {TaskDTO} from '../../../shared/processservice/process.model';
import {TranslateService} from '@ngx-translate/core';

@Component({
	selector: 'app-task-detail-view',
	templateUrl: './task-detail-view.component.html',
	styleUrl: './task-detail-view.component.scss'
})
export class TaskDetailViewComponent {
	@Input() task: TaskDTO | null = null;

	constructor(readonly translate: TranslateService) {}
}
