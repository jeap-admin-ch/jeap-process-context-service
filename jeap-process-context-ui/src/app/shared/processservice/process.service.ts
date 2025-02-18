import {Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';
import {ProcessDTO, ProcessInstanceListDto} from './process.model';
import {catchError, map} from 'rxjs/operators';
import {environment} from '../../../environments/environment';

@Injectable({
	providedIn: 'root'
})
export class ProcessService {
	private static readonly url: string = environment.BACKEND_SERVICE_API + '/processes';

	constructor(readonly http: HttpClient) {}

	getProcess(processId: string | null): Observable<ProcessDTO> {
		return this.http.get<ProcessDTO>(ProcessService.url + '/' + processId).pipe(
			catchError(error => {
				console.error('Error fetching process:', error);
				return throwError(error);
			})
		);
	}

	hasProcess(processId: string): Observable<boolean> {
		return this.http.head<void>(`${ProcessService.url}/${processId}`, {observe: 'response'}).pipe(
			catchError((_) => of({ok: false})),
			map(response => response.ok)
		);
	}

	findProcesses(pageIndex: number, pageSize: number, processData?: string | null): Observable<ProcessInstanceListDto> {
		const requestUrl = `${ProcessService.url}/?pageIndex=${pageIndex}&pageSize=${pageSize}&processData=${processData}`;
		return this.http.get<ProcessInstanceListDto>(requestUrl);
	}
}
