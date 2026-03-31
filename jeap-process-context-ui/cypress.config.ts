import {defineConfig} from 'cypress';
import {devServer} from '@cypress/vite-dev-server';
import angular from '@analogjs/vite-plugin-angular';
import { resolve } from 'path';

export default defineConfig({
	hosts: {
		localhost: '127.0.0.1'
	},
	component: {
		devServer: function (devServerConfig: Parameters<typeof devServer>[0]) {
			return devServer({
				...devServerConfig,
				viteConfig: {
					plugins: [angular({tsconfig: './tsconfig.json'})],
					optimizeDeps: {
						include: [
							'@angular/common',
							'@angular/material/button',
							'@angular/material/card',
							'@angular/material/expansion',
							'@angular/material/form-field',
							'@angular/material/icon',
							'@angular/material/input',
							'@angular/material/list',
							'@angular/material/paginator',
							'@angular/material/radio',
							'@angular/material/slide-toggle',
							'@angular/material/sort',
							'@angular/material/table',
							'@angular/material/tooltip'
						]
					},
					resolve: {
						mainFields: ['es2020', 'es2015', 'browser', 'module', 'main'],
						alias: {
							src: resolve(__dirname, 'src')
						}
					}
				}
			});
		},
		specPattern: '**/*.cy.ts'
	}
});
