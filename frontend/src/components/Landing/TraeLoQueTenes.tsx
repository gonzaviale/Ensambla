const ATAJOS = ['Una foto de tu cuaderno', 'Una lista', 'Una planilla'];

// Traer historia es un atajo opcional: el sistema funciona igual sin nada de esto.
export function TraeLoQueTenes() {
  return (
    <section className="seccion" id="trae" aria-labelledby="trae-titulo">
      <div className="contenedor">
        <div className="trae revelar">
          <div>
            <h2 id="trae-titulo">Traé lo que ya tenés, si querés.</h2>
            <p>
              Si ya anotás en un cuaderno, en una lista o en una planilla, podés traerlo y Ensambla
              lo acomoda. Es un atajo: si preferís empezar de cero, tampoco hace falta.
            </p>
          </div>
          <ul className="trae__atajos">
            {ATAJOS.map((atajo) => (
              <li key={atajo}>{atajo}</li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
