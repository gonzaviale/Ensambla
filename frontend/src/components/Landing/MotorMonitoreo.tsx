import { Link } from '@tanstack/react-router';
import { EscenaMonitores } from './sami/EscenaMonitores';
import './sami.css';

const AVISOS = [
  { color: 'celeste', texto: 'Una venta que cayó' },
  { color: 'amarillo', texto: 'Un producto que se termina' },
  { color: 'coral', texto: 'Un precio para ajustar' },
] as const;

// «Sami vigila»: la mascota mira tres pantallas y avisa cuando algo cambia (escena con GSAP).
export function MotorMonitoreo() {
  return (
    <section className="vigia" id="monitoreo" aria-labelledby="vigia-titulo">
      <div className="contenedor">
        <header className="vigia-cabecera revelar">
          <p className="sami-kicker sami-kicker--claro">Sami vigila</p>
          <h2 id="vigia-titulo">Mientras vos atendés, Sami mira tus números.</h2>
          <p>Y te avisa cuando algo cambia, sin gráficos raros.</p>
        </header>

        <div className="vigia-escenario revelar">
          <EscenaMonitores />
        </div>

        <div className="vigia-pie">
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
