import { Link } from '@tanstack/react-router';

export function LlamadaFinal() {
  return (
    <section className="seccion seccion--final" id="empezar">
      <div className="contenedor">
        <div className="final revelar">
          <div className="final__bloques" aria-hidden="true">
            <span />
            <span />
            <span />
            <span />
            <span />
          </div>
          <div className="final__texto">
            <h2>Tu primer bloque te está esperando.</h2>
            <p>
              Creá tu cuenta gratis, contale a Sami cómo trabajás y empezá a recibir avisos para
              decidir mejor.
            </p>
          </div>
          <div className="final__acciones">
            <Link to="/registro" className="boton boton--primario boton--grande">
              Empezar gratis
            </Link>
            <Link to="/login" className="boton boton--claro">
              Ya tengo cuenta
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
