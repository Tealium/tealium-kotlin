
. updated_modules.sh
updatedModules

echo "Checking test coverage: "
for mod in $updated_modules
do
  echo "./gradlew $mod:verifyTestCoverage"
  if ./gradlew $mod":verifyTestCoverage"; then
    echo "$mod Test Coverage Succeeded" >&2
  else
    echo "$mod Test Coverage failed" >&2
    exit 1
  fi
done