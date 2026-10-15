import { CheckCircleIcon, InfoIcon, ShieldCheckIcon, WarningIcon } from '@phosphor-icons/react';
import { Button, Panel } from '@/components/ui';
import { pesos, plural } from '@/lib/red/formato';

type PasoConfirmarProps = {
  /** Productos que van a tener un costo nuevo en el sistema. */
  costos: number;
  /** De esos, cuántos cambian de precio de venta. */
  preciosQueCambian: number;
  /** Productos cuyo precio se deja como está. */
  preciosQueSeQuedan: number;
  /** Líneas que no se van a aplicar (ignoradas o sin resolver). */
  sinAplicar: number;
  protegido: number;
  onDescartar: () => void;
};

// Paso 4: lo que va a pasar, dicho en llano, antes de tocar nada.
export function PasoConfirmar({
  costos,
  preciosQueCambian,
  preciosQueSeQuedan,
  sinAplicar,
  protegido,
  onDescartar,
}: PasoConfirmarProps) {
  return (
    <div className="flex flex-col gap-5">
      <Panel className="flex flex-col gap-3">
        <p className="m-0 text-h2 font-extrabold">
          Vas a cambiar {plural(preciosQueCambian, 'precio', 'precios')} en tu sistema
        </p>
        <ul className="m-0 flex list-none flex-col gap-2 p-0 text-body">
          <li className="flex items-start gap-2">
            <CheckCircleIcon
              aria-hidden
              size={24}
              weight="fill"
              className="mt-0.5 shrink-0 text-ok"
            />
            <span>
              {plural(costos, 'producto con costo nuevo', 'productos con costo nuevo')}
              {preciosQueSeQuedan > 0 &&
                `, de los cuales ${plural(preciosQueSeQuedan, 'mantiene', 'mantienen')} tu precio actual`}
              .
            </span>
          </li>
          {protegido > 0 && (
            <li className="flex items-start gap-2">
              <ShieldCheckIcon
                aria-hidden
                size={24}
                weight="fill"
                className="mt-0.5 shrink-0 text-ok"
              />
              <span>
                Protegés <strong>≈ {pesos(protegido)} por mes</strong>.
              </span>
            </li>
          )}
          {sinAplicar > 0 && (
            <li className="flex items-start gap-2">
              <WarningIcon
                aria-hidden
                size={24}
                weight="fill"
                className="mt-0.5 shrink-0 text-amber-700"
              />
              <span>
                {plural(sinAplicar, 'línea queda', 'líneas quedan')} sin aplicar porque no se
                resolvieron o las ignoraste.
              </span>
            </li>
          )}
        </ul>
      </Panel>

      <Panel flat tight className="flex items-start gap-3">
        <InfoIcon aria-hidden size={26} weight="fill" className="mt-0.5 shrink-0 text-soft" />
        <p className="m-0 text-body">
          Nada cambia en el local hasta que lo hagas vos en el mostrador. Después de aplicar te
          mostramos una lista con las etiquetas para cambiar.
        </p>
      </Panel>

      <div>
        <Button variant="ghost" onClick={onDescartar}>
          Descartar esta lista
        </Button>
      </div>
    </div>
  );
}
