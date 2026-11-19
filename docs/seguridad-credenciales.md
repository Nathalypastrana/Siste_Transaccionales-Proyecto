# Configuración segura de credenciales

Principio: **ningún secreto vive en el repositorio**. Todo se entrega por variables de entorno; `.env.example` lista los nombres sin valores.

## Inventario de secretos
| Secreto | Variable | Dónde vive | Notas |
|---|---|---|---|
| Usuario/clave de la BD | `DB_USER`, `DB_PASSWORD` | Entorno (obligatorias, sin valor por defecto: sin ellas la app no arranca) | Usar el usuario `hotel_app` (`database/crear_usuario_aplicacion.sql`), no `sa` |
| Firma de sesiones (JWT) | `JWT_SECRET` | Entorno | ≥ 32 caracteres aleatorios: `openssl rand -base64 36` |
| Administrador inicial | `ADMIN_CORREO`, `ADMIN_PASSWORD` | Entorno (solo el primer arranque) | Mín. 8 caracteres; luego quitar la variable |
| Gemini | `GEMINI_API_KEY` | Entorno | Si se expone: revocar y crear otra |
| Correo SMTP | `SPRING_MAIL_*` | Entorno | Usar "contraseña de aplicación", no la clave de la cuenta |
| WhatsApp (Meta) | `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_APP_SECRET`, `WHATSAPP_VERIFY_TOKEN` | Entorno | El token temporal vence; el App Secret valida la firma del webhook |

## Reglas aplicadas
1. **Sin valores por defecto** para `DB_USER`/`DB_PASSWORD` (falla rápido en vez de usar una clave conocida).
2. **Contraseñas de usuarios con BCrypt**; `seguridad.migrar-claves-planas=false` por defecto (no se aceptan claves en texto plano). Los datos de ejemplo traen cuentas **inactivas** con hash de una clave desconocida.
3. Bases antiguas: `database/asegurar_credenciales.sql` desactiva cuentas con clave en texto plano; el administrador les asigna una nueva desde *Administración → Usuarios*.
4. **Mínimo privilegio:** `hotel_app` solo tiene `db_datareader` y `db_datawriter`; el esquema se crea con `schema.sql` y Hibernate no lo modifica.
5. **Tokens de recuperación** guardados solo como hash SHA-256 (30 min, un uso); **auditoría** nunca registra contraseñas ni tokens; teléfonos enmascarados en registros.
6. **Línea de comandos:** pasar la clave de SQL con `SQLCMDPASSWORD` (los scripts lo hacen), no con `-P`, para que no quede en el historial ni en `ps`.
7. En desarrollo sin SMTP el enlace de recuperación se escribe en el log; **no usar así en producción**. `ASISTENTE_SIMULADOR=false` en producción.
8. `.gitignore` excluye `.env*` (salvo `.env.example`), `*.pem`, `*.key`, `application-local.properties`, `target/`, `node_modules/`, `*.log`.

## Procedimiento
```
cp .env.example .env     # completar valores
set -a; source .env; set +a
mvn -f backend/pom.xml spring-boot:run
```
Antes de publicar: `git grep -n -i -E "password|secret|api_key|token" -- ':!docs' ':!*.example'` y revisar que no haya valores reales; `git log -p | grep -i AIza` para detectar una clave de Google en el historial.

## Rotación (hacer antes de la entrega)
- `JWT_SECRET` y la clave de Gemini que se compartieron en el chat durante el desarrollo: **generar nuevas**.
- Si algún secreto llegó a un commit: revocarlo primero y después limpiar el historial (revocar es lo que lo protege).
