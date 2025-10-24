import {Component, inject} from '@angular/core';
import {VersionDetectorService} from './shared/versiondetectorservice/version-detector.service';
import {QdAuthenticationService} from '@quadrel-services/qd-auth';
import {ObMasterLayoutService} from '@oblique/oblique';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    standalone: false
})
export class AppComponent {

	private readonly masterLayoutService = inject(ObMasterLayoutService);

	constructor(
		private readonly authenticationService: QdAuthenticationService,
		private readonly versionDetectorService: VersionDetectorService
	) {
		this.masterLayoutService.header.loginState$.subscribe($event => this.loginStatus($event));
	}

	loginStatus($event: any) {
		this.authenticationService.pamsStatus.next($event);
	}

	getVersion() {
		return this.versionDetectorService.getVersion();
	}
}
