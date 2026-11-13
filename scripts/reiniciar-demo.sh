#!/usr/bin/env bash
set -euo pipefail
# Uso desde la raíz del backend: bash scripts/reiniciar-demo.sh [contenedor] [base] [usuario_postgres]
# Devuelve las siete consultas demo (UUID fijos de110009-…) a su estado inicial con fechas de hoy:
# borra órdenes, resultados, pasos y procedimientos agregados a ESAS consultas desde la app.
# No toca ningún otro dato. Requiere haber ejecutado antes scripts/preparar-bd-desarrollo.sh.
if (( $# > 3 )); then
    printf 'Uso: %s [contenedor] [base] [usuario_postgres]\n' "$0" >&2
    exit 1
fi
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
contenedor=${1:-galenasv}
base_datos=${2:-galenasv}
usuario_postgres=${3:-postgres}
printf 'Se reiniciarán las consultas demo de la base %s (contenedor %s). ¿Continuar? [s/N] ' "$base_datos" "$contenedor"
read -r respuesta
if [[ "$respuesta" != "s" && "$respuesta" != "S" ]]; then
    printf 'Cancelado. No se modificó la base.\n'
    exit 0
fi
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -v reiniciar=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261112_datos_desarrollo_clinicos.sql"
