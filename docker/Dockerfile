# Imagen backend GalenoSV - Tercer Parcial PPI115 2026 GT02
# Base exigida por la rúbrica: debian:stable.
# La imagen NO contiene el WAR: el profesor lo monta en
#   /home/usuario/wlp/usr/servers/galeno/dropins/galeno.war
# Contiene Java, Open Liberty (Jakarta EE 11) y las dependencias "provided" del pom.xml.

FROM debian:stable

# latest = la última versión publicada de Open Liberty; se puede fijar con --build-arg OL_VERSION=x.y.z.w
ARG OL_VERSION=latest
# Mismas versiones que el pom.xml (scope provided).
ARG PG_JDBC_VERSION=42.7.5
ARG OPENPDF_VERSION=2.4.0
ARG PRIMEFACES_VERSION=16.0.0

ARG MVN=https://repo1.maven.org/maven2

RUN apt-get update \
 && apt-get install -y --no-install-recommends openjdk-21-jre-headless curl unzip ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && ln -s "$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")" /usr/lib/jvm/java-21 \
 && useradd -m -s /bin/bash usuario

# Sin JAVA_HOME, bin/server lo deduce del PATH y lo descarta porque el JRE headless no trae include/;
# entonces no carga lib/platform/java/java9.options (--add-opens) y Weld falla con
# "module java.base does not opens java.lang" (WELD-001524). El enlace evita fijar la arquitectura.
ENV JAVA_HOME=/usr/lib/jvm/java-21

USER usuario
WORKDIR /home/usuario

# Open Liberty (kernel) + feature jakartaee-11.0, en /home/usuario/wlp, servidor "galeno".
RUN set -eu; \
    if [ "$OL_VERSION" = "latest" ]; then \
      OL_VERSION=$(curl -fsSL "$MVN/io/openliberty/openliberty-kernel/maven-metadata.xml" \
                   | sed -n 's:.*<release>\(.*\)</release>.*:\1:p'); \
    fi; \
    echo "Open Liberty $OL_VERSION"; \
    curl -fsSL -o ol.zip "$MVN/io/openliberty/openliberty-kernel/$OL_VERSION/openliberty-kernel-$OL_VERSION.zip"; \
    unzip -q ol.zip; rm ol.zip; \
    wlp/bin/featureUtility installFeature jakartaee-11.0 --acceptLicense; \
    wlp/bin/server create galeno; \
    mkdir -p wlp/usr/servers/galeno/dropins \
             wlp/usr/shared/resources/postgresql \
             wlp/usr/shared/config/lib/global

# Dependencias "provided":
#  - Driver JDBC: lo usa el dataSource (library PostgreSQL-Library).
#  - OpenPDF, PrimeFaces y sus temas: "global library" de Liberty, visible para las apps en dropins.
RUN set -eu; \
    curl -fsSL -o "wlp/usr/shared/resources/postgresql/postgresql-$PG_JDBC_VERSION.jar" \
         "$MVN/org/postgresql/postgresql/$PG_JDBC_VERSION/postgresql-$PG_JDBC_VERSION.jar"; \
    G=wlp/usr/shared/config/lib/global; \
    curl -fsSL -o "$G/openpdf-$OPENPDF_VERSION.jar" \
         "$MVN/com/github/librepdf/openpdf/$OPENPDF_VERSION/openpdf-$OPENPDF_VERSION.jar"; \
    curl -fsSL -o "$G/primefaces-$PRIMEFACES_VERSION.jar" \
         "$MVN/org/primefaces/primefaces/$PRIMEFACES_VERSION/primefaces-$PRIMEFACES_VERSION.jar"; \
    curl -fsSL -o "$G/primefaces-themes-$PRIMEFACES_VERSION.jar" \
         "$MVN/org/primefaces/primefaces-themes/$PRIMEFACES_VERSION/primefaces-themes-$PRIMEFACES_VERSION.jar"

COPY --chown=usuario:usuario server.xml wlp/usr/servers/galeno/server.xml

EXPOSE 9080 9443
CMD ["/home/usuario/wlp/bin/server", "run", "galeno"]
