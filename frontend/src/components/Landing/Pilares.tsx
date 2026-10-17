import { variables } from './estilos';

const PILARES = [
  {
    num: '01',
    tag: 'Piezas',
    titulo: 'Modularidad',
    texto:
      'Cada parte de tu negocio es un bloque independiente que podés sumar, mover o quitar cuando quieras.',
  },
  {
    num: '02',
    tag: 'Agente',
    titulo: 'Sami arma tu sistema',
    texto:
      'Te escucha, propone las tablas y las columnas y las deja listas. No hay nada que configurar.',
  },
  {
    num: '03',
    tag: 'Entrada',
    titulo: 'Cargá como te salga',
    texto:
      'Foto del cuaderno, nota de voz, texto o WhatsApp. Revisás y confirmás antes de que se guarde.',
  },
  {
    num: '04',
    tag: 'Monitoreo',
    titulo: 'Avisos para decidir',
    texto:
      'Precios mal ubicados, productos estancados y combos, en lenguaje simple y con una acción sugerida.',
  },
  {
    num: '05',
    tag: 'Zona',
    titulo: 'Datos de tu zona, sin exponer a nadie',
    texto:
      'Te comparás con comercios cercanos con datos agregados y anónimos, siempre de varios negocios.',
  },
  {
    num: '06',
    tag: 'Crecimiento',
    titulo: 'Escalabilidad',
    texto: 'Empezá con un bloque. Cuando tu negocio crezca, tu sistema crece con él.',
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
          <p>Un sistema a tu medida, un agente que lo arma y un motor que te avisa qué hacer.</p>
        </header>

        <div className="pilares">
          {PILARES.map(({ num, tag, titulo, texto }, i) => (
            <article
              key={num}
              className="pilar revelar"
              style={variables({ '--d': `${(i % 3) / 10}s` })}
            >
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
