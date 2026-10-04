#!/bin/bash
set -e

PKG="sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv"
BASE_DIR="src/main/java/sv/edu/ues/ingenieria/ppi115_2026/salud/galenosv/control/service"
mkdir -p "$BASE_DIR"

declare -A IDFIELD=(
  [Clinica]=idClinica
  [Consulta]=idConsulta
  [ConsultaProcedimiento]=idConsultaProcedimiento
  [ConsultaProcedimientoPaso]=idConsultaProcedimientoPaso
  [Documento]=idDocumento
  [Examen]=idExamen
  [ExamenResultado]=idExamenResultado
  [ExamenTipoExamen]=idExamenTipoExamen
  [MedioContacto]=idMedioContacto
  [OrdenExamen]=idOrdenExamen
  [Persona]=idPersona
  [PersonaRol]=idPersonaRol
  [Procedimiento]=idProcedimiento
  [ProcedimientoPaso]=idProcedimientoPaso
  [ProcedimientoPasoExamen]=idProcedimientoPasoExamen
  [ProcedimientoPasoSecuencia]=idProcedimientoPasoSecuencia
  [Rol]=idRol
  [TipoDocumento]=idTipoDocumento
  [TipoExamen]=idTipoExamen
  [TipoMedioContacto]=idTipoMedioContacto
)

for ENT in "${!IDFIELD[@]}"; do
  ID_FIELD="${IDFIELD[$ENT]}"
  SETTER="set${ID_FIELD^}"
  GETTER="get${ID_FIELD^}"
  FILE="$BASE_DIR/${ENT}Service.java"

  EXTRA_VALIDATION=""
  EXTRA_IMPORT=""
  if [ "$ENT" == "Clinica" ]; then
    EXTRA_VALIDATION="
    @Override
    protected void validar(Clinica entidad) {
        super.validar(entidad);
        if (entidad.getNombre() == null || entidad.getNombre().isBlank()) {
            throw new IllegalArgumentException(\"El nombre de la clinica es obligatorio.\");
        }
    }"
  fi

  EXTRA_CREAR_BODY=""
  if [ "$ENT" == "Persona" ]; then
    EXTRA_IMPORT="import java.util.Date;"
    EXTRA_CREAR_BODY="
        if (entidad.getFechaCreacion() == null) {
            entidad.setFechaCreacion(new Date());
        }"
  fi

  if [ -e "$FILE" ]; then echo "Ya existe: $FILE; se conserva."; continue; fi
  cat > "$FILE" << EOF
package ${PKG}.control.service;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ${PKG}.entity.repository.RepositoryInterface;
import ${PKG}.entity.repository.${ENT}Repository;
import ${PKG}.entity.${ENT};
import java.util.UUID;
${EXTRA_IMPORT}

@Stateless
public class ${ENT}Service extends AbstractService<${ENT}, UUID> {

    @Inject
    private ${ENT}Repository ${ENT,}Repository;

    @Override
    protected RepositoryInterface<${ENT}, UUID> getRepository() {
        return ${ENT,}Repository;
    }

    @Override
    protected UUID obtenerId(${ENT} entidad) {
        return entidad.${GETTER}();
    }

    @Override
    public void crear(${ENT} entidad) {
        if (entidad.${GETTER}() == null) {
            entidad.${SETTER}(UUID.randomUUID());
        }${EXTRA_CREAR_BODY}
        super.crear(entidad);
    }
${EXTRA_VALIDATION}
}
EOF
  echo "Creado: $FILE"
done

echo "Listo: 20 Service creados."