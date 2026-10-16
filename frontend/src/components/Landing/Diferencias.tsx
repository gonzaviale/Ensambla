const FILAS = [
  {
    herramienta: 'Treinta, Kyte',
    haces: 'Cargás cada venta, una por una.',
    queda:
      'Muy buenas para llevar las ventas del mostrador; están pensadas para la venta minorista.',
    propia: false,
  },
  {
    herramienta: 'Excel, Google Sheets',
    haces: 'Armás las planillas y las fórmulas.',
    queda: 'Hacen casi todo, si sabés armarlas y mantenerlas al día.',
    propia: false,
  },
  {
    herramienta: 'Airtable, Notion',
    haces: 'Diseñás tus propias tablas.',
    queda: 'Muy flexibles; hay que pensar cómo se organiza antes de usarlas.',
    propia: false,
  },
  {
    herramienta: 'Ensambla',
    haces: 'Contás tu negocio una vez.',
    queda: 'Un sistema ya armado y las respuestas de plata, sin diseñar nada.',
    propia: true,
  },
];

export function Diferencias() {
  return (
    <section className="seccion seccion--tono" id="diferencia" aria-labelledby="diferencia-titulo">
      <div className="contenedor">
        <header className="seccion__cabecera revelar">
          <p className="sobretitulo">Qué nos hace distintos</p>
          <h2 id="diferencia-titulo">En qué nos diferenciamos.</h2>
          <p>
            Hay muy buenas herramientas. Cada una pide algo distinto de vos; esto es lo que pide
            cada una.
          </p>
        </header>

        <table className="diferencias revelar">
          <caption>Qué hacés vos y qué te queda con cada herramienta</caption>
          <thead>
            <tr>
              <th scope="col">Herramienta</th>
              <th scope="col">Qué hacés vos</th>
              <th scope="col">Qué te queda</th>
            </tr>
          </thead>
          <tbody>
            {FILAS.map(({ herramienta, haces, queda, propia }) => (
              <tr key={herramienta} className={propia ? 'fila-ensambla' : undefined}>
                <th scope="row">{herramienta}</th>
                <td data-label="Qué hacés vos">{haces}</td>
                <td data-label="Qué te queda">{queda}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
