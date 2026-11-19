/* Asegura las credenciales de una base que ya tenía usuarios con contraseña en texto plano (fases anteriores).
   Uso:  sqlcmd ... -d ReservasHotelDB -i asegurar_credenciales.sql
   1) Muestra qué cuentas tienen la contraseña SIN cifrar.
   2) Las desactiva y les pone un hash BCrypt de una clave aleatoria que nadie conoce.
   Después, desde Administración > Usuarios el administrador les asigna una contraseña nueva (mínimo 8 caracteres) y las reactiva.
   Las cuentas que ya tienen BCrypt (empiezan por $2) no se tocan. */
SET NOCOUNT ON;

PRINT '--- Cuentas con contraseña en TEXTO PLANO (antes) ---';
SELECT idUsuario, correo, rol, activo FROM Usuario WHERE password NOT LIKE '$2%';

UPDATE Usuario
SET activo = 0,
    password = '$2b$10$DgbtflTZsKqK9x/jXjrAvegjhq.uzD6H33ODehT5jwr34RbBkj3Qi'
WHERE password NOT LIKE '$2%';

PRINT '--- Estado de las contraseñas (después) ---';
SELECT idUsuario, correo, rol, activo,
       CASE WHEN password LIKE '$2%' THEN 'BCrypt' ELSE 'TEXTO PLANO' END AS almacenamiento
FROM Usuario;
GO
