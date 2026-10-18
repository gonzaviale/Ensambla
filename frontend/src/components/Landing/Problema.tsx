import {
  CoinsIcon,
  NotebookIcon,
  PackageIcon,
  PuzzlePieceIcon,
  TagIcon,
} from '@phosphor-icons/react';
import type { Icon } from '@phosphor-icons/react';
import { variables } from './estilos';

interface Dolor {
  color: 'amarillo' | 'coral' | 'celeste' | 'petroleo' | 'naranja';
  Icono: Icon;
  frase: string;
  texto: string;
}

const DOLORES: Dolor[] = [
  {
    color: 'amarillo',
    Icono: NotebookIcon,
    frase: 'Anoto todo en un cuaderno o en WhatsApp.',
    texto: 'Ventas, deudas y pedidos desparramados. Cuando buscás un dato, no lo encontrás.',
  },
  {
    color: 'coral',
    Icono: CoinsIcon,
    frase: 'A fin de mes no sé cuánto gané.',
    texto: 'Entra plata, sale plata, y nunca sabés cuánto te quedó de verdad.',
  },
  {
    color: 'celeste',
    Icono: TagIcon,
    frase: 'Se me pasó subir el precio.',
    texto:
      'El proveedor aumentó y seguiste vendiendo al precio de antes. Ganás menos sin darte cuenta.',
  },
  {
    color: 'petroleo',
    Icono: PackageIcon,
    frase: 'Me quedé sin stock.',
    texto: 'Justo del producto que más sale. Te enterás cuando el cliente ya preguntó.',
  },
  {
    color: 'naranja',
    Icono: PuzzlePieceIcon,
    frase: 'Los sistemas me parecen un lío.',
    texto: 'Mil botones, planillas y palabras raras. No tenés tiempo para aprender todo eso.',
  },
];

export function Problema() {
  return (
    <section className="seccion seccion--celeste problema" id="el-problema">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">El problema</p>
          <h2>
            Tu negocio anda.
            <br />
            Lo que te complica es el desorden.
          </h2>
          <p>
            Atendés, vendés, anotás… y a la noche no sabés bien cómo te fue. Si te suena, no estás
            solo: le pasa a casi todos los que tienen un negocio chico.
          </p>
        </header>

        <ul className="dolores">
          {DOLORES.map(({ color, Icono, frase, texto }, i) => (
            <li
              key={frase}
              className={`dolor dolor--${color} revelar`}
              style={variables({ '--d': `${(i % 3) / 10}s` })}
            >
              <span className="dolor__icono" aria-hidden="true">
                <Icono size={28} weight="bold" />
              </span>
              <h3>{`“${frase}”`}</h3>
              <p>{texto}</p>
            </li>
          ))}
        </ul>

        <div className="problema__salida revelar">
          <p>Ensambla existe para que nada de esto te siga pasando.</p>
          <a href="#sami" className="boton boton--oscuro">
            Conocé a Sami
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 12h14M13 6l6 6-6 6" />
            </svg>
          </a>
        </div>
      </div>
    </section>
  );
}
