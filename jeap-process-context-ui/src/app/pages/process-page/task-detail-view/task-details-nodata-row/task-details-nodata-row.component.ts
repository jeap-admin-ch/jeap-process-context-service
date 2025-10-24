import {Component} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';

@Component({
    selector: 'app-task-details-nodata-row',
    templateUrl: './task-details-nodata-row.component.html',
    styleUrl: './task-details-nodata-row.component.scss',
    standalone: false
})
export class TaskDetailsNodataRowComponent {
	constructor(readonly translate: TranslateService) {}
}
