import {NgModule} from '@angular/core';
import {ExtraOptions, RouterModule, Routes} from '@angular/router';
import {ObUnknownRouteModule} from '@oblique/oblique';
import {StartPageComponent} from './pages/start-page/start-page.component';
import {QdApplicationRoleFilter, QdAuthorizationGuard, QdRoleFilterMatcher} from '@quadrel-services/qd-auth';
import {ProcessInstanceByIdGuard} from './shared/guards/process-instance-by-id.guard';
import {ProcessPageComponent} from './pages/process-page/process-page.component';
import {ForbiddenPageComponent} from "./pages/error-pages/forbidden-page/forbidden-page.component";

const ROLE_FILTER_VIEW = QdApplicationRoleFilter.hasRole(
	QdRoleFilterMatcher.ANY,
	'processinstance',
	'view'
);

const routes: Routes = [
	{path: '', redirectTo: 'startpage', pathMatch: 'full'},
	{
		path: 'Forbidden',
		component: ForbiddenPageComponent
	},
	{
		path: 'startpage', component: StartPageComponent,
		canLoad: [QdAuthorizationGuard],
		canActivate: [QdAuthorizationGuard],
		data: {
			roleFilter: [ROLE_FILTER_VIEW]
		}
	},
	{
		path: 'process/:id', component: ProcessPageComponent,
		canLoad: [QdAuthorizationGuard],
		canActivate: [QdAuthorizationGuard],
		data: {
			roleFilter: [ROLE_FILTER_VIEW]
		}
	},
	{
		path: 'views',
		canLoad: [QdAuthorizationGuard],
		canActivateChild: [QdAuthorizationGuard],
		children: [
			{
				path: 'process-instance-by-id',
				canActivate: [ProcessInstanceByIdGuard],
				component: StartPageComponent
			}
		]
	},

	{path: '**', redirectTo: 'unknown-route'},

];

export const routingConfiguration: ExtraOptions = {
	paramsInheritanceStrategy: 'always'
};

@NgModule({
	imports: [RouterModule.forRoot(routes, routingConfiguration), ObUnknownRouteModule],
	exports: [RouterModule]
})
export class AppRoutingModule {
}
