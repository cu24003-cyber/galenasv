#!/bin/bash
set -e

PKG="sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv"
BASE_DIR="src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/control"
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
  FILE="$BASE_DIR/${ENT}DAO.java"
  cat > "$FILE" << EOF
package ${PKG}.control;

import jakarta.ejb.Stateless;
import ${PKG}.entities.${ENT};
import java.util.UUID;

@Stateless
public class ${ENT}DAO extends DefaultDAO<${ENT}, UUID> {

    public ${ENT}DAO() {
        super(${ENT}.class);
    }
}
EOF
  echo "Creado: $FILE"
done

echo "Listo: 19 DAO creados."