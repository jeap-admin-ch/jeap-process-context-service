#!/bin/bash

# Runs the Cypress component tests so they work both locally and on CI.
set -x
cd "$(dirname "$0")" || exit 1

# Ensure the Cypress binary is present. Idempotent: a no-op when it is already
# cached, so it only downloads on a fresh machine (e.g. node_modules without the
# binary). Without this the run fails with "Cypress binary not installed".
node_modules/.bin/cypress install

# Cypress' bundled Electron runs headlessly without an X server. The jEAP CI build
# image provides an Xvfb helper - start it when present so behaviour matches CI;
# otherwise (developer machines, minimal images) run directly against headless Electron.
started_xvfb=0
if [ -x /usr/local/bin/start_xvfb.sh ]; then
	/usr/local/bin/start_xvfb.sh &
	started_xvfb=1
fi

npm run cypress:run
test_result=$?

if [ "$started_xvfb" -eq 1 ]; then
	pkill -f Xvfb
fi

exit $test_result
