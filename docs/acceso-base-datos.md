# Acceso de ChivoMed en la base de datos

## Esquema vigente

El login no requiere una cuenta en una tabla `usuario`. Busca el documento por tipo y valor,
comprueba que el tipo de documento esté activo y obtiene la persona asociada. Los roles y sus
clínicas siguen en `persona_rol` y determinan las páginas y operaciones protegidas.

La migración `20261104_usuarios_roles.sql` es un antecedente histórico que creó la tabla
`usuario`. La base completa aún puede crearla por compatibilidad con la secuencia histórica;
`20261115_login_por_documento.sql` la elimina después. El script de preparación ya aplica ambas
en el orden correcto. No edites las migraciones históricas.

## Base nueva o entorno local

Desde la raíz del backend, preparar el esquema y los datos demo con:

```bash
bash scripts/preparar-bd-desarrollo.sh [contenedor] [base] [usuario_postgres]
```

El script crea o completa el esquema y aplica `20261115_login_por_documento.sql`. Para revisar
el SQL de la migración sin ejecutarla, consulta
`docs/migrations/20261115_login_por_documento.sql`.

## Una base existente

Antes de cambiar una base que ya contiene datos, crea y comprueba un respaldo con `pg_dump`.
Después puedes quitar la tabla de cuentas obsoleta aplicando la migración:

```bash
psql -X -v ON_ERROR_STOP=1 -d NOMBRE_BASE -f docs/migrations/20261115_login_por_documento.sql
```

La migración es transaccional y elimina únicamente `public.usuario`; no altera personas,
documentos, roles, clínicas, asignaciones ni historial clínico. No se ha aplicado a ninguna base
de datos desde este cambio.

## Acceso según rol

Una persona con documento de tipo activo puede iniciar sesión aunque tenga rol `PACIENTE` o no
tenga asignaciones. Eso no concede acceso a las páginas de personal: las rutas protegidas siguen
validando el rol, la asignación propia y el estado de la clínica.
