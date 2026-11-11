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
printf 'Se crearán tres cuentas de prueba; las existentes conservan su contraseña.\n'
read -r -s -p 'Contraseña para las cuentas nuevas (mínimo 12 caracteres): ' clave
printf '\n'
read -r -s -p 'Repite la contraseña: ' confirmacion
printf '\n'
if [[ "$clave" != "$confirmacion" ]]; then
    printf 'Las contraseñas no coinciden. No se modificó la base.\n' >&2
    exit 1
fi
hash=$(printf '%s\n' "$clave" | java -cp "$raiz/target/classes:$raiz/target/galenosv/WEB-INF/lib/*" \
    sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ContrasenaService --stdin)
unset clave confirmacion
# CREATE DATABASE queda fuera de la transacción de la migración.
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d postgres -v "base=$base_datos" <<'SQL'
SELECT format('CREATE DATABASE %I', :'base')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname=:'base') \gexec
SQL
docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos" \
    < "$raiz/docs/migrations/20261111_base_completa.sql"
{
    printf '\\set password_hash %s\n' "$hash"
    cat "$raiz/docs/migrations/20261111_datos_desarrollo.sql"
} | docker exec -i "$contenedor" psql -X -v ON_ERROR_STOP=1 -U "$usuario_postgres" -d "$base_datos"
unset hash
printf 'Base preparada: %s. Configura el mismo nombre y credenciales de conexión en Liberty y reinicia el backend.\n' "$base_datos"
