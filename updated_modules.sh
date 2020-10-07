#!/bin/bash

# Get a list of all available gradle tasks
function updatedModules() {
  local AVAILABLE_TASKS
  AVAILABLE_TASKS=$(./gradlew tasks --all)
  updated_modules=""

  # Retrieve all the updated modules
  while read -r line; do
    module_name=${line%%/*} # Gets the first word before '/'
    # Now we check if we haven't already added this module
    if [[ ${updated_modules} != *"${module_name}"* ]]; then
      #
      if [[ $AVAILABLE_TASKS =~ $module_name":test" ]]; then
        updated_modules=${updated_modules}" "${module_name}
      fi
    fi
  done < <(git diff --name-only "origin/$GITHUB_BASE_REF")
  # GITHUB_BASE_REF should be the branch we're merging into.

  echo "Found changes to the following modules "

  for mod in $updated_modules
  do
    echo "$mod"
  done
}