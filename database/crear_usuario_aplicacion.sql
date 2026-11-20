-- Requerido por los índices filtrados (UX_Huesped_Correo, UX_Huesped_Telefono): sqlcmd usa QUOTED_IDENTIFIER OFF por defecto
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

/* Usuario de base de datos EXCLUSIVO de la aplicación, con los permisos mínimos (solo leer y escribir datos).
   La aplicación no necesita ser 'sa': el esquema lo crea schema.sql y Hibernate no modifica tablas (ddl-auto=none).
   Uso (con el login administrador 'sa', solo una vez):
     sqlcmd -S localhost -U sa -C -i crear_usuario_aplicacion.sql -v APP_DB_PASSWORD="<clave-larga-y-unica>"
   Luego arranca el backend con DB_USER=hotel_app y DB_PASSWORD=<esa clave>. */
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'hotel_app')
    CREATE LOGIN hotel_app WITH PASSWORD = '$(APP_DB_PASSWORD)', CHECK_POLICY = ON;
GO
USE ReservasHotelDB;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'hotel_app')
    CREATE USER hotel_app FOR LOGIN hotel_app;
ALTER ROLE db_datareader ADD MEMBER hotel_app;
ALTER ROLE db_datawriter ADD MEMBER hotel_app;
GO
