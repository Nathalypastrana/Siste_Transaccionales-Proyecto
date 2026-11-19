# Guion de la demostración y lista de entrega en Canvas

## Antes de la presentación (hazlo el día anterior)
- [ ] Backend arranca sin errores. Variables: `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_CORREO`, `ADMIN_PASSWORD`,
      `ASISTENTE_PROVEEDOR=gemini`, `GEMINI_API_KEY`, `GEMINI_MODELO=gemini-3.5-flash-lite` y **`ASISTENTE_SIMULADOR=true`**
      (sin esa última la pantalla *Consultor virtual* no responde).
- [ ] Datos de la demo: 3 usuarios (general, hotel, recepcionista), tipos, habitaciones, tarifas vigentes para las fechas que vas a usar
      y 2 huéspedes con teléfono, uno con una reserva (anota su código `CR-…`).
- [ ] Gemini gratuito tiene límite de uso: prueba el consultor el día de antes y de nuevo 10 minutos antes.
- [ ] Ejecuta todos los casos de `plan-pruebas.md` y guarda las capturas.
- [ ] Ten abiertas: la app, la terminal del backend (log) y Auditoría.

## Orden sugerido (10–12 minutos)
1. **Prototipo (4 min).** Página pública → login como recepcionista → nueva reserva → comprobante → modificar → cancelar.
   Menciona que cada rol ve un panel distinto. Entra como administrador general y muestra *Auditoría* con lo que acabas de hacer.
2. **Pruebas (3 min).** Muestra la salida de `concurrencia.sh` (1 × 201, 9 × 409) y explica el bloqueo pesimista de la habitación;
   la tabla de `plan-pruebas.md` con resultados y la salida de `rendimiento.py`.
3. **IA (4 min).** Recorre las cinco preguntas de `ficha-ia.md` mientras haces la demostración: consentimiento (`ACEPTO`) → disponibilidad
   con precio → estado de una reserva con código → "quiero cambiar mi reserva" (deriva). Enseña en el log la función que pidió el modelo.
4. **Cierre (1 min).** Seguridad y datos personales (roles, JWT, auditoría, consentimiento) y qué quedaría para producción.

## Plan B si algo falla en vivo
- Gemini no responde o sin cupo: muestra las capturas del consultor y explica con el log; cambia `GEMINI_MODELO` solo si ya probaste otro.
- Backend caído: video corto de la demostración grabado antes.

## Entrega en Canvas
- [ ] **Enlace o acceso al prototipo:** URL del repositorio de GitHub (README con pasos para ejecutarlo) y, si lo grabas, un video de la demostración.
      Comprueba que el repositorio no contenga claves (`GEMINI_API_KEY`, `JWT_SECRET`, contraseñas).
- [ ] **Evidencias de pruebas:** `informe-pruebas.md` + `plan-pruebas.md` completo con capturas + salida de `concurrencia.sh`, `verificar_flujo.sql` y `rendimiento.py`.
- [ ] **Evidencia UI → BD:** `evidencia-persistencia.md` con capturas E01–E10 (interfaz y consulta en BD) y los archivos de `evidencias/`.
- [ ] **Credenciales:** `seguridad-credenciales.md`; `git grep -n -i "password\|api_key\|secret"` sin valores reales; `.env` no versionado.
- [ ] **Evidencia del servicio de IA:** `ficha-ia.md` + capturas del consultor y del log.
