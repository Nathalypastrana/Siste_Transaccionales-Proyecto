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

