import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {ProcessService} from '../processservice/process.service';
import {map} from 'rxjs/operators';

@Injectable({providedIn: 'root'})
export class ProcessInstanceByIdGuard implements CanActivate {
	constructor(
		private readonly processService: ProcessService,
		private readonly router: Router
	) {}

	canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
		const processId = route.queryParams['originProcessId'];
		return this.processService.hasProcess(processId).pipe(
			map(hasProcess => {
				const destinationPath = hasProcess ? `process/${processId}` : `startpage?processIdQuery=${processId}`;
				return this.router.parseUrl(destinationPath);
			})
		);
	}
}
