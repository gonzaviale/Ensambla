import { Link } from '@tanstack/react-router';
import { ArrowDownIcon } from '@phosphor-icons/react/dist/csr/ArrowDown';
import { ArrowUpRightIcon } from '@phosphor-icons/react/dist/csr/ArrowUpRight';

export function Hero() {
  return (
    <section className="ens-wrap ens-hero" id="inicio" aria-labelledby="inicio-titulo">
      <div className="ens-hero-copy">
        <p className="ens-eyebrow">Para comercios de barrio como el tuyo</p>
        <h1 id="inicio-titulo">
          Tu cuaderno de siempre. <br />
          Tus cuentas, <br />
          <em>más claras.</em>
        </h1>
        <p>
          Ensambla es un cuaderno digital para anotar ventas, gastos y fiados, y encontrar lo que
          necesitás sin buscar hoja por hoja.
        </p>
        <div className="ens-actions">
          <Link to="/registro" className="ens-primary">
            Empezar mi cuaderno <ArrowUpRightIcon aria-hidden size={20} />
          </Link>
          <a className="ens-link" href="#como-funciona">
            Ver un ejemplo <ArrowDownIcon aria-hidden size={18} />
          </a>
        </div>
        <p className="ens-note">Empezá con lo que ya anotás.</p>
      </div>
      <figure className="ens-hero-photo">
        <img
          src="/landing/almacen-cuaderno.jpg"
          width={1254}
          height={1254}
          fetchPriority="high"
          decoding="async"
          alt="Escena ilustrativa de una comerciante anotando en un cuaderno detrás del mostrador de un almacén de barrio."
        />
        <div className="ens-sticker" aria-hidden="true">
          El negocio no espera.
          <br />
          Tus anotaciones tampoco.
        </div>
        <figcaption className="ens-photo-label">Imagen ilustrativa.</figcaption>
      </figure>
    </section>
  );
}
