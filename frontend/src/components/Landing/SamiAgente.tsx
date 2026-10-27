const ANOTACIONES = ['Ventas', 'Gastos', 'Fiados', 'Productos'] as const;

export function SamiAgente() {
  return (
    <section className="ens-section" id="a-tu-manera" aria-labelledby="a-tu-manera-titulo">
      <div className="ens-wrap ens-your-way ens-your-way--centered">
        <div>
          <p className="ens-eyebrow">A tu manera</p>
          <h2 id="a-tu-manera-titulo">Cada negocio tiene su manera de anotar.</h2>
          <p className="ens-lead">Empezá por lo que necesitás hoy. El resto puede esperar.</p>
        </div>
        <figure className="ens-sami-scene">
          <div className="ens-sami-stage">
            <img
              src="/landing/sami-bloques.webp"
              width={1536}
              height={1024}
              loading="lazy"
              decoding="async"
              alt="Sami, la mascota de Ensambla, armando piezas de colores."
            />
            <ul className="ens-tags" aria-label="Ejemplos de qué anotar">
              {ANOTACIONES.map((nombre) => (
                <li key={nombre}>{nombre}</li>
              ))}
            </ul>
          </div>
          <figcaption>
            <strong>Una mano para empezar.</strong>Sami te acompaña al pasar tus anotaciones. Vos
            revisás y decidís qué guardar.
          </figcaption>
        </figure>
      </div>
    </section>
  );
}
