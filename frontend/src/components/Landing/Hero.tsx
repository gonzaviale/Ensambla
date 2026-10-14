import { Link } from '@tanstack/react-router';
import { variables } from './estilos';

const BLOQUES_3D = [
  { color: 'logo-naranja', x: 0, y: 0, w: 2.74, h: 4.01, ex: -1, ey: -1, ez: 0.4, dl: '0s' },
  { color: 'logo-turquesa', x: 0, y: 4.38, w: 2.74, h: 1.82, ex: -1, ey: 0.9, ez: -0.3, dl: '0.1s' },
  { color: 'logo-amarillo', x: 3.1, y: 0, w: 2.74, h: 1.82, ex: 0.6, ey: -1.3, ez: 0.5, dl: '0.2s' },
  { color: 'logo-celeste', x: 3.1, y: 2.19, w: 2.74, h: 1.82, ex: 1.2, ey: -0.2, ez: -0.4, dl: '0.3s' },
  { color: 'logo-coral', x: 3.1, y: 4.38, w: 2.74, h: 1.82, ex: 0.6, ey: 1, ez: 0.3, dl: '0.4s' },
] as const;

const FICHAS = [
  { n: 1, icono: '▤', color: 'naranja', titulo: 'Inventario', detalle: '128 productos' },
  { n: 2, icono: '✓', color: 'coral', titulo: 'Pedido #204', detalle: 'Listo para entregar' },
  { n: 3, icono: '◎', color: 'pizarra', titulo: '+12 clientes', detalle: 'esta semana' },
] as const;

const DATOS = [
  { valor: '0', texto: 'líneas de código' },
  { valor: '3', texto: 'plantillas para empezar' },
  { valor: '∞', texto: 'combinaciones de bloques' },
] as const;

export function Hero() {
  return (
    <section className="hero" id="inicio">
      <div className="hero__fondo" aria-hidden="true">
        <span className="deco deco--1" />
        <span className="deco deco--2" />
        <span className="deco deco--3" />
        <span className="deco deco--4" />
      </div>

      <div className="contenedor hero__grilla">
        <div className="hero__texto">
          <p className="etiqueta aparecer">
            <span className="etiqueta__punto" aria-hidden="true" />
            Plataforma no-code para emprendedores, comercios y PyMEs
          </p>
          <h1 className="hero__titulo">
            <span className="linea aparecer" style={variables({ '--d': '.05s' })}>
              Tu sistema,
            </span>{' '}
            <span className="linea aparecer" style={variables({ '--d': '.15s' })}>
              un <span className="resaltado">bloque</span>
            </span>{' '}
            <span className="linea aparecer" style={variables({ '--d': '.25s' })}>
              a la vez.
            </span>
          </h1>
          <span
            className="subrayado aparecer"
            style={variables({ '--d': '.35s' })}
            aria-hidden="true"
          />
          <p className="hero__bajada aparecer" style={variables({ '--d': '.4s' })}>
            Convertí los procesos de tu negocio en herramientas digitales simples. Elegí una
            plantilla, sumá bloques de inventario, pedidos o clientes, y empezá a usarlos hoy. Sin
            programar.
          </p>
          <div className="hero__acciones aparecer" style={variables({ '--d': '.5s' })}>
            <Link to="/registro" className="boton boton--primario">
              Crear mi sistema gratis
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </Link>
            <a href="#como-funciona" className="boton boton--secundario">
              Ver cómo funciona
            </a>
          </div>
          <ul className="hero__datos aparecer" style={variables({ '--d': '.6s' })}>
            {DATOS.map(({ valor, texto }) => (
              <li key={texto}>
                <strong>{valor}</strong> {texto}
              </li>
            ))}
          </ul>
        </div>

        <div className="hero__visual" aria-hidden="true">
          <div className="hero__aro" />
          {/* Composición de bloques en CSS 3D (misma estructura que la portada del manual) */}
          <div className="bloques3d">
            <div className="bloques3d__sombra" />
            <div className="bloques3d__escena">
              {BLOQUES_3D.map(({ color, x, y, w, h, ex, ey, ez, dl }, i) => (
                <div
                  key={i}
                  className={`b3d b3d--${color}`}
                  style={variables({
                    '--x': x,
                    '--y': y,
                    '--w': w,
                    '--h': h,
                    '--ex': ex,
                    '--ey': ey,
                    '--ez': ez,
                    '--dl': dl,
                  })}
                >
                  <i className="b3d__frente" />
                  <i className="b3d__arriba" />
                  <i className="b3d__der" />
                  <i className="b3d__izq" />
                </div>
              ))}
            </div>
          </div>
          {FICHAS.map(({ n, icono, color, titulo, detalle }) => (
            <div key={n} className={`ficha ficha--${n}`}>
              <span className={`ficha__icono ficha__icono--${color}`}>{icono}</span>
              <div>
                <b>{titulo}</b>
                <small>{detalle}</small>
              </div>
            </div>
          ))}
        </div>
      </div>

      <a href="#como-funciona" className="hero__bajar" aria-label="Bajar a cómo funciona">
        <span />
      </a>
    </section>
  );
}
