import { Link } from '@tanstack/react-router';
import './sami.css';

const AYUDAS = [
  { color: 'celeste', titulo: 'Te escucha', texto: 'Le contás cómo trabajás, con tus palabras.' },
  { color: 'amarillo', titulo: 'Ordena por vos', texto: 'Cada cosa de tu negocio va a su lugar.' },
  { color: 'coral', titulo: 'Te pide tu OK', texto: 'Nada se guarda sin que lo confirmes.' },
] as const;

// «Conocé a Sami»: texto a un lado e ilustración de la mascota armando bloques al otro.
export function SamiAgente() {
  return (
    <section className="sami-seccion" id="sami" aria-labelledby="sami-titulo">
      <div className="contenedor sami-grilla">
        <div className="sami-texto revelar">
          <p className="sami-kicker">Conocé a Sami</p>
          <h2 id="sami-titulo">Sami te da una mano. Bueno, ocho.</h2>
          <p className="sami-bajada">
            Es el asistente de Ensambla. Le contás cómo trabajás y él va encajando cada parte de tu
            negocio, bloque a bloque. Vos no tenés que armar nada.
          </p>

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

        <figure className="sami-figura revelar">
          <img
            src="/landing/sami-bloques.webp"
            width={1536}
            height={1024}
            loading="lazy"
            decoding="async"
            alt="Sami, el pulpo de Ensambla, sostiene un bloque naranja junto a una pila de bloques de colores."
          />
        </figure>
      </div>
    </section>
  );
}
