#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Formato docker --env-file: valores literales, sin source/eval ni expansión de secretos.
if [[ -f .env ]]; then
    while IFS= read -r linea || [[ -n "$linea" ]]; do
        linea=${linea%$'\r'}
        [[ "$linea" =~ ^[[:space:]]*(#|$) ]] && continue
        if [[ ! "$linea" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            echo 'Formato inválido en .env: use NOMBRE=valor, sin export.' >&2
            exit 1
        fi
        nombre=${BASH_REMATCH[1]}
        valor=${BASH_REMATCH[2]}
        # Las variables del proceso tienen prioridad sobre el archivo local.
        if [[ ! -v "$nombre" ]]; then
            export "$nombre=$valor"
        fi
    done < .env
fi

for nombre in PG_USER PG_PASS PG_DB PG_HOST PG_PORT LTPA_KEYS_PASSWORD; do
    if [[ -z "${!nombre:-}" ]]; then
        printf 'Falta %s: configure .env o el entorno del proceso.\n' "$nombre" >&2
        exit 1
    fi
done

if [[ $# -eq 0 ]]; then
    set -- liberty:run
fi
exec mvn "$@"
