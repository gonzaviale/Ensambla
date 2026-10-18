import { Link } from '@tanstack/react-router';
import { variables } from './estilos';

// Composición del isotipo: el bloque grande es el naranja (la persona que arma su sistema).
const BLOQUES_3D = [
  { color: 'naranja', x: 0, y: 0, w: 2.74, h: 4.01, ex: -1, ey: -1, ez: 0.4, dl: '0s' },
  { color: 'petroleo', x: 0, y: 4.38, w: 2.74, h: 1.82, ex: -1, ey: 0.9, ez: -0.3, dl: '0.1s' },
  { color: 'amarillo', x: 3.1, y: 0, w: 2.74, h: 1.82, ex: 0.6, ey: -1.3, ez: 0.5, dl: '0.2s' },
  { color: 'celeste', x: 3.1, y: 2.19, w: 2.74, h: 1.82, ex: 1.2, ey: -0.2, ez: -0.4, dl: '0.3s' },
  { color: 'coral', x: 3.1, y: 4.38, w: 2.74, h: 1.82, ex: 0.6, ey: 1, ez: 0.3, dl: '0.4s' },
] as const;

const FICHAS = [
  { n: 1, icono: '▤', color: 'celeste', titulo: 'Tus productos', detalle: '128 cargados' },
  { n: 2, icono: '$', color: 'amarillo', titulo: 'Este mes te quedó', detalle: '$ 184.500' },
  { n: 3, icono: '!', color: 'coral', titulo: 'Subí la yerba', detalle: 'Está 8 % abajo' },
] as const;

const DATOS = [
  { valor: '3', texto: 'formas de contarle: foto, voz o texto' },
  { valor: '0', texto: 'planillas que armar' },
  { valor: '1', texto: 'asistente que te ayuda: Sami' },
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

      <div className="contenedor hero__centro">
        <p className="etiqueta aparecer">
          <span className="etiqueta__punto" aria-hidden="true" />
          Tu sistema, un bloque a la vez.
        </p>
        <h1 className="hero__titulo">
          <span className="linea aparecer" style={variables({ '--d': '.05s' })}>
            ¿Todavía anotás todo
          </span>{' '}
          <span className="linea aparecer" style={variables({ '--d': '.15s' })}>
            en el cuaderno?
          </span>
        </h1>
        <p className="hero__bajada aparecer" style={variables({ '--d': '.3s' })}>
          Te entiendo: no tenés tiempo para aprender un sistema. Por eso Sami, nuestro asistente,
          arma el de tu negocio con bloques. Vos le contás cómo trabajás, y él se encarga del resto.
        </p>
        <div className="hero__acciones aparecer" style={variables({ '--d': '.4s' })}>
          <Link to="/registro" className="boton boton--primario boton--grande">
            Crear mi sistema gratis
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 12h14M13 6l6 6-6 6" />
            </svg>
          </Link>
          <a href="#el-problema" className="boton boton--secundario boton--grande">
            ¿Te pasa esto?
          </a>
        </div>
      </div>

      <div className="contenedor">
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
          <div className="hero__fichas">
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

        <ul className="hero__datos aparecer" style={variables({ '--d': '.6s' })}>
          {DATOS.map(({ valor, texto }) => (
            <li key={texto}>
              <strong>{valor}</strong> {texto}
            </li>
          ))}
        </ul>
      </div>

      <a href="#el-problema" className="hero__bajar" aria-label="Bajar a ver el problema">
        <span />
      </a>
    </section>
  );
}
