import { variables } from './estilos';

const PASOS = [
  {
    color: 'celeste',
    num: '1',
    titulo: 'Contale tu negocio',
    texto: 'Decile a Sami qué vendés y cómo trabajás, como si se lo contaras a un amigo.',
  },
  {
    color: 'coral',
    num: '2',
    titulo: 'Mandale lo que anotás',
    texto: 'Una foto del cuaderno, un audio o un mensaje. Vos revisás y confirmás.',
  },
  {
    color: 'petroleo',
    num: '3',
    titulo: 'Mirá cómo te va',
    texto: 'Sabés cuánto ganaste, quién te debe y cuándo conviene subir un precio.',
  },
] as const;

export function ComoFunciona() {
  return (
    <section className="seccion seccion--amarillo como" id="como-funciona">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Cómo te ayuda</p>
          <h2>Tres pasos y listo.</h2>
          <p>
            Cada parte de tu negocio es un bloque, y los bloques se encajan entre sí. Sami los arma
            por vos.
          </p>
        </header>

        <ol className="tiempo">
          {PASOS.map(({ color, num, titulo, texto }, i) => (
            <li
              key={num}
              className="tiempo__paso revelar"
              style={variables({ '--d': `${i / 8}s` })}
            >
              <span className={`tiempo__nodo tiempo__nodo--${color}`} aria-hidden="true">
                {num}
              </span>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
