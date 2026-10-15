import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { MegaphoneIcon, ShoppingCartIcon, TagIcon } from '@phosphor-icons/react';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { Badge, Button, EmptyState, Panel, Toast, buttonClasses } from '@/components/ui';
import {
  useAccionesRed,
  useOfertas,
  usePedido,
  useProductos,
  useProveedores,
} from '@/hooks/red/useRed';
import { fechaCorta, nombreDelDia, pesos, plural, porcentaje } from '@/lib/red/formato';
import type { OfertaProveedor } from '@/types/red';
import { ahorroDeOferta, cantidadDeOferta, descuentoDeOferta, ofertaVigente } from './calculosRed';

// «Ofertas que te sirven»: sólo las de productos que el comercio vende; el resto se muestra
// atenuado y explicado, para que se entienda qué filtra la red por él.
export function OfertasQueSirven() {
  return (
    <RequierePlan
      funcion="ofertas"
      beneficio="Te avisamos cuando un proveedor publica una oferta de algo que vendés, con cuánto ahorrás."
    >
      <OfertasContenido />
    </RequierePlan>
  );
}

function OfertasContenido() {
  const ofertas = useOfertas();
  const productos = useProductos();
  const proveedores = useProveedores();
  const pedido = usePedido();
  const { agregarAlPedido } = useAccionesRed();
  const [ahora] = useState(() => new Date());
  const [aviso, setAviso] = useState<string | null>(null);

  const proveedor = (id: string) => proveedores.find((p) => p.id === id)?.nombre ?? 'Un proveedor';
  const vigentes = ofertas.filter((oferta) => ofertaVigente(oferta, ahora));

  const queSirven = vigentes
    .flatMap((oferta) => {
      const producto = productos.find((p) => p.id === oferta.productoId);
      return producto ? [{ oferta, producto, ahorro: ahorroDeOferta(oferta, producto) }] : [];
    })
    .sort((a, b) => b.ahorro - a.ahorro);
  const idsQueSirven = new Set(queSirven.map((item) => item.oferta.id));
  const otras = vigentes.filter((oferta) => !idsQueSirven.has(oferta.id));
  const ahorroTotal = queSirven.reduce((suma, item) => suma + item.ahorro, 0);

  const agregar = (oferta: OfertaProveedor) => {
    const producto = productos.find((p) => p.id === oferta.productoId);
    if (!producto) return;
    const cantidad = cantidadDeOferta(oferta, producto);
    agregarAlPedido(producto.id, cantidad);
    setAviso(
      `Agregamos ${plural(cantidad, 'unidad', 'unidades')} de ${producto.nombre} a tu pedido.`,
    );
  };

  return (
    <section
      aria-labelledby="red-ofertas"
      data-recorrido="red-ofertas"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1.5">
        <h2 id="red-ofertas" className="m-0 text-h3">
          Ofertas que te sirven
        </h2>
        <p className="m-0 text-body text-soft">
          Los proveedores publican una vez y Ensambla se las muestra sólo a quien vende ese
          producto.
          {queSirven.length > 0 && (
            <>
              {' '}
              Con estas ofertas podrías ahorrar ≈ <strong>{pesos(ahorroTotal)}</strong>.
            </>
          )}
        </p>
      </div>

      {queSirven.length === 0 ? (
        <EmptyState
          icon={<TagIcon aria-hidden size={32} />}
          title="Por ahora no hay ofertas de lo que vendés"
        >
          Cuando un proveedor publique una oferta de uno de tus productos, te avisamos.
        </EmptyState>
      ) : (
        <ul className="m-0 flex list-none flex-col gap-3 p-0">
          {queSirven.map(({ oferta, producto, ahorro }) => {
            const enPedido = pedido.find((item) => item.productoId === producto.id);
            const cantidad = cantidadDeOferta(oferta, producto);
            return (
              <li key={oferta.id}>
                <Panel className="flex flex-col gap-3">
                  <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1">
                    <div className="flex min-w-0 flex-col gap-0.5">
                      <h3 className="m-0 text-body font-bold">{producto.nombre}</h3>
                      <p className="m-0 text-hint text-soft">{proveedor(oferta.proveedorId)}</p>
                    </div>
                    <Badge tone="ok" icon={<TagIcon size={16} weight="fill" />}>
                      {porcentaje(descuentoDeOferta(oferta))} menos
                    </Badge>
                  </div>

                  <p className="m-0 text-body">
                    <span className="text-soft">Costo habitual {pesos(oferta.costoHabitual)}</span>
                    {' → '}
                    <strong>oferta {pesos(oferta.costoOferta)}</strong>
                  </p>
                  <ul className="m-0 flex list-none flex-col gap-1 p-0 text-hint text-soft">
                    <li>
                      Hasta el {nombreDelDia(oferta.vence)} ({fechaCorta(oferta.vence)})
                    </li>
                    {oferta.minimoBultos && (
                      <li>
                        Mínimo {plural(oferta.minimoBultos, 'bulto', 'bultos')}
                        {producto.unidadesPorBulto
                          ? ` (${plural(cantidad, 'unidad', 'unidades')})`
                          : ''}
                      </li>
                    )}
                    <li>
                      Ahorro estimado ≈ <strong className="text-fg">{pesos(ahorro)}</strong> (según
                      lo que vendés por semana)
                    </li>
                  </ul>

                  <div className="flex flex-wrap items-center gap-3">
                    <Button variant="primary" onClick={() => agregar(oferta)}>
                      <ShoppingCartIcon aria-hidden size={22} />
                      <span>
                        Agregar al pedido
                        <span className="sr-only"> de {producto.nombre}</span>
                      </span>
                    </Button>
                    {enPedido && (
                      <Badge tone="ok">
                        En tu pedido: {plural(enPedido.cantidad, 'unidad', 'unidades')}
                      </Badge>
                    )}
                  </div>
                </Panel>
              </li>
            );
          })}
        </ul>
      )}

      {pedido.length > 0 && (
        <p className="m-0">
          <Link to="/hoy" className={buttonClasses({ variant: 'ghost' })}>
            Ver mi pedido en Hoy
          </Link>
        </p>
      )}

      {otras.length > 0 && (
        <div className="flex flex-col gap-3">
          <h3 className="m-0 text-body font-bold">Otras ofertas del barrio que no vendés</h3>
          <p className="m-0 text-hint text-soft">
            No te las avisamos porque no las vendés. Si algún día las sumás, aparecen arriba.
          </p>
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {otras.map((oferta) => (
              <li key={oferta.id}>
                <Panel flat tight className="flex flex-col gap-1 text-soft">
                  <p className="m-0 flex items-start gap-2 text-body font-semibold">
                    <MegaphoneIcon aria-hidden size={22} className="mt-0.5 shrink-0" />
                    {oferta.nombreProducto}
                  </p>
                  <p className="m-0 text-hint">
                    {proveedor(oferta.proveedorId)} · {pesos(oferta.costoHabitual)} →{' '}
                    {pesos(oferta.costoOferta)} ({porcentaje(descuentoDeOferta(oferta))} menos) ·
                    hasta el {nombreDelDia(oferta.vence)}
                  </p>
                </Panel>
              </li>
            ))}
          </ul>
        </div>
      )}

      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </section>
  );
}
