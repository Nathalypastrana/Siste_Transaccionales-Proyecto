# Frontend - Reservas de hotel

React 18 + TypeScript + Vite. Consume la API del backend (Spring Boot, puerto 8080).

## Pantallas

| Ruta | Descripción |
|------|-------------|
| `/` | Panel con indicadores y últimas reservas |
| `/reservas/nueva` | Formulario de nueva reserva (operación principal): valida campos, consulta las habitaciones libres según las fechas y calcula el total |
| `/reservas` | Consulta de reservas, búsqueda y cancelación |
| `/habitaciones` | Habitaciones con tipo, capacidad, tarifa vigente y estado (filtro por estado) |
| `/huespedes` | Registro de huéspedes con validaciones y listado |

## Ejecutar

Requiere Node 18+ y el backend corriendo en `http://localhost:8080`.

```
cd frontend
npm install
npm run dev
```

Abrir http://localhost:5173. Para apuntar a otra URL del backend, crear `.env.local` con `VITE_API_URL=http://host:puerto`.

## Estructura

```
src/
├── api.ts          # Cliente HTTP hacia el backend
├── types.ts        # Tipos TypeScript de la API
├── hooks.ts        # useCarga: carga de datos con estados de carga y error
├── utils.ts        # Fechas y formato de moneda
├── components/     # Insignia de estado, alertas
└── pages/          # Inicio, Habitaciones, Huespedes, NuevaReserva, Reservas
```
