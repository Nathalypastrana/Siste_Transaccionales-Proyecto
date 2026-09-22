# Diseño de un Sistema Transaccional para la Gestión de Reservas en un Hotel

## Descripción

Este proyecto propone el diseño de un sistema transaccional para la gestión de reservas en un hotel, orientado a mejorar el control de habitaciones, huéspedes, tarifas, disponibilidad y reservas.

La solución busca reducir errores asociados a procesos manuales, como duplicidad de información, pérdida de datos, disponibilidad desactualizada y reservas duplicadas.

El sistema implementará mecanismos de gestión de transacciones basados en las propiedades ACID: Atomicidad, Consistencia, Aislamiento y Durabilidad, con el propósito de garantizar la integridad y confiabilidad de la información durante las diferentes operaciones realizadas por los usuarios.

Uno de los principales aspectos del proyecto será el control de concurrencia, debido a que dos o más usuarios podrían intentar reservar la misma habitación para las mismas fechas de manera simultánea.

## Objetivo general

Diseñar un sistema transaccional para la gestión de reservas hoteleras que permita administrar huéspedes, habitaciones, tarifas, disponibilidad y reservas, garantizando la integridad y consistencia de la información mediante propiedades ACID y mecanismos de control de concurrencia.

## Objetivos específicos

- Diseñar una estructura de información para gestionar huéspedes, habitaciones, tarifas y reservas.
- Implementar operaciones transaccionales para la creación, modificación y cancelación de reservas.
- Aplicar las propiedades ACID para garantizar la integridad y consistencia de los datos.
- Implementar mecanismos de control de concurrencia para evitar reservas duplicadas.
- Realizar pruebas de concurrencia mediante solicitudes simultáneas sobre una misma habitación.
- Verificar que únicamente una transacción pueda confirmar una reserva cuando existan solicitudes simultáneas para el mismo recurso y periodo.
- Validar que las operaciones fallidas sean rechazadas o revertidas sin generar inconsistencias en la base de datos.

## Tecnologías utilizadas

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- React
- TypeScript
- Microsoft SQL Server

## Base de datos

El sistema utiliza Microsoft SQL Server como sistema gestor de base de datos.

La base de datos utilizada en el proyecto se denomina:

`ReservasHotelDB`

La estructura está organizada mediante siete tablas principales:

1. **TipoHabitacion:** almacena los tipos de habitación disponibles y su capacidad.
2. **Habitacion:** almacena la información de las habitaciones, su número, piso, capacidad y estado.
3. **Tarifa:** almacena los valores de las tarifas asociadas a los tipos de habitación y sus periodos de vigencia.
4. **Huesped:** almacena la información de los huéspedes.
5. **Usuario:** almacena los usuarios encargados de operar el sistema y sus roles.
6. **Reserva:** registra las reservas realizadas, relacionando huéspedes, habitaciones, usuarios y tarifas.
7. **Comprobante:** registra los comprobantes asociados a las reservas.

La tabla `Reserva` constituye la entidad central del modelo, debido a que relaciona información de huéspedes, habitaciones, usuarios y tarifas.

## Integridad de los datos

La base de datos utiliza diferentes restricciones para mantener la integridad de la información:

- Claves primarias (`PRIMARY KEY`) para identificar de forma única cada registro.
- Claves foráneas (`FOREIGN KEY`) para establecer las relaciones entre las tablas.
- Restricciones `CHECK` para validar valores permitidos.
- Restricciones `UNIQUE` para evitar registros duplicados.
- Valores `DEFAULT` para establecer valores automáticos.
- Tipos de datos adecuados para cada atributo.

Entre las validaciones implementadas se encuentra el control de fechas de las reservas, donde la fecha de salida debe ser posterior a la fecha de entrada.

## Datos de prueba

El proyecto incluye datos iniciales para comprobar el funcionamiento de la base de datos.

Los datos de prueba incluyen:

- Tipos de habitación.
- Habitaciones.
- Usuarios.
- Huéspedes.
- Tarifas.
- Reservas.
- Comprobantes.

Los datos se encuentran en el archivo:

`database/datos_prueba.sql`

## Pruebas de la base de datos

Las consultas de comprobación se encuentran en:

`database/pruebas.sql`

Estas pruebas permiten verificar:

- Los datos almacenados en las tablas.
- Las restricciones de integridad.
- Las claves foráneas.
- Las relaciones entre las entidades.
- El rechazo de datos que no cumplen las restricciones establecidas.

Como prueba de integridad, se verifica que una reserva con una fecha de salida anterior a la fecha de entrada sea rechazada por la restricción `CK_Reserva_Fechas`.