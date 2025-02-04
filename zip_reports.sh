#!/bin/bash

. updated_modules.sh
updatedModules

echo "Packaging test reports: "
reportsDir="reports"
mkdir "$reportsDir"

for mod in $updated_modules
do
  if [ -d "$mod/build/reports/" ]; then
    zip -r "$reportsDir/${mod}.zip" "$mod/build/reports/"
  fi
done