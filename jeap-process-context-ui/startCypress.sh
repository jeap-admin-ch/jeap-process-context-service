#!/bin/bash

#This script is called from exec-maven plugin because cypress needs the xvfb running.
set -x
/usr/local/bin/start_xvfb.sh &
npm run cypress:run
test_result=$?
pkill -f Xvfb
exit $test_result








