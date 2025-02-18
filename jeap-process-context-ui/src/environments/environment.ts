import {QdAuthConfigServerSide, QdLogLevel} from "@quadrel-services/qd-auth";

export const environment = {
	banner: { text: 'LOCAL'},
	BACKEND_SERVICE_API: 'http://localhost:8080/process-context/api'
};

export const appSetup = {
	production: false,
	serviceEndpoint: 'http://localhost:8080/process-context/'
};

export const authConfig: QdAuthConfigServerSide = {
	configPathSegment: 'api/configuration',
	logLevel: QdLogLevel.Debug,
	renewUserInfoAfterTokenRenew: true,
	silentRenew: true,
	silentRenewUrl: `${window.location.origin}/assets/auth/silent-renew.html`,
	useAutoLogin: true
};
