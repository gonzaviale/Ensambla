const RUBROS = [
  'Almacenes',
  'Kioscos',
  'Despensas',
  'Autoservicios',
  'Ferreterías',
  'Panaderías',
  'Dietéticas',
  'Librerías',
];

export function CintaRubros() {
  return (
    <section className="cinta" aria-label="Para comercios de barrio como los tuyos">
      <div className="cinta__pista">
        <ul className="cinta__lista">
          {RUBROS.map((rubro) => (
            <li key={rubro}>{rubro}</li>
          ))}
        </ul>
        {/* Segunda copia para que el desplazamiento sea continuo */}
        <ul className="cinta__lista" aria-hidden="true">
          {RUBROS.map((rubro) => (
            <li key={rubro}>{rubro}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}
