import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { SlidersHorizontalIcon, TagIcon } from '@phosphor-icons/react';
import { CatalogoPrecios } from '@/components/Precios/CatalogoPrecios';
import { DetalleProducto } from '@/components/Precios/DetalleProducto';
import { Pestanas } from '@/components/Precios/Pestanas';
import { ReglasDePrecio } from '@/components/Precios/ReglasDePrecio';
import { idPanel, idPestana, type AvisoPrecios } from '@/components/Precios/ayudas';
import { PageHeading, Toast } from '@/components/ui';

type VistaPrecios = 'productos' | 'reglas';

const PREFIJO = 'precios';

const PESTANAS = [
  { id: 'productos', etiqueta: 'Mis productos', icono: <TagIcon size={22} /> },
  { id: 'reglas', etiqueta: 'Reglas de precio', icono: <SlidersHorizontalIcon size={22} /> },
] as const;

// «¿Cuánto cobro, cuánto me cuesta y de dónde sale cada costo?»: el catálogo con las ganancias, el
// detalle de cada producto y las reglas con las que Ensambla propone los precios.
export function PreciosPage() {
  const navigate = useNavigate();
  const [vista, setVista] = useState<VistaPrecios>('productos');
  const [productoAbierto, setProductoAbierto] = useState<string | null>(null);
  const [aviso, setAviso] = useState<AvisoPrecios | null>(null);

  function precioGuardado() {
    setProductoAbierto(null);
    setAviso({ mensaje: 'Listo. Falta cambiar la etiqueta en el mostrador', mostrador: true });
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        title="Mis precios"
        lead="Cuánto cobrás, cuánto te cuesta y de dónde sale cada costo."
      />

      <Pestanas
        label="Secciones de Mis precios"
        prefijo={PREFIJO}
        pestanas={[...PESTANAS]}
        activa={vista}
        onCambiar={setVista}
      />

      {/* Los dos paneles quedan montados: así no se pierden los filtros ni las reglas a medio editar. */}
      <div
        role="tabpanel"
        id={idPanel(PREFIJO, 'productos')}
        aria-labelledby={idPestana(PREFIJO, 'productos')}
        hidden={vista !== 'productos'}
      >
        <CatalogoPrecios onAbrir={setProductoAbierto} />
      </div>
      <div
        role="tabpanel"
        id={idPanel(PREFIJO, 'reglas')}
        aria-labelledby={idPestana(PREFIJO, 'reglas')}
        hidden={vista !== 'reglas'}
      >
        <ReglasDePrecio onAviso={setAviso} />
      </div>

      <DetalleProducto
        productoId={productoAbierto}
        onCerrar={() => setProductoAbierto(null)}
        onPrecioGuardado={precioGuardado}
      />

      {aviso && (
        <Toast
          tipo="exito"
          mensaje={aviso.mensaje}
          onClose={() => setAviso(null)}
          accion={
            aviso.mostrador
              ? {
                  etiqueta: 'Ir al mostrador',
                  onClick: () => {
                    setAviso(null);
                    void navigate({ to: '/mostrador' });
                  },
                }
              : undefined
          }
        />
      )}
    </div>
  );
}
