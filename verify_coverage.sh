#!/bin/bash

. updated_modules.sh
updatedModules

echo "Checking test coverage: "
reportsDir="reports"
mkdir "$reportsDir"

wereFailures=0

for mod in $updated_modules
do
  echo "./gradlew $mod:jacocoTestReport"
  ./gradlew $mod:jacocoTestReport

  failed=""

  echo "./gradlew $mod:verifyTestCoverage"
  if ./gradlew $mod":verifyTestCoverage"; then
    echo "$mod Test Coverage Succeeded" >&2
  else
    echo "$mod Test Coverage failed" >&2
    failed="coverage-failed-"
    # Uncomment to fail the workflow step.
    #wereFailures = 1
  fi

  echo "./gradlew $mod:detekt"
  ./gradlew $mod":detekt"

  zip -r "$reportsDir/${failed}${mod}.zip" "$mod/build/reports/"
done

exit $wereFailures