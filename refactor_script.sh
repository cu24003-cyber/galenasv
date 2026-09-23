#!/bin/bash
set -euo pipefail

# ── CONFIGURA ANTES DE CORRER ────────────────────────────────────────
INCLUDE_PERSONA=true   # true: PersonaDAO/PersonaService se refactorizan igual que las demás
                        # false: se dejan intactas (OJO: PersonaService.java ya tiene
                        #        getRepository() de una corrida anterior, revísalo a mano si pones false)
# ──────────────────────────────────────────────────────────────────────

BASE=/home/raul/Documents/proyectos/galenasv/src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv
PKG=sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv

cd /home/raul/Documents/proyectos/galenasv

# Verificación de seguridad: exige rama limpia y no ser master
BRANCH=$(git branch --show-current)
if [ "$BRANCH" = "master" ]; then
  echo "ABORTA: estás en master. Cambia a tu rama de revisión antes de correr esto."
  exit 1
fi
if [ -n "$(git status --porcelain)" ]; then
  echo "ABORTA: hay cambios sin commitear. Haz commit o stash antes de correr esto."
  echo "(Si el cambio de getDao->getRepository que ya corriste está sin commit, es esperado; revísalo con git diff antes de decidir.)"
  exit 1
fi

echo "== Rama: $BRANCH — OK, continuando =="

# 1. Mover la carpeta control/ -> repository/
if [ -d "$BASE/control" ]; then
  git mv "$BASE/control" "$BASE/repository"
else
  echo "AVISO: $BASE/control ya no existe. ¿Ya se movió antes? Revisa manualmente."
fi
REPO="$BASE/repository"

# 2. Actualizar el package declaration en todos los archivos movidos
grep -rl "package ${PKG}.control;" "$REPO" | xargs -r sed -i "s/package ${PKG}.control;/package ${PKG}.repository;/"

# 3. Renombrar DefaultDAO -> AbstractRepository
if [ -f "$REPO/DefaultDAO.java" ]; then
  git mv "$REPO/DefaultDAO.java" "$REPO/AbstractRepository.java"
  sed -i \
      -e 's/\bDefaultDAO\b/AbstractRepository/g' \
      -e 's/\bDefaultDAOInterface\b/RepositoryInterface/g' \
      "$REPO/AbstractRepository.java"
fi

# 4. Renombrar DefaultDAOInterface -> RepositoryInterface
if [ -f "$REPO/DefaultDAOInterface.java" ]; then
  git mv "$REPO/DefaultDAOInterface.java" "$REPO/RepositoryInterface.java"
  sed -i 's/\bDefaultDAOInterface\b/RepositoryInterface/g' "$REPO/RepositoryInterface.java"
fi

# 5. Lista de las 19 entities que siempre se refactorizan
ENTITIES=(
  Clinica Consulta ConsultaProcedimiento ConsultaProcedimientoPaso
  Documento Examen ExamenResultado ExamenTipoExamen MedioContacto
  OrdenExamen PersonaRol Procedimiento ProcedimientoPaso
  ProcedimientoPasoExamen ProcedimientoPasoSecuencia Rol
  TipoDocumento TipoExamen TipoMedioContacto
)

if [ "$INCLUDE_PERSONA" = true ]; then
  ENTITIES+=(Persona)
fi

echo "== Entities a procesar: ${#ENTITIES[@]} =="

for E in "${ENTITIES[@]}"; do
  DAO_FILE="$REPO/${E}DAO.java"
  REPO_FILE="$REPO/${E}Repository.java"

  if [ ! -f "$DAO_FILE" ]; then
    echo "AVISO: no existe $DAO_FILE, se omite."
    continue
  fi

  git mv "$DAO_FILE" "$REPO_FILE"

  sed -i \
      -e "s/\b${E}DAO\b/${E}Repository/g" \
      -e 's/\bDefaultDAO\b/AbstractRepository/g' \
      "$REPO_FILE"

  LOWER_E="${E,}"

  SERVICE_FILE=$(find /home/raul/Documents/proyectos/galenasv/src/main/java -name "${E}Service.java" | head -n1)

  if [ -z "$SERVICE_FILE" ]; then
    echo "AVISO: no se encontró ${E}Service.java, revísalo a mano."
    continue
  fi

  sed -i \
      -e "s/import ${PKG//./\\.}\.control\.DefaultDAOInterface;/import ${PKG//./\\.}.repository.RepositoryInterface;/" \
      -e "s/import ${PKG//./\\.}\.control\.${E}DAO;/import ${PKG//./\\.}.repository.${E}Repository;/" \
      -e "s/\bDefaultDAOInterface\b/RepositoryInterface/g" \
      -e "s/\b${E}DAO\b/${E}Repository/g" \
      -e "s/\b${LOWER_E}DAO\b/${LOWER_E}Repository/g" \
      "$SERVICE_FILE"

  echo "OK: $E -> Repository (archivo + service actualizado)"
done

# 6. Renombrar getDao() -> getRepository() en toda la capa de Service
SERVICE_DIR="/home/raul/Documents/proyectos/galenasv/src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/service"

echo "== Renombrando getDao() -> getRepository() en $SERVICE_DIR =="

if [ -f "$SERVICE_DIR/AbstractService.java" ]; then
  if grep -q '\bgetDao\b' "$SERVICE_DIR/AbstractService.java"; then
    sed -i 's/\bgetDao\b/getRepository/g' "$SERVICE_DIR/AbstractService.java"
    echo "OK: AbstractService.java actualizado"
  else
    echo "INFO: AbstractService.java ya no tenía getDao() (¿corrida previa?)."
  fi
else
  echo "AVISO: no se encontró AbstractService.java en $SERVICE_DIR, revísalo a mano."
fi

find "$SERVICE_DIR" -name "*Service.java" ! -name "AbstractService.java" | while read -r SF; do
  if grep -q '\bgetDao\b' "$SF"; then
    sed -i 's/\bgetDao\b/getRepository/g' "$SF"
    echo "OK: $(basename "$SF") actualizado (getDao->getRepository)"
  fi
done

echo ""
echo "== Verificación: buscando referencias viejas que hayan quedado sueltas =="
LEFTOVER=$(grep -rl "control\.\|DefaultDAO\b\|DefaultDAOInterface\b" \
    /home/raul/Documents/proyectos/galenasv/src/main/java --include="*.java" || true)

if [ -n "$LEFTOVER" ]; then
  echo "ATENCIÓN: estos archivos todavía mencionan 'control' o 'DefaultDAO'. Revísalos a mano:"
  echo "$LEFTOVER"
else
  echo "Sin referencias viejas a 'control'/'DefaultDAO' encontradas."
fi

echo ""
echo "== Verificación: ¿queda algún getDao() suelto? =="
LEFTOVER_GETDAO=$(grep -rl '\bgetDao\b' /home/raul/Documents/proyectos/galenasv/src/main/java --include="*.java" || true)
if [ -n "$LEFTOVER_GETDAO" ]; then
  echo "ATENCIÓN: estos archivos todavía usan getDao(). Revísalos a mano:"
  echo "$LEFTOVER_GETDAO"
else
  echo "Sin referencias a getDao() encontradas."
fi

echo ""
echo "== Listo. Ahora corre:  mvn clean compile  =="
