# Datos demo según el guion del Primer Parcial (PPI115 2026 GT02)

Este archivo define los datos de demostración de GalenoSV. Reemplazan a los datos demo actuales
(`Clínica de demostración`, `admin.demo`, `doctor.demo`, `paciente.demo`) para que la base con la que
se revisa el Segundo Parcial sea la misma que pide el guion del Primer Parcial, y para que coincida
con los nombres de las imágenes de referencia del Segundo Parcial.

**Todos los datos son inventados.** Ningún nombre, documento, teléfono ni correo corresponde a una
persona real.

## 1. Clínicas (guion: tres clínicas, dos activas y una inactiva, con tipos distintos)

| Clínica | Estado | Tipo de clínica |
|---|---|---|
| Clínica Santa Ana | Activa | General |
| Clínica San Salvador | Activa | Especialidades |
| Clínica Sonsonate | **Inactiva** | Pediátrica |

Si el modelo no tiene tipo de clínica, usar el campo equivalente que exista y avisar.

## 2. Roles (guion: cuatro activos y uno inactivo)

| Rol | Estado | Es empleado |
|---|---|---|
| Atención al público | Activo | Sí |
| Paciente | Activo | No |
| Enfermera | Activo | Sí |
| Doctor | Activo | Sí |
| Farmacéutico | **Inactivo** | Sí |

## 3. Tipos de medio de contacto (guion: dos activos con expresión regular y uno inactivo)

| Tipo | Estado | Formato | Expresión regular | Indicaciones |
|---|---|---|---|---|
| Teléfono | Activo | 4 dígitos, guion, 4 dígitos; empieza con 2, 6 o 7 | `^[267][0-9]{3}-[0-9]{4}$` | Ejemplo: 7123-4567 |
| Correo electrónico | Activo | texto@dominio.ext | `^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$` | Ejemplo: nombre@correo.com |
| Fax | **Inactivo** | 4 dígitos, guion, 4 dígitos | `^[0-9]{4}-[0-9]{4}$` | Ejemplo: 2222-3333 |

## 4. Tipos de documento (guion: dos activos con expresión regular y uno inactivo)

| Tipo | Estado | Formato | Expresión regular | Indicaciones |
|---|---|---|---|---|
| DUI | Activo | 8 dígitos, guion, 1 dígito | `^[0-9]{8}-[0-9]$` | Ocho dígitos, guion y dígito verificador. Ejemplo: 01000001-1 |
| Pasaporte | Activo | 1 letra mayúscula y 8 dígitos | `^[A-Z][0-9]{8}$` | Una letra y ocho dígitos. Ejemplo: A10000001 |
| Licencia de conducir | **Inactivo** | 4 + 6 + 3 + 1 dígitos con guiones | `^[0-9]{4}-[0-9]{6}-[0-9]{3}-[0-9]$` | Ejemplo: 0101-010101-101-1 |

En la HU1 y la HU2 el selector mostrará DUI y Pasaporte (solo los activos).

## 5. Tipos de examen (guion: dos activos y uno inactivo)

| Tipo de examen | Estado |
|---|---|
| Laboratorio clínico | Activo |
| Imagenología | Activo |
| Patología | **Inactivo** |

## 6. Exámenes (guion: al menos dos activos con tipos activos y uno inactivo)

| Examen | Estado | Tipo de examen |
|---|---|---|
| Hemograma completo | Activo | Laboratorio clínico |
| Glucosa en ayunas | Activo | Laboratorio clínico |
| Examen general de orina | Activo | Laboratorio clínico |
| Rayos X de tórax | Activo | Imagenología |
| Electrocardiograma | **Inactivo** | Imagenología |

## 7. Procedimientos (guion: dos activos; paso inicial y dos subsecuentes entre sí; cada paso con un rol distinto; exámenes en algunos pasos; último paso como finalización)

### Consulta general (activo)

| Orden | Paso | Rol responsable | Exámenes asignables | Finalización |
|---|---|---|---|---|
| 1 (inicial) | Triaje | Enfermera | — | No |
| 2 | Consulta médica | Doctor | Hemograma completo, Glucosa en ayunas, Examen general de orina, Rayos X de tórax; Electrocardiograma asociado pero **inactivo** (para demostrar el filtro) | No |
| 3 | Entrega de indicaciones | Atención al público | — | **Sí** |

Secuencia: Triaje → Consulta médica → Entrega de indicaciones.

### Control de hipertensión (activo)

| Orden | Paso | Rol responsable | Exámenes asignables | Finalización |
|---|---|---|---|---|
| 1 (inicial) | Toma de presión arterial | Enfermera | — | No |
| 2 | Evaluación médica | Doctor | Glucosa en ayunas, Rayos X de tórax | No |
| 3 | Programación de seguimiento | Atención al público | — | **Sí** |

Secuencia: Toma de presión arterial → Evaluación médica → Programación de seguimiento.

## 8. Personas (guion: al menos cuatro, cada una con un rol distinto en la misma clínica; una quinta sin rol)

Fechas de nacimiento fijas (no relativas). Cada persona tiene un teléfono y un correo válidos.

| Persona | Nacimiento | Documentos | Teléfono | Correo | Roles y clínicas | Usuario (login) |
|---|---|---|---|---|---|---|
| Carlos Gómez | 1980-03-15 | DUI `01000001-1`, Pasaporte `A10000001` | 7100-0001 | carlos.gomez@galenosv.test | **Doctor** en Santa Ana y en San Salvador | Sí |
| Lucía Pérez | 1988-07-22 | DUI `01000002-2` | 7100-0002 | lucia.perez@galenosv.test | **Enfermera** en Santa Ana | Sí |
| Marta López | 1990-11-05 | DUI `01000003-3` | 7100-0003 | marta.lopez@galenosv.test | **Atención al público** en Santa Ana y en San Salvador | Sí |
| Ana Martínez | 1992-04-10 (34 años) | DUI `01000004-4`, Pasaporte `B10000004` | 7100-0004 | ana.martinez@galenosv.test | **Paciente** en Santa Ana y en San Salvador | No |
| Pedro Hernández | 1975-01-30 | DUI `01000005-5` | 7100-0005 | pedro.hernandez@galenosv.test | **Sin rol** (quinta persona del guion) | No |
| Rosa Méndez | 1985-09-18 | DUI `01000006-6` | 7100-0006 | rosa.mendez@galenosv.test | **Enfermera** en San Salvador | Sí |
| Jorge Ruiz | 1978-12-02 | DUI `01000007-7` | 7100-0007 | jorge.ruiz@galenosv.test | **Doctor** solo en San Salvador (persona "con roles solo en otras clínicas") | Sí |
| Julia Flores | 1960-06-25 | DUI `01000008-8` | 7100-0008 | julia.flores@galenosv.test | **Paciente** en Santa Ana | No |
| Juan Vásquez | 2001-02-14 | DUI `01000009-9` | 7100-0009 | juan.vasquez@galenosv.test | **Paciente** en Santa Ana | No |

Clínica de trabajo con empleados y pacientes (guion "Cambio de clínica"): **Clínica Santa Ana**.

## 9. Consultas (fechas relativas a hoy, hora de El Salvador)

Cada paso queda asignado a una persona con el rol responsable en la clínica de la consulta.

### Clínica Santa Ana

| # | Paciente | Día y hora | Procedimiento | Estado inicial |
|---|---|---|---|---|
| SA-1 | Ana Martínez | hoy − 20 días, 09:00 | Consulta general | **Cerrada**: los 3 pasos COMPLETADOS con valor (Lucía, Carlos, Marta), una orden de Hemograma completo en Consulta médica |
| SA-2 | Julia Flores | ayer, 15:00 | Control de hipertensión | **Cerrada**: los 3 pasos COMPLETADOS con valor |
| SA-3 | Ana Martínez | hoy, 08:00 | Consulta general | Triaje COMPLETADO por Lucía con valor; **Consulta médica PENDIENTE** para Carlos |
| SA-4 | Julia Flores | hoy, 09:15 | Control de hipertensión | **Toma de presión PENDIENTE** para Lucía |
| SA-5 | Juan Vásquez | hoy, 10:30 | Consulta general | **Triaje PENDIENTE** para Lucía (sin empezar) |
| SA-6 | Ana Martínez | hoy, 11:45 | Consulta general | Triaje y Consulta médica COMPLETADOS, con una orden de Glucosa en ayunas; **Entrega de indicaciones PENDIENTE** para Marta |
| SA-7 | Juan Vásquez | hoy + 3 días, 08:30 | Consulta general | Triaje PENDIENTE (futura) |
| SA-8 | Ana Martínez | hoy + 10 días, 10:00 | Control de hipertensión | Toma de presión PENDIENTE (futura) |

### Clínica San Salvador

| # | Paciente | Día y hora | Procedimiento | Estado inicial |
|---|---|---|---|---|
| SS-1 | Ana Martínez | hoy − 10 días, 14:00 | Consulta general | **Cerrada**: los 3 pasos COMPLETADOS con valor (Rosa, Jorge, Marta) |
| SS-2 | Ana Martínez | hoy, 13:00 | Consulta general | Triaje COMPLETADO por Rosa; **Consulta médica PENDIENTE** para Carlos |
| SS-3 | Ana Martínez | hoy + 5 días, 09:00 | Control de hipertensión | Toma de presión PENDIENTE (futura) |

Textos de ejemplo para los valores (inventados): Triaje "PA 120/80, FC 72, T 36.6 °C, peso 62 kg";
Consulta médica "Paciente estable. Se indican exámenes de control."; Entrega de indicaciones
"Se entregan indicaciones y cita de seguimiento.".

## 10. Para qué sirve cada cuenta en la revisión del Segundo Parcial

| Documento | Persona | Uso |
|---|---|---|
| DUI `01000004-4` o Pasaporte `B10000004` | Ana Martínez (paciente) | **HU1**: 2 clínicas; citas pasadas, de hoy y futuras. Rechazada en el login |
| DUI `01000001-1` o Pasaporte `A10000001` | Carlos Gómez (Doctor en 2 clínicas) | **HU2, HU3 y HU4**: selector con 2 clínicas; atiende la Consulta médica de SA-3 y SS-2 |
| DUI `01000002-2` | Lucía Pérez (Enfermera) | HU3 y HU4: atiende el Triaje de SA-4 y SA-5 |
| DUI `01000003-3` | Marta López (Atención al público, 2 clínicas) | HU3 y HU4: atiende el último paso de SA-6 y cierra la consulta |
| DUI `01000005-5` | Pedro Hernández (sin rol) | HU2: rechazado |
| `12345678-9` | nadie | HU1 y HU2: mensaje neutro |

`reiniciar-demo.sh` debe devolver las 11 consultas a este estado inicial.
