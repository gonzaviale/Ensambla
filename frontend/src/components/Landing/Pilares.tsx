import { variables } from './estilos';

const NUMEROS = [
  { color: 'amarillo', valor: '3', texto: 'formas de contarle las cosas: foto, voz o texto' },
  { color: 'celeste', valor: '6', texto: 'bloques listos para encajar y armar tu negocio' },
  { color: 'blanco', valor: '0', texto: 'planillas o cuentas que tengas que armar vos' },
] as const;

const PILARES = [
  {
    color: 'naranja',
    tag: 'Bloques',
    titulo: 'Un bloque por cada parte de tu negocio',
    texto:
      'Productos, ventas, gastos, fiados, pedidos… Cada uno se encaja con los otros. Sumás o sacás bloques cuando quieras, sin romper nada.',
  },
  {
    color: 'amarillo',
    tag: 'Sami',
    titulo: 'Sami lo arma por vos',
    texto: 'Le contás cómo trabajás y te deja todo listo. No hay nada que configurar.',
  },
  {
    color: 'coral',
    tag: 'Fácil',
    titulo: 'Cargá como te salga',
    texto: 'Foto del cuaderno, audio, texto o WhatsApp. Vos revisás y confirmás.',
  },
  {
    color: 'celeste',
    tag: 'Avisos',
    titulo: 'Te avisa antes de que sea tarde',
    texto:
      'Cuándo subir un precio, qué se está por acabar y quién te debe, con palabras simples y una sugerencia para hacer.',
  },
  {
    color: 'blanco',
    tag: 'Tu zona',
    titulo: 'Mirá cómo te va frente a tu barrio',
    texto:
      'Te comparás con comercios cercanos usando datos de varios negocios juntos. Nadie ve los números de otro.',
  },
  {
    color: 'amarillo',
    tag: 'Crece con vos',
    titulo: 'Empezá chico, seguí cuando quieras',
    texto: 'Arrancá con un bloque. Cuando tu negocio crezca, tu sistema crece con él.',
  },
] as const;

export function Pilares() {
  return (
    <section className="seccion seccion--oscura" id="por-que">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Por qué Ensambla</p>
          <h2>
            Simple hoy.
            <br />
            Listo para crecer mañana.
          </h2>
          <p>Un sistema a tu medida, una ayuda que lo arma y avisos que te dicen qué hacer.</p>
        </header>

        <ul className="numeros">
          {NUMEROS.map(({ color, valor, texto }, i) => (
            <li key={texto} className="revelar" style={variables({ '--d': `${i / 10}s` })}>
              <strong className={`numeros__valor numeros__valor--${color}`}>{valor}</strong>
              <span>{texto}</span>
            </li>
          ))}
        </ul>

        <div className="bento">
          {PILARES.map(({ color, tag, titulo, texto }, i) => (
            <article
              key={titulo}
              className={`bento__celda bento__celda--${i + 1} bento__celda--${color} revelar`}
              style={variables({ '--d': `${(i % 3) / 10}s` })}
            >
              <span className="bento__tag">{tag}</span>
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
