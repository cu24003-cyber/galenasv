#!/bin/bash
set -e

PKG="sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv"
BASE_DIR="src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/entity/repository"
mkdir -p "$BASE_DIR"

ENTITIES=(
  Clinica
  Consulta
  ConsultaProcedimiento
  ConsultaProcedimientoPaso
  Documento
  Examen
  ExamenResultado
  ExamenTipoExamen
  MedioContacto
  OrdenExamen
  PersonaRol
  Procedimiento
  ProcedimientoPaso
  ProcedimientoPasoExamen
  ProcedimientoPasoSecuencia
  Rol
  TipoDocumento
  TipoExamen
  TipoMedioContacto
)

for ENT in "${ENTITIES[@]}"; do
  FILE="$BASE_DIR/${ENT}Repository.java"
  if [ -e "$FILE" ]; then echo "Ya existe: $FILE; se conserva."; continue; fi
  cat > "$FILE" << EOF
package ${PKG}.entity.repository;

import jakarta.ejb.Stateless;
import ${PKG}.entity.${ENT};
import java.util.UUID;

@Stateless
public class ${ENT}Repository extends AbstractRepository<${ENT}, UUID> {

    public ${ENT}Repository() {
        super(${ENT}.class);
    }
}
EOF
  echo "Creado: $FILE"
done

echo "Listo: 19 Repository creados."