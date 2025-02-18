import {QdAppSetup, QdAuthConfigServerSide, QdLogLevel} from '@quadrel-services/qd-auth';

export const environment = {
	banner: { text: ''},
	BACKEND_SERVICE_API: '/process-context/api'
};

export const appSetup: QdAppSetup = {
	production: true,
	serviceEndpoint: '/process-context/'
};

export const authConfig: QdAuthConfigServerSide = {
	configPathSegment: 'api/configuration',
	logLevel: QdLogLevel.Error,
	renewUserInfoAfterTokenRenew: true,
	silentRenew: true,
	silentRenewUrl: `${window.location.origin}/process-context/assets/auth/silent-renew.html`,
	useAutoLogin: true
};
