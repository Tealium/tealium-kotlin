#!/bin/bash

. updated_modules.sh
updatedModules

echo "Checking test coverage: "
reportsDir="reports"
mkdir "$reportsDir"
for mod in $updated_modules
do
  echo "./gradlew $mod:jacocoTestReport $mod:verifyTestCoverage"
  if ./gradlew $mod:jacocoTestReport $mod":verifyTestCoverage"; then
    echo "$mod Test Coverage Succeeded" >&2
  else
    echo "$mod Test Coverage failed" >&2

    #TODO: get access to the coverage stats and report
    exit 1
  fi
  zip -r "$reportsDir/${mod}.zip" "$mod/build/reports/"
done