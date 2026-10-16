import { Link } from '@tanstack/react-router';
import { Isotipo } from '@/components/ui';

export function LandingFooter() {
  return (
    <footer className="pie">
      <div className="contenedor pie__grilla">
        <div className="pie__marca">
          <a href="#inicio" className="marca" aria-label="Ensambla, volver al inicio">
            <Isotipo className="marca__iso" />
            <span className="marca__nombre">Ensambla</span>
          </a>
          <p>Contá tu negocio. Ensambla lo arma. Tu sistema, un bloque a la vez.</p>
        </div>
        <nav className="pie__nav" aria-label="Pie de página">
          <div>
            <h3>Producto</h3>
            <a href="#se-arma">Se ensambla solo</a>
            <a href="#desbloquea">Qué desbloquea</a>
            <a href="#diferencia">En qué nos diferenciamos</a>
            <a href="#planes">Planes y precios</a>
          </div>
          <div>
            <h3>Cuenta</h3>
            <Link to="/probar">Probá ahora</Link>
            <Link to="/registro">Crear cuenta</Link>
            <Link to="/login">Ingresar</Link>
          </div>
        </nav>
      </div>
      <div className="contenedor pie__legal">
        <p>© {new Date().getFullYear()} Ensambla · Trabajo Práctico Integrador · UNLaM</p>
        <a href="#inicio" className="arriba">
          Volver arriba ↑
        </a>
      </div>
    </footer>
  );
}
