import {Component, inject} from '@angular/core';
import {VersionDetectorService} from './shared/versiondetectorservice/version-detector.service';
import {ObMasterLayoutService} from '@oblique/oblique';
import {QdAuthenticationService} from '@quadrel-enterprise-ui/auth';

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
		// Oblique reports an undefined login state when the ePortal backend cannot be reached. Forwarding it
		// would make the authentication service fail while reading the PAMS session status.
		if ($event !== undefined && $event !== null) {
			this.authenticationService.pamsStatus.next($event);
		}
	}

	getVersion() {
		return this.versionDetectorService.getVersion();
	}
}
