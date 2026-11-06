import type { ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

export function PanelProveedor({
  titulo,
  detalle,
  icono,
  children,
  accion,
}: {
  titulo: string;
  detalle?: string;
  icono: NombreDeIcono;
  children: ReactNode;
  accion?: ReactNode;
}) {
  return (
    <section className="prov-panel">
      <header className="prov-panel-cabeza">
        <div className="prov-titulo-icono">
          <span>
            <Icon name={icono} />
          </span>
          <div>
            <h2>{titulo}</h2>
            {detalle && <p>{detalle}</p>}
          </div>
        </div>
        {accion}
      </header>
      {children}
    </section>
  );
}
