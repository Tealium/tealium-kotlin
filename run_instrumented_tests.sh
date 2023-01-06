#!/bin/bash

. updated_modules.sh
updatedModules

echo "Running instrumented unit tests: "
for mod in $updated_modules
do
  echo "./gradlew $mod:connectedDebugAndroidTest"
  if ./gradlew $mod":connectedDebugAndroidTest --stacktrace"; then
    echo "$mod Instrumented Unit Test Succeeded" >&2
  else
    echo "$mod Instrumented Unit Test failed" >&2
    exit 1
  fi
done