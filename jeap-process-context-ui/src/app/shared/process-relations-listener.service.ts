import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';

@Injectable({
	providedIn: 'root'
})
export class ProcessRelationsListenerService {
	private readonly clickSubject = new Subject<string>();
	private readonly refreshTableSource = new Subject<void>();
	// eslint-disable-next-line @typescript-eslint/member-ordering
	clickEvent$ = this.clickSubject.asObservable();
	// eslint-disable-next-line @typescript-eslint/member-ordering
	refreshTable$ = this.refreshTableSource.asObservable();

	emitClickEvent(processId: string): void {
		this.clickSubject.next(processId);
	}

	triggerTableRefresh() {
		this.refreshTableSource.next();
	}
}
