
import { Link } from 'react-router-dom'
import { api } from '../api'
import { Cargando } from '../components/Estado'
import { useCarga } from '../hooks'
import { HOTEL } from '../hotel'
import { moneda } from '../utils'

const imagenesHabitaciones = [
  'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=700',
  'https://images.unsplash.com/photo-1616594039964-ae9021a400a0?w=700',
  'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=700',
]

const servicios = [
  { icono: '🌳', nombre: 'Entorno natural', detalle: 'Paisajes y tranquilidad' },
  { icono: '🐄', nombre: 'Animales', detalle: 'Experiencia campesina' },
  { icono: '🥾', nombre: 'Actividades al aire libre', detalle: 'Disfruta la naturaleza' },
]

export default function Publica() {
  const { datos, cargando, error } = useCarga(api.habitacionesPublicas)

  const whatsapp = HOTEL.whatsapp.replace(/\D/g, '')
  const enlaceWhatsApp = `https://wa.me/${whatsapp}?text=${encodeURIComponent(
    'Hola, estoy interesado en reservar una habitación en Hotel En mi pueblito.'
  )}`

  const contacto = [
    ['Ubicación', `${HOTEL.ciudad}`],
    ['Teléfono', HOTEL.telefono],
    ['Correo', HOTEL.correo],
    HOTEL.checkIn && ['Check-in', HOTEL.checkIn],
    HOTEL.checkOut && ['Check-out', HOTEL.checkOut],
  ].filter(Boolean) as string[][]

  return (
    <div className="hotel-publico">

      {/* NAVEGACIÓN */}
      <header className="hotel-navbar">
        <Link to="/" className="hotel-marca">
          <span className="hotel-logo">
            <img
              src="/img/logo-hotel1.png"
              alt="Logo Hotel En mi pueblito"
              className="hotel-logo"
            />
          </span>
          <span>
            <strong>{HOTEL.nombre2}</strong>
            <small>Tradición, descanso y naturaleza</small>
          </span>
        </Link>

        <nav className="hotel-nav-links">
          <a href="#nosotros">Nosotros</a>
          <a href="#habitaciones">Habitaciones</a>
          <a href="#servicios">Experiencias</a>
          <a href="#contacto">Contacto</a>
        </nav>

        <Link to="/login" className="hotel-acceso">
          Acceso del personal
        </Link>
      </header>

      {/* PORTADA */}
      <section className="hotel-hero">
        <div className="hotel-hero-contenido">

          <h1>
            Bienvenidos a<br />
            <span>En mi pueblito</span>
          </h1>

          <p>{HOTEL.eslogan}</p>

          <p className="hotel-hero-descripcion">
            Un rincón especial para desconectarte de la rutina,
            respirar aire puro y disfrutar de la tranquilidad del campo.
          </p>

          {HOTEL.whatsapp && (
            <a
              href={enlaceWhatsApp}
              target="_blank"
              rel="noreferrer"
              className="hotel-boton-principal"
            >
              Reservar por WhatsApp
            </a>
          )}
        </div>
      </section>

      {/* NOSOTROS */}
      <main className="hotel-contenido">

        <section className="hotel-nosotros" id="nosotros">
          <div className="hotel-nosotros-imagen">
            <img
              src="/img/cerro-tibet.jpg"
              alt="Paisaje natural de Boyacá"
            />
            <span>Un lugar para sentirse como en casa</span>
          </div>

          <div className="hotel-nosotros-texto">
            <span className="hotel-subtitulo">NUESTRA ESENCIA</span>
            <h2>Un pedacito de Boyacá para ti</h2>

            <p>{HOTEL.descripcion}</p>

            <div className="hotel-frase">
              «La tranquilidad del campo, la calidez de nuestro hogar».
            </div>

            <a href="#habitaciones" className="hotel-enlace">
              Descubre nuestras habitaciones →
            </a>
          </div>
        </section>

        {/* HABITACIONES */}
        <section className="hotel-seccion" id="habitaciones">
          <div className="hotel-titulo-seccion">
            <span className="hotel-subtitulo">DESCANSO Y COMODIDAD</span>
            <h2>Nuestras habitaciones</h2>
            <p>
              Encuentra el espacio ideal para disfrutar tu estadía.
            </p>
          </div>

          {cargando && <Cargando />}

          {!cargando && error && (
            <p className="hotel-mensaje">
              No pudimos cargar las habitaciones en este momento.
            </p>
          )}

          {!cargando && !error && datos?.length === 0 && (
            <p className="hotel-mensaje">
              Pronto publicaremos nuestras habitaciones.
            </p>
          )}

          <div className="hotel-habitaciones">
            {(datos ?? []).map((habitacion, index) => (
              <article className="hotel-habitacion" key={habitacion.nombre}>
                <div className="hotel-habitacion-imagen">
                  <img
                    src={imagenesHabitaciones[index % imagenesHabitaciones.length]}
                    alt={`Habitación ${habitacion.nombre}`}
                    loading="lazy"
                  />
                </div>

                <div className="hotel-habitacion-info">
                  <h3>{habitacion.nombre}</h3>

                  {habitacion.descripcion && (
                    <p>{habitacion.descripcion}</p>
                  )}

                  <span className="hotel-capacidad">
                    Capacidad: {habitacion.capacidad}{' '}
                    {habitacion.capacidad === 1 ? 'persona' : 'personas'}
                  </span>

                  <div className="hotel-precio">
                    {habitacion.precioNoche != null
                      ? moneda(habitacion.precioNoche)
                      : 'Consultar tarifa'}
                    <small> / noche</small>
                  </div>

                  <a
                    href={`${enlaceWhatsApp}&room=${encodeURIComponent(habitacion.nombre)}`}
                    target="_blank"
                    rel="noreferrer"
                    className="hotel-boton-habitacion"
                  >
                    Consultar habitación
                  </a>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* EXPERIENCIAS */}
        <section className="hotel-seccion hotel-experiencias" id="servicios">
          <div className="hotel-titulo-seccion">
            <span className="hotel-subtitulo">VIVE LA EXPERIENCIA</span>
            <h2>Disfruta de nuestro entorno</h2>
            <p>
              Conecta con la naturaleza y vive la esencia del campo boyacense.
            </p>
          </div>

          <div className="hotel-servicios">
            {servicios.map((servicio) => (
              <article className="hotel-servicio" key={servicio.nombre}>
                <span className="hotel-servicio-icono">
                  {servicio.icono}
                </span>
                <h3>{servicio.nombre}</h3>
                <p>{servicio.detalle}</p>
              </article>
            ))}
          </div>
        </section>

        {/* CONTACTO */}
        <section className="hotel-contacto" id="contacto">
          <div className="hotel-contacto-texto">
            <span className="hotel-subtitulo">ESTAMOS PARA RECIBIRTE</span>
            <h2>Ven a visitarnos</h2>
            <p>
              Te esperamos en nuestro pequeño rincón de tranquilidad
              en Belén, Boyacá.
            </p>

            {HOTEL.whatsapp && (
              <a
                href={enlaceWhatsApp}
                target="_blank"
                rel="noreferrer"
                className="hotel-boton-principal"
              >
                Escríbenos por WhatsApp
              </a>
            )}
          </div>

          <div className="hotel-contacto-datos">
            {contacto.map(([nombre, valor]) => (
              <div className="hotel-dato" key={nombre}>
                <strong>{nombre}</strong>
                <span>{valor}</span>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* PIE DE PÁGINA */}
      <footer className="hotel-footer">
        <div className="hotel-footer-marca">
          <span className="hotel-logo">
            <img
              src="/img/logo-hotel1.png"
              alt="Logo Hotel En mi pueblito"
              className="hotel-logo"
            />
          </span>
          <div>
            <strong>{HOTEL.nombre}</strong>
            <p>{HOTEL.eslogan}</p>
          </div>
        </div>

        <p>
          © {new Date().getFullYear()} {HOTEL.nombre}.
          Todos los derechos reservados.
        </p>

        <a href="#nosotros">Volver arriba ↑</a>
      </footer>

      {/* BOTÓN FLOTANTE WHATSAPP */}
      {HOTEL.whatsapp && (
        <a
          className="hotel-whatsapp-flotante"
          href={enlaceWhatsApp}
          target="_blank"
          rel="noreferrer"
          aria-label="Contactar por WhatsApp"
        >
          ☎
        </a>
      )}
    </div>
  )
}
