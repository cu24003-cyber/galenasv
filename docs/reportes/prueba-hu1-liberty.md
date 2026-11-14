# Prueba de HU1 contra Liberty

Fecha: 2026-11-13 (hora de El Salvador). Rama: `backend-rubrica`. Servidor: Open Liberty en
`localhost:9080` con los datos demo recién cargados. No se modificó código.

Resultado: **las 5 pruebas pasaron.** Esto cierra el pendiente "No se probó contra Liberty"
de `tarea2-reporte.md`: las proyecciones JPQL con `JOIN`, el `IN :consultas` y la
deserialización de `LocalDate` funcionan con EclipseLink y Yasson.

| # | Prueba | Esperado | Obtenido |
| --- | --- | --- | --- |
| 1 | `clinicas` con DUI `00000000-3` | 200, 2 clínicas | 200, 2 clínicas |
| 2 | `citas` en cada clínica, rango −30/+30 días | 3 citas por clínica, 4 campos | 3 + 3, solo `idConsulta`, `fechaInicio`, `nombreClinica`, `procedimientos` |
| 3 | Ticket de la cita `…-2` con el documento del paciente | 200, archivo empieza con `%PDF` | 200, `%PDF-1.5`, 1330 bytes, 1 página |
| 4 | Ticket de la misma cita con `00000000-2` (doctor.demo) | 404 | 404 `cita.noExiste` |
| 5 | Documento `99999999-9` en `clinicas` y `citas` | 200 `[]` | 200 `[]` en ambas |

## Variables

```bash
API=http://localhost:9080/galenosv/api
ORIGEN='Origin: http://localhost:9080'
DUI=de110004-0000-4000-8000-000000000001
C1=de110000-0000-4000-8000-000000000001
C2=de110000-0000-4000-8000-000000000002
DESDE=$(TZ=America/El_Salvador date -d '-30 days' +%F)   # 2026-10-14
HASTA=$(TZ=America/El_Salvador date -d '+30 days' +%F)   # 2026-12-13
```

## 1. Clínicas del paciente

```bash
curl -s -w '\nHTTP %{http_code}\n' -X POST "$API/publico/clinicas" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"
```

```
[{"idClinica":"de110000-0000-4000-8000-000000000001","nombre":"Clínica de demostración"},{"idClinica":"de110000-0000-4000-8000-000000000002","nombre":"Clínica de demostración Norte"}]
HTTP 200
```

## 2. Citas por clínica (pasado y futuro)

```bash
for C in $C1 $C2; do
  curl -s -w '\nHTTP %{http_code}\n' -X POST "$API/publico/citas" -H "$ORIGEN" \
    -H 'Content-Type: application/json' \
    -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\",\"idClinica\":\"$C\",\"desde\":\"$DESDE\",\"hasta\":\"$HASTA\"}"
done
```

Clínica 1:

```
[{"idConsulta":"de110009-0000-4000-8000-000000000001","fechaInicio":"2026-10-24T09:00:00-06:00","nombreClinica":"Clínica de demostración","procedimientos":["Consulta general de demostración"]},{"idConsulta":"de110009-0000-4000-8000-000000000002","fechaInicio":"2026-11-13T08:00:00-06:00","nombreClinica":"Clínica de demostración","procedimientos":["Consulta general de demostración"]},{"idConsulta":"de110009-0000-4000-8000-000000000003","fechaInicio":"2026-11-20T10:00:00-06:00","nombreClinica":"Clínica de demostración","procedimientos":["Consulta general de demostración"]}]
HTTP 200
```

Clínica 2:

```
[{"idConsulta":"de110009-0000-4000-8000-000000000004","fechaInicio":"2026-11-01T14:00:00-06:00","nombreClinica":"Clínica de demostración Norte","procedimientos":["Consulta general de demostración"]},{"idConsulta":"de110009-0000-4000-8000-000000000005","fechaInicio":"2026-11-13T11:00:00-06:00","nombreClinica":"Clínica de demostración Norte","procedimientos":["Consulta general de demostración"]},{"idConsulta":"de110009-0000-4000-8000-000000000006","fechaInicio":"2026-11-16T15:30:00-06:00","nombreClinica":"Clínica de demostración Norte","procedimientos":["Consulta general de demostración"]}]
HTTP 200
```

Las fechas coinciden con la demo: clínica 1 hace 20 días, hoy 08:00 y en 7 días; clínica 2
hace 12 días, hoy 11:00 y en 3 días. Todas con offset `-06:00`. Ninguna respuesta trae
notas, valores, estados ni datos de la persona.

## 3. Ticket PDF

```bash
curl -s -D - -o /tmp/ticket-hu1.pdf -X POST \
  "$API/publico/citas/de110009-0000-4000-8000-000000000002/ticket" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-3\"}"
head -c 8 /tmp/ticket-hu1.pdf | od -c | head -1
file /tmp/ticket-hu1.pdf
```

```
HTTP/1.1 200 OK
Cache-Control: no-store
Pragma: no-cache
X-Content-Type-Options: nosniff
Content-Disposition: attachment; filename="ticket-cita-de110009.pdf"
Content-Type: application/pdf
Content-Language: en-US
Content-Length: 1330

0000000   %   P   D   F   -   1   .   5
/tmp/ticket-hu1.pdf: PDF document, version 1.5, 1 page(s) (zip deflate encoded)
```

## 4. Ticket con un documento ajeno

```bash
curl -s -w '\nHTTP %{http_code}\n' -X POST \
  "$API/publico/citas/de110009-0000-4000-8000-000000000002/ticket" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"00000000-2\"}"
```

```
{"code":"cita.noExiste","details":[],"message":"No encontramos esa cita para el documento indicado.","path":"/galenosv/api/publico/citas/de110009-0000-4000-8000-000000000002/ticket","status":404,"timestamp":"2026-11-13T15:18:31.238201047Z"}
HTTP 404
```

## 5. Documento inexistente

```bash
curl -s -w '\nHTTP %{http_code}\n' -X POST "$API/publico/clinicas" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"99999999-9\"}"
curl -s -w '\nHTTP %{http_code}\n' -X POST "$API/publico/citas" -H "$ORIGEN" \
  -H 'Content-Type: application/json' \
  -d "{\"idTipoDocumento\":\"$DUI\",\"numeroDocumento\":\"99999999-9\",\"idClinica\":\"$C1\",\"desde\":\"$DESDE\",\"hasta\":\"$HASTA\"}"
```

```
[]
HTTP 200
[]
HTTP 200
```

## Observaciones

- Hubo 7 solicitudes a rutas públicas en esta prueba; cuentan para el límite de 60 por IP
  cada 5 minutos.
- No se probaron aquí el 403 sin `Origin`, el rango invertido ni el 429; no estaban en el
  alcance pedido.
- El PDF quedó en `/tmp/ticket-hu1.pdf`; no se abrió para revisar su contenido visual.
- `mvn test` después de la prueba: 634 pruebas, 0 fallos, 0 errores, 0 omitidas.
