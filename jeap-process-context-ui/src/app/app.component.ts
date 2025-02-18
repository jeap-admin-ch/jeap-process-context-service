import {Component, inject} from '@angular/core';
import {VersionDetectorService} from './shared/versiondetectorservice/version-detector.service';
import {TranslateService} from '@ngx-translate/core';
import {QdAuthenticationService} from '@quadrel-services/qd-auth';
import {ObMasterLayoutService} from '@oblique/oblique';

@Component({
	selector: 'app-root',
	templateUrl: './app.component.html',
	styleUrls: ['./app.component.css']
})
export class AppComponent {
	private readonly translateService = inject(TranslateService);
	private readonly masterLayoutService = inject(ObMasterLayoutService);

	constructor(
		private readonly authenticationService: QdAuthenticationService,
		private readonly versionDetectorService: VersionDetectorService
	) {
		this.translateService.onLangChange.subscribe(({lang}) => this.translateService.use(lang));
		this.masterLayoutService.header.loginState$.subscribe($event => this.loginStatus($event));
	}

	loginStatus($event: any) {
		this.authenticationService.pamsStatus.next($event);
	}

	getVersion() {
		return this.versionDetectorService.getVersion();
	}
}
