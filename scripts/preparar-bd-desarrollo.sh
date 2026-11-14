#!/usr/bin/env bash
set -euo pipefail
# Uso desde la raíz del backend: bash scripts/preparar-bd-desarrollo.sh [contenedor] [base] [usuario_postgres]
# Requiere Docker con PostgreSQL 13+, Java 21 y haber ejecutado mvn -DskipTests package.
if (( $# > 3 )); then
    printf 'Uso: %s [contenedor] [base] [usuario_postgres]\n' "$0" >&2
    exit 1
fi
raiz=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
contenedor=${1:-galenasv}
base_datos=${2:-galenasv}
usuario_postgres=${3:-postgres}
if [[ ! -f "$raiz/target/classes/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/control/service/ContrasenaService.class" || ! -f "$raiz/target/galenosv/WEB-INF/lib/password4j-1.8.4.jar" ]]; then
    printf 'Primero ejecuta: mvn -DskipTests package\n' >&2
    exit 1
fi
# HU2: el login es por documento. password_hash sigue siendo obligatorio en el esquema,
# así que se llena con el hash de una clave aleatoria que nadie conoce ni necesita.
hash=$(head -c 48 /dev/urandom | base64 -w0 | java -cp "$raiz/target/classes:$raiz/target/galenosv/WEB-INF/lib/*" \
    sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContrasenaService --stdin)
# CREATE DATABASE queda fuera de la transacción de la migración.
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d postgres -v "base=$base_datos" <<'SQL'
SELECT format('CREATE DATABASE %I', :'base')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname=:'base') \gexec
SQL
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261111_base_completa.sql"
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261112_documento_tipo_valor_unico.sql"
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261113_orden_examen_examen.sql"
# Datos demo del guion del Primer Parcial (docs/datos-demo-rubrica.md). Si la base tiene la demo
# anterior (admin.demo, doctor.demo), antes aplique 20261113_limpieza_demo_anterior.sql (docs/LEVANTAR.md).
{
    printf '\\set password_hash %s\n' "$hash"
    cat "$raiz/docs/migrations/20261113_datos_demo_rubrica.sql"
} | docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos"
unset hash
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261113_datos_demo_rubrica_consultas.sql"
printf 'Base preparada: %s. Entra con tipo DUI y 01000001-1 (Carlos, Doctor), 01000002-2 (Lucía, Enfermera) o 01000003-3 (Marta, Atención al público). Configura el mismo nombre y credenciales de conexión en Liberty y reinicia el backend.\n' "$base_datos"
