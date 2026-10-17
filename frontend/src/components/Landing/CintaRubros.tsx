const RUBROS = [
  'Almacenes',
  'Kioscos',
  'Minimercados',
  'Dietéticas',
  'Verdulerías',
  'Librerías',
  'Perfumerías',
  'Comercios de barrio',
];

export function CintaRubros() {
  return (
    <section className="cinta" aria-label="Para quién es Ensambla">
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
