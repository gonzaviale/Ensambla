import { Link } from '@tanstack/react-router';

export function LlamadaFinal() {
  return (
    <section className="seccion seccion--final" id="empezar">
      <div className="contenedor final revelar">
        <div className="final__bloques" aria-hidden="true">
          <span />
          <span />
          <span />
          <span />
          <span />
        </div>
        <h2>Dejá el cuaderno para otra cosa.</h2>
        <p>
          Creá tu cuenta gratis, contale a Sami cómo trabajás y mirá tu negocio ordenado desde el
          primer día.
        </p>
        <div className="final__acciones">
          <Link to="/registro" className="boton boton--primario boton--grande">
            Empezar gratis
          </Link>
          <Link to="/login" className="boton boton--contorno boton--grande">
            Ya tengo cuenta
          </Link>
        </div>
      </div>
    </section>
  );
}
