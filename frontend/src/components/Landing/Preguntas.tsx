const PREGUNTAS = [
  {
    pregunta: '¿Tengo que cargar todo de una?',
    respuesta:
      'Podés empezar por una parte de tu negocio, como las ventas. Sumá el resto cuando lo necesites.',
  },
  {
    pregunta: '¿Tengo que dejar de usar mi cuaderno?',
    respuesta:
      'Podés seguir anotando como hasta ahora y pasar tus anotaciones de a poco. Vos elegís por dónde empezar.',
  },
  {
    pregunta: '¿Qué pasa si una foto se lee mal?',
    respuesta:
      'Antes de guardar, revisás los datos. Podés corregirlos o quitar lo que no querés incorporar.',
  },
] as const;

export function Preguntas() {
  return (
    <section className="ens-section" id="tus-dudas" aria-labelledby="dudas-titulo">
      <div className="ens-wrap ens-faq">
        <div>
          <p className="ens-eyebrow">Antes de empezar</p>
          <h2 id="dudas-titulo">Las dudas también tienen su lugar.</h2>
        </div>
        <div>
          {PREGUNTAS.map(({ pregunta, respuesta }) => (
            <details key={pregunta}>
              <summary>{pregunta}</summary>
              <p>{respuesta}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
