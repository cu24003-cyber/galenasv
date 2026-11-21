# Compatibilidad con la base original + parche: avance

Fecha: 2026-11-20.
Estado: retomado por autorización del usuario el 2026-11-21. Roles y órdenes adaptados; quedan los controles finales y empaquetado.

## Objetivo y restricciones

La aplicación debe funcionar en la máquina del ingeniero, cuya base corresponde al DDL original de Classroom más su parche. El flujo actual de la aplicación debe conservarse. No se ha confirmado el contenido de sus tablas ni un formato específico para guardar los exámenes seleccionados.

No cambiar la base del ingeniero, ejecutar migraciones adicionales ni cargar nuestros datos demo sobre ella. Probar en una base aislada. Mantener rutas, campos JSON, reglas de acceso y comportamiento visible del frontend siempre que sea posible.

## Trabajo ya terminado

- Configuración mediante `.env`, plantilla sin secretos, cargador de variables e instrucciones.
- Commit publicado en `origin/backend-rubrica`: `4524e28105671cc39924ab39f7f02ecccf61b27a`, «Configura Liberty mediante variables de entorno».
- El `.env` privado está excluido de Git. Se comparte `.env.example`.
- Verificación de ese cambio: 797 pruebas Maven y 6 del cargador aprobadas.
- Comparación detallada: [comparacion-base-tercer-parcial.md](comparacion-base-tercer-parcial.md).

Backend: `/home/junior/galenosv`. Frontend: `/home/junior/chivoMed/ChivoMed`.

## Hallazgos confirmados en archivos

Se contrastaron las columnas mapeadas de las 20 entidades con el DDL original y su parche. Solo faltan `rol.codigo` y `orden_examen.id_examen`.

El parche ya incorpora `consulta_procedimiento_paso.id_procedimiento_paso`, `valor` y su FK. El flujo de responsables de pasos compara UUID de roles; puede conservarse. Las restricciones e índices adicionales del proyecto también difieren, aunque no implican por sí solos rediseñar pantallas.

PostgreSQL no estaba accesible en `localhost:5432` durante la comparación. Los resultados describen archivos y migraciones, no confirman el esquema ni los datos de una instancia activa.

## Estrategia y avance

### 1. Mantener el contrato de la API y el frontend

Conservar `codigo`, `codigoRol`, `idExamen`, `indicaciones`, identificadores, rutas y errores actuales. Un campo del DTO no necesita corresponder directamente a una columna. La adaptación debe concentrarse en el backend; verificar el frontend existente antes de decidir que necesita cambios.

### 2. Roles sin la columna `codigo`

Decisión del usuario (2026-11-21): usar nombres acordados, no UUID configurados. `Rol.getCodigo()` deriva los códigos de Paciente y Atención al público/Administración; las ediciones conservan su clasificación. El DTO mantiene sus códigos y los pasos siguen comparándose por UUID.

Conservar la regla: empleado es cualquier rol activo distinto de Paciente, con las comprobaciones de clínica actuales; Administración conserva sus permisos especiales. La responsabilidad de cada paso sigue comparándose por UUID.

Pendiente: conocer los roles reales de la base del ingeniero. No fijar UUID de nuestra demo ni conceder Administración por heurísticas de nombres. La configuración debe validarse; una identidad especial ausente o ambigua no debe ampliar permisos.

Alcance: entidad Rol, autenticación, consultas públicas, permisos REST, recepción, atención y DTOs de sesión/asignaciones/roles. Revisar todas las consultas JPQL a `codigo`, no solo el mapeo de la entidad.

### 3. Órdenes sin la columna `id_examen`

`procedimiento_paso_examen` define exámenes permitidos en el catálogo del paso; no registra cuál se eligió en una orden concreta. No usar esa asociación como si identificara el examen solicitado para un paciente.

Alternativa recomendada bajo la restricción de conservar el esquema: almacenar el UUID del examen y las indicaciones en un formato estructurado y versionado dentro de `orden_examen.indicaciones`, que ya es `text`. El backend codifica/decodifica; la API sigue exponiendo `idExamen` e `indicaciones` separados. Reutilizar las capacidades JSON existentes, sin agregar dependencias por este motivo.

Implementado y validado con PostgreSQL real, incluyendo reinicio, resultados, conversión y concurrencia. Su límite: no hay FK que proteja la relación orden-examen; el backend debe validar asignabilidad, existencia y eliminaciones, contemplando concurrencia. Debe conservar el límite del texto de usuario sin confundirlo con el tamaño de los metadatos.

Cubrir todas las lecturas y escrituras: rutas REST directas/anidadas, entidad y relación inversa de Examen, servicios, vistas JSF y ediciones de indicaciones. El formato interno no debe mostrarse al usuario ni perderse al editar el texto. Distinguir texto antiguo de metadatos propios y manejar contenido inválido sin inventar una relación.

Conservar selección múltiple, «Ya ordenado», resultados por orden, recargas y reinicios. Las órdenes antiguas con solo texto siguen legibles y sin examen identificado; no inferir su examen por nombre. La conversión transaccional está preparada y probada en una copia: `docs/migrations/20261121_ordenes_base_original.sql`. Seguir `docs/conversion-ordenes-base-original.md` con respaldo antes de desplegar sobre nuestra base. No se ejecutó sobre datos actuales.

### 4. Documentos sin la restricción única adicional

`DocumentoRepository.buscarPersonaPorDocumento` ya rechaza coincidencias entre varias personas (primer bloque). Sin unicidad, un mismo tipo/número podría pertenecer a varias personas.

Rechazar identidades ambiguas en login y consultas públicas de citas; no devolver información de varias personas ni elegir una arbitrariamente. Controlar duplicados al guardar, incluyendo solicitudes simultáneas. La validación «consultar y luego insertar» sin coordinación no reemplaza una restricción única. El flujo visible sigue usando tipo y número de documento.

Revisar también las protecciones de referencias y eliminaciones que hoy descansan en nuestras FK adicionales; reutilizar las validaciones existentes donde ya cubran el caso.

## Validación antes de darlo por resuelto

1. Crear una base de prueba independiente únicamente con original + parche. No usar `preparar-bd-desarrollo.sh` actual: aplica nuestras migraciones adicionales.
2. Mantener `jakarta.persistence.schema-generation.database.action=none` y comprobar que la aplicación no altera el esquema.
3. Preparar fixtures compatibles sin cambiar las migraciones históricas ni cargar demo en la base del ingeniero.
4. Pruebas de persistencia real además de unitarias: los mocks no detectan columnas inexistentes.
5. Recorrer HU1–HU4 con el frontend actual: citas/ticket, login y rechazo de pacientes, selección de rol/clínica, atención por responsable, avanzar/retroceder, cierre y separación entre clínicas.
6. Ordenar varios exámenes, guardar resultados, recargar y reiniciar el backend; verificar persistencia, texto legado, ediciones y contenido inválido.
7. Probar roles especiales mal configurados, roles inactivos y documentos ambiguos/concurrentes sin ampliar accesos.

## Empaquetado posterior

Quedan pendientes las imágenes según la rúbrica, dependencias de aplicación `provided` con sus bibliotecas disponibles en Liberty, contexto `/galeno` y coordinación del proxy del frontend. Estos cambios se deben distinguir de la adaptación del esquema para poder comprobar cada parte.

## Punto de reanudación

El usuario autorizó comenzar la adaptación y confirmó los nombres acordados. Primer bloque implementado:
roles sin columna `codigo`, consultas de pacientes por nombre, protección del renombrado y rechazo de
documentos ambiguos en login/HU1. Contrato JSON del frontend conservado.

Segundo bloque implementado: órdenes sin `orden_examen.id_examen`, persistencia de examen/indicaciones,
protección de borrado y conversión de asociaciones antiguas probada en copia. Ver
[reporte de órdenes](reportes/compatibilidad-ordenes-20261121.md).

Pendiente: controles de duplicados al guardar documentos
(incluida concurrencia), revisión final de referencias sin las FK adicionales, recorrido HU1–HU4 completo
y empaquetado (`provided`, bibliotecas externas, contexto y Docker). No ejecutar las migraciones de
desarrollo sobre la base del ingeniero. No migrar ni borrar datos actuales como parte de este bloque.

Ver el resultado de las pruebas en [reportes/compatibilidad-roles-20261121.md](reportes/compatibilidad-roles-20261121.md).
