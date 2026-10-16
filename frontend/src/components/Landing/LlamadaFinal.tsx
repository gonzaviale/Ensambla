import { Link } from '@tanstack/react-router';

export function LlamadaFinal() {
  return (
    <section className="seccion seccion--final" id="empezar" aria-labelledby="empezar-titulo">
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
            <h2 id="empezar-titulo">Tu sistema empieza con una frase.</h2>
            <p>
              Probalo ahora, sin cuenta, o creá la tuya gratis. Contá tu negocio y Ensambla lo arma.
            </p>
          </div>
          <div className="final__acciones">
            <Link to="/probar" className="boton boton--primario boton--grande">
              Probá ahora
            </Link>
            <Link to="/registro" className="boton boton--claro">
              Crear mi sistema gratis
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
