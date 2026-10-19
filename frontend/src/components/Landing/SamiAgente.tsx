const ANOTACIONES = ['Ventas', 'Gastos', 'Fiados', 'Productos'] as const;

export function SamiAgente() {
  return (
    <section className="ens-section" id="a-tu-manera" aria-labelledby="a-tu-manera-titulo">
      <div className="ens-wrap ens-your-way">
        <div>
          <p className="ens-eyebrow">A tu manera</p>
          <h2 id="a-tu-manera-titulo">Cada negocio tiene su manera de anotar.</h2>
          <p className="ens-lead">
            Empezá por lo que más necesitás ordenar. Después podés sumar otras anotaciones, a medida
            que las uses.
          </p>
          <ul className="ens-tags" aria-label="Ejemplos de qué anotar">
            {ANOTACIONES.map((nombre) => (
              <li key={nombre}>{nombre}</li>
            ))}
          </ul>
        </div>
        <figure className="ens-sami">
          <img
            src="/landing/sami-bloques.webp"
            width={1536}
            height={1024}
            loading="lazy"
            decoding="async"
            alt="Sami, la mascota de Ensambla, armando piezas de colores."
          />
          <figcaption>
            <strong>Una mano para empezar.</strong>Sami te acompaña al pasar tus anotaciones. Vos
            revisás y decidís qué guardar.
          </figcaption>
        </figure>
      </div>
    </section>
  );
}
