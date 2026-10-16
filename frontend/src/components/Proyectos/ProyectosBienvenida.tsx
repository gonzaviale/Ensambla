import { useUsuario } from '@/hooks/auth/queries';
import { useTituloPagina } from '@/hooks/useTituloPagina';
import { SistemaIlustracion } from './SistemaIlustracion';

export function ProyectosBienvenida() {
  const usuario = useUsuario();
  useTituloPagina('¿Por dónde querés empezar?');

  return (
    <section className="proyectos-hero" aria-labelledby="inicio-titulo">
      <div className="proyectos-hero-copy">
        <span className="proyectos-eyebrow">Tu manera de trabajar tiene lugar acá</span>
        <p className="proyectos-hello">Hola, {usuario.nombre}.</p>
        <h1 id="inicio-titulo">¿Por dónde querés empezar?</h1>
        <p className="proyectos-hero-description">
          Vos traés la idea.
          <br />
          <strong>Juntos le damos forma.</strong>
        </p>
        <p className="proyectos-hero-detail">
          Pasá lo que tenés en el cuaderno a un sistema para tu negocio. Organizá tus productos,
          pedidos o clientes con piezas que podés hacer tuyas.
        </p>
        <div className="proyectos-sami-note">
          <img src="/mockup/sami-armando-sin-fondo.png" alt="" />
          <div>
            <strong>Cada pieza cuenta.</strong>
            <span>Empezá con una foto, una base o tus propias tablas.</span>
          </div>
        </div>
        <div className="proyectos-brand-pieces" aria-hidden="true">
          <i />
          <i />
          <i />
          <i />
          <i />
          <span>Tu trabajo, a tu manera.</span>
        </div>
      </div>
      <SistemaIlustracion />
    </section>
  );
}
