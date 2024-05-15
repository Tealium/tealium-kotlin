#!/bin/bash

. updated_modules.sh
updatedModules

echo "Running unit tests: "
for mod in $updated_modules
do
  echo "./gradlew $mod:testDebugUnitTest"
  if ./gradlew $mod":testDebugUnitTest" --stacktrace; then
    echo "$mod Unit Test Succeeded" >&2
  else
    echo "$mod Unit Test failed" >&2
    exit 1
  fi
done