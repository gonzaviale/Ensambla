import { Link } from '@tanstack/react-router';
import './sami.css';

const AVISOS = [
  { color: 'celeste', texto: 'Una venta que cayó' },
  { color: 'amarillo', texto: 'Un producto que se termina' },
  { color: 'coral', texto: 'Un precio para ajustar' },
] as const;

// «Sami vigila»: ilustración de la mascota frente a sus pantallas y texto con lo que te avisa.
export function MotorMonitoreo() {
  return (
    <section className="vigia" id="monitoreo" aria-labelledby="vigia-titulo">
      <div className="contenedor vigia-grilla">
        <figure className="vigia-figura revelar">
          <img
            src="/landing/sami-vigila.webp"
            width={1536}
            height={1024}
            loading="lazy"
            decoding="async"
            alt="Sami, el pulpo de Ensambla, frente a varias pantallas con gráficos de ventas, stock y precios."
          />
        </figure>

        <div className="vigia-texto revelar">
          <p className="sami-kicker sami-kicker--claro">Sami vigila</p>
          <h2 id="vigia-titulo">Mientras vos atendés, Sami mira tus números.</h2>
          <p className="vigia-bajada">Y te avisa cuando algo cambia, sin gráficos raros.</p>

          <ul className="vigia-avisos" aria-label="Sami te avisa de">
            {AVISOS.map(({ color, texto }) => (
              <li key={texto}>
                <span className={`vigia-punto vigia-punto--${color}`} aria-hidden="true" />
                {texto}
              </li>
            ))}
          </ul>
          <Link to="/registro" className="boton boton--primario">
            Quiero que Sami vigile
          </Link>
        </div>
      </div>
    </section>
  );
}
