import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CheckCircleIcon, ScalesIcon, ShoppingCartIcon } from '@phosphor-icons/react';
import { RequierePlan } from '@/components/Plan/PlanGate';
import { Badge, Button, EmptyState, Panel, Toast, buttonClasses } from '@/components/ui';
import { useAccionesRed, usePedido, useProductos, useProveedores } from '@/hooks/red/useRed';
import { cantidadSugeridaDePedido, variacionPct } from '@/lib/red/calculos';
import { pesos, plural, porcentaje } from '@/lib/red/formato';
import { ahorroPorMes, mejorAlternativa } from './calculosRed';

// «Comparar proveedores»: el costo de hoy contra la mejor alternativa de la red, por producto.
export function CompararProveedores() {
  return (
    <RequierePlan
      funcion="comparar-proveedores"
      beneficio="Te mostramos si otro proveedor de la red tiene el mismo producto más barato y cuánto ahorrarías por mes."
    >
      <CompararContenido />
    </RequierePlan>
  );
}

function CompararContenido() {
  const productos = useProductos();
  const proveedores = useProveedores();
  const pedido = usePedido();
  const { agregarAlPedido } = useAccionesRed();
  const [aviso, setAviso] = useState<string | null>(null);

  const proveedor = (id: string) =>
    proveedores.find((p) => p.id === id)?.nombre ?? 'Otro proveedor';

  const filas = productos
    .flatMap((producto) => {
      const mejor = mejorAlternativa(producto);
      if (!mejor) return [];
      return [
        {
          producto,
          mejor,
          diferencia: producto.costo - mejor.costo,
          porcentaje: -variacionPct(producto.costo, mejor.costo),
          ahorroMes: ahorroPorMes(producto, mejor.costo),
        },
      ];
    })
    .sort((a, b) => b.ahorroMes - a.ahorroMes);
  const total = filas.reduce((suma, fila) => suma + fila.ahorroMes, 0);

  const pedir = (productoId: string, proveedorNombre: string) => {
    const producto = productos.find((p) => p.id === productoId);
    if (!producto) return;
    const cantidad = cantidadSugeridaDePedido(producto);
    agregarAlPedido(productoId, cantidad);
    setAviso(
      `Agregamos ${plural(cantidad, 'unidad', 'unidades')} de ${producto.nombre} al pedido, para pedirle a ${proveedorNombre}.`,
    );
  };

  return (
    <section
      aria-labelledby="red-comparar"
      data-recorrido="red-comparar"
      className="flex flex-col gap-4"
    >
      <div className="flex flex-col gap-1.5">
        <h2 id="red-comparar" className="m-0 text-h3">
          Comparar proveedores
        </h2>
        <p className="m-0 text-body text-soft">
          Para los productos que otro proveedor de la red también tiene, mirá cuánto cambia el
          costo.
        </p>
      </div>

      {filas.length === 0 ? (
        <EmptyState
          icon={<ScalesIcon aria-hidden size={32} />}
          title="Todavía no hay otros proveedores para comparar"
        >
          Cuando otro proveedor de la red tenga alguno de tus productos, lo comparamos acá.
        </EmptyState>
      ) : (
        <>
          <Panel flat className="flex flex-col gap-1">
            {total > 0 ? (
              <p className="m-0 text-body">
                Podrías ahorrar ≈ <strong className="text-h3">{pesos(total)}</strong> por mes.
              </p>
            ) : (
              <p className="m-0 text-body font-semibold">
                Hoy ya comprás al mejor costo de la red.
              </p>
            )}
            <p className="m-0 text-hint text-soft">
              Cuenta la diferencia por unidad, por lo que vendés en 4 semanas.
            </p>
          </Panel>

          <ul className="m-0 flex list-none flex-col gap-3 p-0">
            {filas.map(({ producto, mejor, diferencia, porcentaje: pct, ahorroMes }) => {
              const conviene = diferencia > 0;
              const nombreMejor = proveedor(mejor.proveedorId);
              const enPedido = pedido.some((item) => item.productoId === producto.id);
              return (
                <li key={producto.id}>
                  <Panel className="flex flex-col gap-3">
                    <div className="flex flex-wrap items-start justify-between gap-x-4 gap-y-1">
                      <h3 className="m-0 min-w-0 text-body font-bold">{producto.nombre}</h3>
                      {conviene ? (
                        <Badge tone="ok">
                          {pesos(diferencia)} menos ({porcentaje(pct, { decimales: 1 })})
                        </Badge>
                      ) : (
                        <Badge tone="neutral" icon={<CheckCircleIcon size={16} weight="fill" />}>
                          Tu proveedor ya es el más barato
                        </Badge>
                      )}
                    </div>

                    <dl className="m-0 grid gap-3 sm:grid-cols-2">
                      <div className="flex flex-col gap-0.5">
                        <dt className="text-hint text-soft">Hoy le comprás a</dt>
                        <dd className="m-0 text-body">
                          <strong>{proveedor(producto.proveedorId)}</strong> a{' '}
                          {pesos(producto.costo)}
                        </dd>
                      </div>
                      <div className="flex flex-col gap-0.5">
                        <dt className="text-hint text-soft">
                          {conviene ? 'Mejor opción de la red' : 'Otra opción de la red'}
                        </dt>
                        <dd className="m-0 text-body">
                          <strong>{nombreMejor}</strong> a {pesos(mejor.costo)}
                        </dd>
                      </div>
                    </dl>

                    {conviene && (
                      <p className="m-0 text-hint text-soft">
                        Ahorro estimado ≈ <strong className="text-fg">{pesos(ahorroMes)}</strong>{' '}
                        por mes.
                      </p>
                    )}

                    <div className="flex flex-wrap items-center gap-3">
                      {conviene ? (
                        <Button variant="primary" onClick={() => pedir(producto.id, nombreMejor)}>
                          <ShoppingCartIcon aria-hidden size={22} />
                          <span>
                            Pedir a {nombreMejor}
                            <span className="sr-only"> {producto.nombre}</span>
                          </span>
                        </Button>
                      ) : (
                        <Link to="/precios" className={buttonClasses({ variant: 'secondary' })}>
                          Ver mis precios
                          <span className="sr-only"> de {producto.nombre}</span>
                        </Link>
                      )}
                      {enPedido && <Badge tone="ok">Ya está en tu pedido</Badge>}
                    </div>
                  </Panel>
                </li>
              );
            })}
          </ul>

          <p className="m-0 text-hint text-soft">
            Ordenamos por lo que ahorrás, no por quién pague. En Ensambla ningún proveedor puede
            pagar para que no veas que otro vende más barato.
          </p>
        </>
      )}

      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </section>
  );
}
