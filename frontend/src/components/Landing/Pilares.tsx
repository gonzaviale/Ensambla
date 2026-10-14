import { variables } from './estilos';

const PILARES = [
  {
    num: '01',
    tag: 'Piezas',
    titulo: 'Modularidad',
    texto:
      'Cada parte de tu negocio es un bloque independiente que podés sumar, mover o quitar cuando quieras.',
    retraso: '0s',
  },
  {
    num: '02',
    tag: 'Orden',
    titulo: 'Simplicidad',
    texto: 'Formularios y tablas que se generan solos. Sin jerga técnica, sin manuales eternos.',
    retraso: '.1s',
  },
  {
    num: '03',
    tag: 'Crecimiento',
    titulo: 'Escalabilidad',
    texto: 'Empezá con un bloque. Cuando tu negocio crezca, tu sistema crece con él.',
    retraso: '.2s',
  },
] as const;

export function Pilares() {
  return (
    <section className="seccion seccion--oscura">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Por qué Ensambla</p>
          <h2>
            Simple hoy.
            <br />
            Listo para crecer mañana.
          </h2>
        </header>

        <div className="pilares">
          {PILARES.map(({ num, tag, titulo, texto, retraso }) => (
            <article key={num} className="pilar revelar" style={variables({ '--d': retraso })}>
              <span className="pilar__num">{num}</span>
              <span className="pilar__tag">{tag}</span>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
