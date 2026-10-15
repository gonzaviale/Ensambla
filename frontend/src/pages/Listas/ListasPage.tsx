import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, ProhibitIcon, TrayIcon, UploadSimpleIcon } from '@phosphor-icons/react';
import { CargarListaSheet } from '@/components/Listas/CargarListaSheet';
import { NOMBRE_ORIGEN_CORTO } from '@/components/Listas/analisis';
import { TarjetaLista } from '@/components/Listas/TarjetaLista';
import { CandadoPlan } from '@/components/Plan/PlanGate';
import { Badge, Button, EmptyState, PageHeading, Panel } from '@/components/ui';
import { useListas, usePlanActual, useProveedores } from '@/hooks/red/useRed';
import { haceCuanto } from '@/lib/fechas';
import { fechaLarga, plural, porcentaje } from '@/lib/red/formato';
import type { ListaProveedor } from '@/types/red';

const masNuevaPrimero = (a: ListaProveedor, b: ListaProveedor) =>
  new Date(b.recibidaEl).getTime() - new Date(a.recibidaEl).getTime();

// Listas de proveedores: las que esperan revisión arriba, el historial (con su trazabilidad) abajo.
export function ListasPage() {
  const listas = useListas();
  const proveedores = useProveedores();
  const plan = usePlanActual();
  const [cargando, setCargando] = useState(false);

  const porRevisar = listas.filter((l) => l.estado === 'por-revisar').sort(masNuevaPrimero);
  const historial = listas.filter((l) => l.estado !== 'por-revisar').sort(masNuevaPrimero);
  const nombreDe = (proveedorId: string) =>
    proveedores.find((p) => p.id === proveedorId)?.nombre ?? 'Tu proveedor';
  const limite = plan.limiteListas;
  const usadas = limite === null ? plan.listasUsadasMes : Math.min(plan.listasUsadasMes, limite);

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Tus proveedores"
        title="Listas de tus proveedores"
        lead="Cuando llega una lista, Ensambla te muestra qué precios revisar y te ayuda a actualizarlos."
        actions={
          plan.sinListasEsteMes ? undefined : (
            <Button
              variant="solid"
              data-recorrido="listas-cargar"
              onClick={() => setCargando(true)}
            >
              <UploadSimpleIcon aria-hidden size={22} weight="bold" />
              Cargar una lista
            </Button>
          )
        }
      />

      {limite !== null && (
        <p className="m-0 text-body text-soft">
          <strong className="text-fg">
            {usadas} de {limite} listas
          </strong>{' '}
          este mes en el plan {plan.definicion.nombre}.
        </p>
      )}

      {plan.sinListasEsteMes && limite !== null && (
        <CandadoPlan
          funcion="listas-ilimitadas"
          beneficio={`Usaste ${usadas} de ${limite} listas de este mes. Con más listas por mes no te quedás sin revisar los precios.`}
        />
      )}

      <section
        aria-labelledby="por-revisar"
        data-recorrido="listas-por-revisar"
        className="flex flex-col gap-4"
      >
        <h2 id="por-revisar" className="m-0 text-h2">
          Por revisar
        </h2>
        {porRevisar.length === 0 ? (
          <EmptyState
            icon={<TrayIcon aria-hidden size={32} />}
            title="No tenés listas para revisar"
          >
            <p className="m-0">
              Cuando tu proveedor te mande una lista, cargala acá y en un minuto ves qué precios
              cambiar. Mientras tanto, tus precios están al día.
            </p>
          </EmptyState>
        ) : (
          <ul className="m-0 grid list-none gap-4 p-0 md:grid-cols-2">
            {porRevisar.map((lista) => (
              <li key={lista.id} className="flex">
                <TarjetaLista lista={lista} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-labelledby="ya-aplicadas" className="flex flex-col gap-4">
        <h2 id="ya-aplicadas" className="m-0 text-h2">
          Ya aplicadas
        </h2>
        {historial.length === 0 ? (
          <p className="m-0 text-body text-soft">
            Todavía no aplicaste ninguna lista. Acá va a quedar el historial.
          </p>
        ) : (
          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {historial.map((lista) => (
              <li key={lista.id}>
                <Panel tight className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex min-w-0 flex-col gap-0.5">
                    <p className="m-0 text-body font-semibold break-words">
                      {nombreDe(lista.proveedorId)}
                    </p>
                    <p className="m-0 text-hint text-soft">
                      {lista.estado === 'aplicada' && lista.aplicadaEl
                        ? `Aplicada el ${fechaLarga(lista.aplicadaEl)}`
                        : `Llegó ${haceCuanto(lista.recibidaEl)}`}{' '}
                      · por {NOMBRE_ORIGEN_CORTO[lista.origen]}
                      {lista.resumen &&
                        ` · ${plural(lista.resumen.productos, 'producto', 'productos')}, ${porcentaje(lista.resumen.aumentoPromedio, { decimales: 1 })} en promedio`}
                    </p>
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    {lista.estado === 'aplicada' ? (
                      <Badge tone="ok" icon={<CheckCircleIcon size={16} weight="fill" />}>
                        Aplicada
                      </Badge>
                    ) : (
                      <Badge tone="neutral" icon={<ProhibitIcon size={16} />}>
                        Descartada
                      </Badge>
                    )}
                    <Link
                      to="/listas/$listaId"
                      params={{ listaId: lista.id }}
                      aria-label={`Ver la lista de ${nombreDe(lista.proveedorId)}`}
                      className="text-body font-semibold"
                    >
                      Ver lista
                    </Link>
                  </div>
                </Panel>
              </li>
            ))}
          </ul>
        )}
      </section>

      <CargarListaSheet open={cargando} onClose={() => setCargando(false)} />
    </div>
  );
}
