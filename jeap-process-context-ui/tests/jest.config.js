'use strict';

module.exports = {
	preset: 'jest-preset-angular',
	setupFilesAfterEnv: ['<rootDir>/tests/setupJest.ts'],
	coverageDirectory: '<rootDir>/coverage/sonarQube',
	testResultsProcessor: 'jest-sonar-reporter',
	collectCoverage: true,
	forceCoverageMatch: [
		'**/src/app/**/*.ts',
		'**/src/app/**/*.html'
	],
	testPathIgnorePatterns: ['/node_modules/', 'test.ts'],
	transformIgnorePatterns: ['node_modules/(?!(.*\\.mjs$|@angular/common/locales/.*\\.js$|@quadrel-enterprise-ui/.*\\.js$))']
};
