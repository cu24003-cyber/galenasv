#!/bin/sh
# Arranque del contenedor backend (tercer parcial).
#
# El profesor monta el WAR (sin librerías, dependencias "provided") en dropins/galeno.war.
# PrimeFaces 16 registra sus componentes con @FacesComponent, y Liberty solo escanea anotaciones
# dentro de la aplicación: desde la librería global las pantallas JSF fallan con
# "Undefined component type org.primefaces.component.CommandButton".
# Por eso este script COPIA el WAR montado a apps/, le agrega los JAR de PrimeFaces en
# WEB-INF/lib y arranca Liberty con esa copia (server.xml: dropins desactivado, contexto /galeno).
# El archivo montado nunca se modifica.
set -eu

SERVIDOR=/home/usuario/wlp/usr/servers/galeno
WAR_PATH="${WAR_PATH:-$SERVIDOR/dropins/galeno.war}"
LIBS_APP=/home/usuario/libs-app
DESTINO="$SERVIDOR/apps/galeno.war"

if [ ! -f "$WAR_PATH" ]; then
    echo "ERROR: no se encontró el WAR en $WAR_PATH." >&2
    echo "Móntelo con -v \"\$PWD/target/galeno.war\":$SERVIDOR/dropins/galeno.war" >&2
    echo "o indique otra ruta con -e WAR_PATH=/ruta/al/galeno.war" >&2
    exit 1
fi

mkdir -p "$SERVIDOR/apps"
cp "$WAR_PATH" "$DESTINO.tmp"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/WEB-INF/lib"
cp "$LIBS_APP"/*.jar "$TMP/WEB-INF/lib/"
# -0: los JAR ya están comprimidos.
(cd "$TMP" && zip -q -0 -r "$DESTINO.tmp" WEB-INF/lib)
mv "$DESTINO.tmp" "$DESTINO"
rm -rf "$TMP"
trap - EXIT

echo "WAR preparado: $WAR_PATH -> $DESTINO (+ $(ls "$LIBS_APP" | tr '\n' ' '))"
exec /home/usuario/wlp/bin/server run galeno
