import { RequierePlan } from '@/components/Plan/PlanGate';
import { Panel } from '@/components/ui';
import { useProductos, useReglas } from '@/hooks/red/useRed';
import { margenSobreCosto } from '@/lib/red/calculos';
import { pesos, plural, porcentaje } from '@/lib/red/formato';

function Dato({ cifra, texto }: { cifra: string; texto: string }) {
  return (
    <li className="flex min-w-0 flex-col gap-1 rounded-lg bg-canvas p-4">
      <p className="m-0 text-h3 leading-tight font-bold break-words text-fg">{cifra}</p>
      <p className="m-0 text-hint text-soft">{texto}</p>
    </li>
  );
}

// «Resumen del día» (doc §2, plan Comercio): cómo viene la semana en plata y en margen, sin tablas.
// Las ventas son las de los últimos 7 días multiplicadas por el precio de hoy: por eso se aclara «estimado».
export function ResumenDelDia() {
  return (
    <div data-recorrido="hoy-resumen">
      <RequierePlan
        funcion="resumen-del-dia"
        beneficio="Mirá cómo viene la semana: cuánto vendés, cuánto ganás y qué producto anda casi al costo."
      >
        <ResumenContenido />
      </RequierePlan>
    </div>
  );
}

function ResumenContenido() {
  const productos = useProductos();
  const reglas = useReglas();

  const ventasSemana = productos.reduce((suma, p) => suma + p.ventasSemana * p.precio, 0);
  const costoVendido = productos.reduce((suma, p) => suma + p.ventasSemana * p.costo, 0);
  // Ponderado por lo que se vende; si no hay ventas, el promedio simple de los productos.
  const margenPromedio =
    costoVendido > 0
      ? ((ventasSemana - costoVendido) / costoVendido) * 100
      : productos.length > 0
        ? productos.reduce((suma, p) => suma + margenSobreCosto(p.costo, p.precio), 0) /
          productos.length
        : 0;
  const bajos = productos.filter((p) => margenSobreCosto(p.costo, p.precio) < reglas.margenMinimo);
  const masAjustado = [...productos].sort(
    (a, b) => margenSobreCosto(a.costo, a.precio) - margenSobreCosto(b.costo, b.precio),
  )[0];

  return (
    <Panel aria-labelledby="hoy-resumen" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 id="hoy-resumen" className="m-0 text-h3">
          Resumen del día
        </h2>
        <p className="m-0 text-hint text-muted">
          Estimado con lo que vendiste esta semana y los precios de hoy.
        </p>
      </div>

      {productos.length === 0 ? (
        <p className="m-0 text-body text-soft">
          Cuando cargues tus productos, acá vas a ver cómo viene la semana.
        </p>
      ) : (
        <>
          <ul className="m-0 grid list-none grid-cols-1 gap-3 p-0 sm:grid-cols-3">
            <Dato
              cifra={`≈ ${pesos(ventasSemana)}`}
              texto={`vendidos esta semana, unos ${pesos(ventasSemana / 7)} por día (estimado)`}
            />
            <Dato
              cifra={porcentaje(margenPromedio)}
              texto="de margen promedio sobre el costo (estimado)"
            />
            <Dato
              cifra={String(bajos.length)}
              texto={
                bajos.length === 0
                  ? `productos con margen bajo: todos pasan tu mínimo de ${porcentaje(reglas.margenMinimo)}`
                  : `${bajos.length === 1 ? 'producto quedó' : 'productos quedaron'} con margen bajo (menos de ${porcentaje(reglas.margenMinimo)})`
              }
            />
          </ul>

          {masAjustado && (
            <p className="m-0 text-body text-soft">
              {margenSobreCosto(masAjustado.costo, masAjustado.precio) < reglas.margenMinimo ? (
                <>
                  El que más cerca del costo se vende es{' '}
                  <strong className="text-fg">{masAjustado.nombre}</strong>:{' '}
                  {masAjustado.precio < masAjustado.costo
                    ? 'lo vendés por debajo de lo que te cuesta'
                    : `te deja ${porcentaje(margenSobreCosto(masAjustado.costo, masAjustado.precio))} sobre lo que te cuesta`}{' '}
                  ({pesos(masAjustado.costo)} de costo contra {pesos(masAjustado.precio)} de
                  precio).
                </>
              ) : (
                <>
                  El producto con menos ganancia es{' '}
                  <strong className="text-fg">{masAjustado.nombre}</strong>:{' '}
                  {porcentaje(margenSobreCosto(masAjustado.costo, masAjustado.precio))} sobre el
                  costo. Ningún producto se vende casi al costo.
                </>
              )}{' '}
              {bajos.length > 1 &&
                `Tenés ${plural(bajos.length, 'producto', 'productos')} para revisar.`}
            </p>
          )}
        </>
      )}
    </Panel>
  );
}
