import { Link } from '@tanstack/react-router';
import { EscenaBloques } from './sami/EscenaBloques';
import './sami.css';

const AYUDAS = [
  { color: 'celeste', titulo: 'Te escucha', texto: 'Le contás cómo trabajás, con tus palabras.' },
  { color: 'amarillo', titulo: 'Ordena por vos', texto: 'Cada cosa de tu negocio va a su lugar.' },
  { color: 'coral', titulo: 'Te pide tu OK', texto: 'Nada se guarda sin que lo confirmes.' },
] as const;

// «Conocé a Sami»: la mascota recibe bloques, los ordena y los apila (escena con GSAP).
export function SamiAgente() {
  return (
    <section className="sami-seccion" id="sami" aria-labelledby="sami-titulo">
      <div className="contenedor">
        <header className="sami-cabecera revelar">
          <p className="sami-kicker">Conocé a Sami</p>
          <h2 id="sami-titulo">Sami te da una mano. Bueno, ocho.</h2>
          <p>
            Es el asistente de Ensambla. Le contás cómo trabajás y él va encajando cada parte de tu
            negocio, bloque a bloque. Vos no tenés que armar nada.
          </p>
        </header>

        <div className="sami-escenario revelar">
          <EscenaBloques />
        </div>

        <ul className="sami-ayudas">
          {AYUDAS.map(({ color, titulo, texto }) => (
            <li key={titulo} className={`sami-ayuda sami-ayuda--${color}`}>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </li>
          ))}
        </ul>

        <p className="sami-cierre">
          <Link to="/registro" className="boton boton--primario">
            Empezar con Sami
          </Link>
        </p>
      </div>
    </section>
  );
}
