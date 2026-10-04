#!/bin/bash
set -euo pipefail
# La migración BCE ya está aplicada. Este script comprueba la estructura actual.
base="src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv"
for layer in boundary/model control/service entity/repository; do
  test -d "$base/$layer"
done
if rg -n 'galenosv\.(model|service|entities|repository|converter|validation|web)\.' src/main; then
  echo "Quedan referencias a los paquetes anteriores."
  exit 1
fi
echo "Estructura BCE y referencias verificadas."
