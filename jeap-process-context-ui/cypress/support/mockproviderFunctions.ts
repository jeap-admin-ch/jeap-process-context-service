import {MockProvider} from 'ng-mocks';
import {TranslateService} from '@ngx-translate/core';
import {of} from 'rxjs';
import {EventEmitter} from '@angular/core';

export function getTranslationServiceMockProvider(currentLang: string) {
	return MockProvider(TranslateService, {
		get currentLang(): string {
			return currentLang;
		},
		get(key: any): any {
			return of(key);
		},
		onLangChange: new EventEmitter<any>(),
		onTranslationChange: new EventEmitter<any>(),
		onDefaultLangChange: new EventEmitter<any>()
	});
}
