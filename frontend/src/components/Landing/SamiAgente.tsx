import { Link } from '@tanstack/react-router';
import { SamiEscena } from './Sami';

const CAPACIDADES = [
  {
    color: 'naranja',
    titulo: 'Te escucha',
    texto: 'Contale qué vendés y cómo trabajás, con tus palabras y sin tecnicismos.',
  },
  {
    color: 'celeste',
    titulo: 'Arma tu sistema',
    texto: 'Propone la plantilla, las tablas y las columnas que tu negocio necesita.',
  },
  {
    color: 'amarillo',
    titulo: 'Ordena lo que cargás',
    texto: 'Entiende fotos, audios y mensajes, y los deja en el bloque que corresponde.',
  },
  {
    color: 'coral',
    titulo: 'Te pide tu OK',
    texto: 'Nada se guarda sin que lo confirmes. La última palabra siempre es tuya.',
  },
] as const;

export function SamiAgente() {
  return (
    <section className="seccion seccion--tono" id="sami">
      <div className="contenedor sami">
        <div className="sami__texto revelar">
          <p className="sobretitulo">Conocé a Sami</p>
          <h2>Sami arma tu sistema por vos.</h2>
          <p className="sami__bajada">
            Sami es el agente de Ensambla, nuestro pulpo con muchos brazos para que no te falte
            ninguno. Te escucha, entiende cómo trabajás y ordena tu negocio en bloques. No hay que
            diseñar tablas ni aprender a configurar nada.
          </p>
          <ul className="sami__lista">
            {CAPACIDADES.map(({ color, titulo, texto }) => (
              <li key={titulo}>
                <span className={`sami__punto sami__punto--${color}`} aria-hidden="true" />
                <div>
                  <h3>{titulo}</h3>
                  <p>{texto}</p>
                </div>
              </li>
            ))}
          </ul>
          <Link to="/registro" className="boton boton--primario">
            Empezá con Sami
          </Link>
        </div>

        <div className="sami__escena revelar">
          <div className="sami__globo" aria-hidden="true">
            <span>Contame qué vendés…</span>
            <span>Armé tus bloques.</span>
            <span>¡Listo! ¿Lo confirmás?</span>
          </div>
          <SamiEscena />
        </div>
      </div>
    </section>
  );
}
