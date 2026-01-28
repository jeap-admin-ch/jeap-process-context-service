import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {
	OB_PAMS_CONFIGURATION,
	ObButtonModule,
	ObEPamsEnvironment,
	ObErrorMessagesModule,
	ObExternalLinkModule,
	ObHttpApiInterceptor,
	ObMasterLayoutConfig,
	ObMasterLayoutModule,
	provideObliqueConfiguration
} from '@oblique/oblique';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {CommonModule, DatePipe, JsonPipe, NgForOf, NgIf, registerLocaleData} from '@angular/common';
import localeDECH from '@angular/common/locales/de-CH';
import localeFRCH from '@angular/common/locales/fr-CH';
import localeITCH from '@angular/common/locales/it-CH';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {TranslateModule} from '@ngx-translate/core';
import {appSetup, authConfig} from '../environments/environment';

import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule as MatFormFieldModule} from '@angular/material/form-field';
import {MatColumnDef, MatHeaderCell, MatHeaderCellDef, MatTableModule as MatTableModule} from '@angular/material/table';
import {MatInputModule as MatInputModule} from '@angular/material/input';
import {MatPaginatorModule as MatPaginatorModule} from '@angular/material/paginator';
import {MatExpansionModule} from '@angular/material/expansion';

import {MatIcon, MatIconModule} from '@angular/material/icon';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatSortModule} from '@angular/material/sort';

import {StartPageComponent} from './pages/start-page/start-page.component';

import {ProcessPageComponent} from './pages/process-page/process-page.component';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatListItem, MatListItemLine, MatListModule} from '@angular/material/list';
import {TaskTemplateViewComponent} from './pages/process-page/task-template-view/task-template-view.component';
import {TaskActivityViewComponent} from './pages/process-page/task-activity-view/task-activity-view.component';
import {MatRadioModule} from '@angular/material/radio';
import {ProcessRelationsComponent} from './pages/process-page/process-relations/process-relations.component';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ForbiddenPageComponent} from './pages/error-pages/forbidden-page/forbidden-page.component';
import {TaskDetailViewComponent} from './pages/process-page/task-detail-view/task-detail-view.component';
import {TaskDetailsNodataRowComponent} from './pages/process-page/task-detail-view/task-details-nodata-row/task-details-nodata-row.component';
import {QdAuthModule, QdConfigService} from "@quadrel-services/qd-auth";

export type QdShellHeaderWidgetEnvironment = 'DEV' | 'TEST' | 'REF' | 'ABN' | 'PROD';

registerLocaleData(localeDECH);
registerLocaleData(localeFRCH);
registerLocaleData(localeITCH);

@NgModule({
	declarations: [
		AppComponent,
		StartPageComponent,
		ProcessPageComponent,
		TaskTemplateViewComponent,
		TaskActivityViewComponent,
		ProcessRelationsComponent,
		ForbiddenPageComponent,
		TaskDetailViewComponent,
		TaskDetailsNodataRowComponent
	],
	bootstrap: [AppComponent],
	imports: [
		CommonModule,
		TranslateModule,
		NgIf,
		JsonPipe,
		NgForOf,
		DatePipe,
		MatListItem,
		MatListItemLine,
		MatHeaderCell,
		MatIcon,
		BrowserModule,
		AppRoutingModule,
		ObMasterLayoutModule,
		BrowserAnimationsModule,
		ObButtonModule,
		MatTooltipModule,
		QdAuthModule.forRoot(appSetup, authConfig),
		TranslateModule.forRoot(),
		MatButtonModule,
		MatCardModule,
		MatIconModule,
		ObExternalLinkModule,
		MatFormFieldModule,
		ReactiveFormsModule,
		MatInputModule,
		ObErrorMessagesModule,
		MatTableModule,
		MatSortModule,
		MatPaginatorModule,
		MatExpansionModule,
		MatSlideToggleModule,
		MatListModule,
		MatRadioModule,
		FormsModule,
		MatHeaderCellDef,
		MatColumnDef
	],
	schemas: [CUSTOM_ELEMENTS_SCHEMA],
	providers: [
		{provide: HTTP_INTERCEPTORS, useClass: ObHttpApiInterceptor, multi: true},
		{
			provide: OB_PAMS_CONFIGURATION,
			useFactory: (appModule: AppModule) => appModule.getObPamsEnvironment(),
			deps: [AppModule]
		},
		provideHttpClient(withInterceptorsFromDi()),
		provideObliqueConfiguration({
			accessibilityStatement: {
				createdOn: new Date('2025-10-23'),
				conformity: 'none',
				applicationName: "Process Context Service",
				applicationOperator: '-',
				contact: [
					{email: 'nobody@bit.admin.ch'}
				]
			}
		})
	]
})
export class AppModule {
	masterLayoutConfig: ObMasterLayoutConfig;
	obEPamsEnvironment: ObEPamsEnvironment | null;

	constructor(
		masterLayoutConfig: ObMasterLayoutConfig,
		readonly qdConfigService: QdConfigService,
	) {
		this.masterLayoutConfig = masterLayoutConfig;
		this.configureServiceNavigation();
	}

	/**
	 * Retrieves the current PAMS environment for the ServiceNavigation in Oblique.
	 * Possible values: "-d", "-r", "-t", "-a", ""
	 *
	 */
	getObPamsEnvironment() {
		return {environment: this.obEPamsEnvironment};
	}

	private configureServiceNavigation(): void {
		this.masterLayoutConfig.homePageRoute = '/startpage';
		this.masterLayoutConfig.locale.locales = ['de', 'fr', 'it'];
		this.masterLayoutConfig.locale.defaultLanguage = 'de';
		this.masterLayoutConfig.header.serviceNavigation.pamsAppId = 'notUsed';
		this.masterLayoutConfig.header.serviceNavigation.displayInfo = false;
		this.masterLayoutConfig.header.serviceNavigation.displayLanguages = true;
		this.masterLayoutConfig.header.serviceNavigation.displayMessage = true;
		this.masterLayoutConfig.header.serviceNavigation.displayApplications = true;
		this.masterLayoutConfig.header.serviceNavigation.displayProfile = true;
		this.masterLayoutConfig.header.serviceNavigation.displayAuthentication = true;
		this.masterLayoutConfig.header.serviceNavigation.handleLogout = true;

		this.qdConfigService.config$.subscribe(qdConfig => {
			if (qdConfig) {
				authConfig.clientId = qdConfig.clientId;
				authConfig.systemName = qdConfig.systemName;
				this.obEPamsEnvironment = this.mapEnvironmentEnum(qdConfig.pamsEnvironment);
			}
		});
	}

	/**
	 * Maps a given QdShellHeaderWidgetEnvironment string to the corresponding ObEPamsEnvironment enumeration value.
	 */
	private mapEnvironmentEnum(qdEnv: '' | QdShellHeaderWidgetEnvironment): ObEPamsEnvironment | null {
		switch (qdEnv) {
			case 'DEV':
				return ObEPamsEnvironment.DEV;
			case 'REF':
				return ObEPamsEnvironment.REF;
			case 'ABN':
				return ObEPamsEnvironment.ABN;
			case 'TEST':
				return ObEPamsEnvironment.TEST;
			case 'PROD':
				return ObEPamsEnvironment.PROD;
			default:
				console.warn(`Unrecognized pamsEnvironment: ${qdEnv}. ServiceNavigation will not work.`);
				return null;
		}
	}
}
