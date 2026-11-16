import { Link } from '@tanstack/react-router';
import { ArrowUpRightIcon } from '@phosphor-icons/react/dist/csr/ArrowUpRight';

export function LlamadaFinal() {
  return (
    <section className="ens-final" id="empezar" aria-labelledby="empezar-titulo">
      <div className="ens-wrap">
        <div className="ens-mini-blocks" aria-hidden="true">
          <i />
          <i />
          <i />
          <i />
        </div>
        <h2 id="empezar-titulo">Organizá las anotaciones de tu negocio en un solo lugar</h2>
        <p>
          Empezá con una venta, un gasto o un fiado para tener la información disponible cuando la
          necesites.
        </p>
        <Link to="/registro" className="ens-primary">
          Armar mi negocio <ArrowUpRightIcon aria-hidden size={20} />
        </Link>
        <Link to="/login" className="ens-final-login">
          Ya tengo cuenta
        </Link>
      </div>
    </section>
  );
}
