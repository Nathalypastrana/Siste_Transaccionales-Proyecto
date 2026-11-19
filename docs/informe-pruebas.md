# Informe de pruebas

Proyecto: sistema transaccional de reservas de hotel (UMB, Sistemas Transaccionales) — Laura Nathaly Pastrana Gallo y Ariadna Cardona.
Versión probada (commit): `________` (obtener con `git rev-parse --short HEAD`) · Fecha de ejecución: `________` · Responsable: `________`

## 1. Objetivo y alcance
Verificar que los procesos transaccionales (reservar, modificar, cancelar, comprobante), la seguridad por roles, la concurrencia,
las notificaciones y el consultor de IA funcionan y que los datos quedan persistidos de forma consistente.
Fuera de alcance: pruebas de carga a escala de producción y envío masivo real por WhatsApp (se prueba con número de pruebas de Meta).

## 2. Entorno
| Elemento | Valor |
|---|---|
| Sistema operativo | macOS |
| Java / Maven | 17 / Spring Boot 3.3.5 |
| Base de datos | SQL Server (Docker, `ReservasHotelDB`) |
| Frontend | React 18 + TypeScript + Vite (Node `____`) |
| Datos | `schema.sql` + `datos_prueba.sql` (+ `datos_rendimiento.sql` para rendimiento) |
| Credenciales | variables de entorno; sin contraseñas en el repositorio (ver `seguridad-credenciales.md`) |

## 3. Estrategia
| Tipo | Herramienta | Documento / script |
|---|---|---|
| Funcionales y de seguridad (38 casos) | Interfaz y `curl` | `plan-pruebas.md` (P01–P38) |
| Persistencia UI → BD | SQL Server | `evidencia-persistencia.md`, `pruebas/consultas_evidencia.sql`, `evidencia.sh`, `evidencia_e2e.sh` |
| Concurrencia | 10 peticiones simultáneas | `pruebas/concurrencia.sh` |
| Rendimiento | script Python | `pruebas/rendimiento.py` |
| Verificación de flujo en BD | T-SQL | `database/verificar_flujo.sql`, `pruebas/pruebas.sql` |
| Servicio de IA | consultor + log | `ficha-ia.md`, `asistente-ia.md` |

## 4. Resultados
Resumen (completar tras ejecutar `plan-pruebas.md`):
| Total | Aprobados | Fallidos | Pendientes |
|---|---|---|---|
| 38 | __ | __ | __ |

Concurrencia: 10 solicitudes sobre la misma habitación/fechas → esperado 1×201 y 9×409; obtenido: `________`.
Rendimiento: `________` (pegar resumen de `rendimiento.py`).
Persistencia: ver tabla E01–E10 en `evidencia-persistencia.md`.

## 5. Defectos encontrados y corregidos
| # | Defecto | Causa | Corrección | Estado |
|---|---|---|---|---|
| 1 | Puerto 8080 ocupado al arrancar | Instancia previa | Cerrar proceso (`lsof -i :8080`) | Cerrado |
| 2 | Credenciales por defecto y claves en texto plano en datos de ejemplo | Configuración de desarrollo | Variables obligatorias, seed con BCrypt e inactivo, `asegurar_credenciales.sql` | Cerrado |
| 3 | `____` | | | |

## 6. Conclusiones
Los flujos principales cumplen los criterios de aceptación cuando todos los casos quedan en ✔. Riesgos abiertos: WhatsApp en producción exige
plantilla aprobada por Meta; rotar las claves que se compartieron durante el desarrollo.
