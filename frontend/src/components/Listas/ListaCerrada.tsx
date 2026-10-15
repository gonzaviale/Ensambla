import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, ProhibitIcon } from '@phosphor-icons/react';
import { Badge, PageHeading, Panel, buttonClasses } from '@/components/ui';
import { useProductos, useProveedor } from '@/hooks/red/useRed';
import { haceCuanto } from '@/lib/fechas';
import { costo, fechaCorta, fechaLarga, plural, porcentaje } from '@/lib/red/formato';
import type { ListaProveedor } from '@/types/red';
import { NOMBRE_ORIGEN_CORTO } from './analisis';

// Vista de sólo lectura de una lista ya aplicada o descartada, con la trazabilidad de los costos.
export function ListaCerrada({ lista }: { lista: ListaProveedor }) {
  const proveedor = useProveedor(lista.proveedorId);
  const productos = useProductos();
  const nombre = proveedor?.nombre ?? 'Tu proveedor';
  const aplicada = lista.estado === 'aplicada';
  const deEstaLista = productos.filter((p) => p.costoListaId === lista.id);

  return (
    <div className="flex flex-col gap-6">
      <PageHeading
        kicker={aplicada ? 'Lista aplicada' : 'Lista descartada'}
        title={`Lista de ${nombre}`}
        lead={`Llegó ${haceCuanto(lista.recibidaEl)} por ${NOMBRE_ORIGEN_CORTO[lista.origen]}.`}
        actions={
          <Link to="/listas" className={buttonClasses({ variant: 'solid' })}>
            Volver a Listas
          </Link>
        }
      />

      <Panel className="flex flex-col gap-3">
        {aplicada ? (
          <>
            <Badge
              tone="ok"
              icon={<CheckCircleIcon size={16} weight="fill" />}
              className="self-start"
            >
              Aplicada{lista.aplicadaEl ? ` el ${fechaLarga(lista.aplicadaEl)}` : ''}
            </Badge>
            <p className="m-0 text-body">
              {lista.resumen
                ? `Se actualizaron ${plural(lista.resumen.productos, 'producto', 'productos')}, con una suba de ${porcentaje(lista.resumen.aumentoPromedio, { decimales: 1 })} en promedio.`
                : 'Los precios de esta lista ya se aplicaron.'}
            </p>
          </>
        ) : (
          <>
            <Badge tone="neutral" icon={<ProhibitIcon size={16} />} className="self-start">
              Descartada
            </Badge>
            <p className="m-0 text-body">
              Descartaste esta lista: no se cambió ningún costo ni ningún precio.
            </p>
          </>
        )}
      </Panel>

      {aplicada && (
        <section aria-labelledby="trazabilidad" className="flex flex-col gap-3">
          <h2 id="trazabilidad" className="m-0 text-h3">
            {deEstaLista.length === 0
              ? 'Qué costos salen de esta lista'
              : deEstaLista.length === 1
                ? 'El costo de este producto sale de esta lista'
                : `Los costos de estos ${deEstaLista.length} productos salen de esta lista`}
          </h2>
          {deEstaLista.length === 0 ? (
            <p className="m-0 text-body text-soft">
              Hoy ningún costo sale de esta lista: se actualizaron con listas más nuevas.
            </p>
          ) : (
            <ul className="m-0 flex list-none flex-col gap-2 p-0">
              {deEstaLista.map((producto) => (
                <li key={producto.id}>
                  <Panel tight className="flex flex-wrap items-baseline justify-between gap-x-4">
                    <span className="text-body font-semibold break-words">{producto.nombre}</span>
                    <span className="text-body">
                      {costo(producto.costo)}{' '}
                      <span className="text-hint text-soft">
                        desde el {fechaCorta(producto.costoDesde)}
                      </span>
                    </span>
                  </Panel>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}
