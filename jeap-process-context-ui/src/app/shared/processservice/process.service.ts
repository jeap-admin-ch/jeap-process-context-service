import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';
import {MessageDTO, PageResponse, ProcessDataDTO, ProcessDTO, ProcessInstanceLightDto, ProcessRelationDTO, RelationDTO} from './process.model';
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
			catchError(_ => of({ok: false})),
			map(response => response.ok)
		);
	}

	findProcesses(pageIndex: number, pageSize: number, processData?: string | null): Observable<PageResponse<ProcessInstanceLightDto>> {
		const requestUrl = `${ProcessService.url}/?page=${pageIndex}&size=${pageSize}&processData=${processData}`;
		return this.http.get<PageResponse<ProcessInstanceLightDto>>(requestUrl);
	}

	getProcessRelations(originProcessId: string, page: number, size: number): Observable<PageResponse<ProcessRelationDTO>> {
		const requestUrl = `${ProcessService.url}/${originProcessId}/process-relations?page=${page}&size=${size}`;
		return this.http.get<PageResponse<ProcessRelationDTO>>(requestUrl);
	}

	getRelations(originProcessId: string, page: number, size: number): Observable<PageResponse<RelationDTO>> {
		const requestUrl = `${ProcessService.url}/${originProcessId}/relations?page=${page}&size=${size}`;
		return this.http.get<PageResponse<RelationDTO>>(requestUrl);
	}

	getProcessData(originProcessId: string, page: number, size: number): Observable<PageResponse<ProcessDataDTO>> {
		const requestUrl = `${ProcessService.url}/${originProcessId}/process-data?page=${page}&size=${size}`;
		return this.http.get<PageResponse<ProcessDataDTO>>(requestUrl);
	}

	getMessages(originProcessId: string, page: number, size: number): Observable<PageResponse<MessageDTO>> {
		const requestUrl = `${ProcessService.url}/${originProcessId}/messages?page=${page}&size=${size}`;
		return this.http.get<PageResponse<MessageDTO>>(requestUrl);
	}
}
