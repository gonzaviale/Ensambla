import { Link, Outlet } from '@tanstack/react-router';
import { CabeceraComun } from '@/components/sistema/CabeceraComun';

export function ZonaLayout() {
  return (
    <>
      <CabeceraComun active="zone" />
      <div className="zona-modo-wrap">
        <nav className="zona-modo" aria-label="Secciones de Zona">
          <Link to="/zona" activeOptions={{ exact: true }} activeProps={{ className: 'active' }}>
            Mi zona actual
          </Link>
          <Link to="/zona/donde-abrir" activeProps={{ className: 'active' }}>
            Dónde abrir
          </Link>
        </nav>
      </div>
      <Outlet />
    </>
  );
}
