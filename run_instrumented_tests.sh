
. updated_modules.sh
updatedModules

echo "Running instrumented unit tests: "
for mod in $updated_modules
do
  echo "./gradlew $mod:connectedCheck"
  if ./gradlew $mod":connectedCheck"; then
    echo "$mod Instrumented Unit Test Succeeded" >&2
  else
    echo "$mod Instrumented Unit Test failed" >&2
    exit 1
  fi
done