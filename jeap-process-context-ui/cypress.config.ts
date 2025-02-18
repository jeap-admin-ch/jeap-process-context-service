import { defineConfig } from 'cypress';

export default defineConfig({
	hosts: {
		localhost: '127.0.0.1'
	},
	component: {
		devServer: {
			framework: 'angular',
			bundler: 'webpack',
			options: {
				projectConfig: {
					root: './',
					sourceRoot: 'src',
					buildOptions: {
						outputPath: 'target/classes/static'
					}
				}
			}
		},
		specPattern: '**/*.cy.ts'
	}
});
