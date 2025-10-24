import {MockProvider} from 'ng-mocks';
import {TranslateService} from '@ngx-translate/core';
import {of, Subject} from 'rxjs';


export function getTranslationServiceMockProvider(currentLang: string) {
	return MockProvider(TranslateService, {
		get currentLang(): string {
			return currentLang;
		},
		get(key: any): any {
			return of(`[${currentLang}] ${key}`);
		},
		onLangChange: new Subject<any>(),
		onTranslationChange: new Subject<any>(),
		onDefaultLangChange: new Subject<any>()
	});
}
