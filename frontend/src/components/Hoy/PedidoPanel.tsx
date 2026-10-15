import { useState } from 'react';
import {
  MinusIcon,
  PlusIcon,
  ShareNetworkIcon,
  ShoppingCartIcon,
  TrashIcon,
} from '@phosphor-icons/react';
import { Button, ConfirmDialog, Panel, buttonClasses } from '@/components/ui';
import {
  useAccionesRed,
  useNegocio,
  usePedido,
  useProductos,
  useProveedores,
} from '@/hooks/red/useRed';
import { pesos, plural } from '@/lib/red/formato';
import type { MensajeHoy } from './avisoMeta';
import { agruparPedido, textoPedido, type LineaPedido } from './formatoHoy';

type PedidoPanelProps = {
  avisar: (mensaje: MensajeHoy) => void;
  /** Mueve el foco al título de la sección cuando una fila desaparece. */
  alQuitar: () => void;
};

function CantidadEditable({ linea }: { linea: LineaPedido }) {
  const { agregarAlPedido } = useAccionesRed();
  const { producto, cantidad } = linea;
  // Mientras se escribe se conserva el texto (puede quedar vacío un instante); al salir se vuelve al valor guardado.
  const [texto, setTexto] = useState<string | null>(null);

  function cambiar(valor: number) {
    if (Number.isInteger(valor) && valor > 0) agregarAlPedido(producto.id, valor);
  }

  return (
    <div className="flex items-center gap-1">
      <Button
        iconOnly
        variant="secondary"
        aria-label={`Una unidad menos de ${producto.nombre}`}
        disabled={cantidad <= 1}
        onClick={() => cambiar(cantidad - 1)}
      >
        <MinusIcon aria-hidden size={20} weight="bold" />
      </Button>
      <input
        type="number"
        inputMode="numeric"
        min={1}
        step={1}
        aria-label={`Cantidad de ${producto.nombre}`}
        value={texto ?? String(cantidad)}
        onFocus={(event) => event.currentTarget.select()}
        onChange={(event) => {
          setTexto(event.target.value);
          cambiar(Number(event.target.value));
        }}
        onBlur={() => setTexto(null)}
        className="min-h-12 w-20 rounded-md border border-fg/55 bg-surface px-2 text-center text-body text-fg hover:border-fg"
      />
      <Button
        iconOnly
        variant="secondary"
        aria-label={`Una unidad más de ${producto.nombre}`}
        onClick={() => cambiar(cantidad + 1)}
      >
        <PlusIcon aria-hidden size={20} weight="bold" />
      </Button>
    </div>
  );
}

// «Tu pedido»: lo que se fue sumando desde los avisos, agrupado por proveedor (a cada uno se le compra
// por separado) y listo para compartir por WhatsApp.
export function PedidoPanel({ avisar, alQuitar }: PedidoPanelProps) {
  const pedido = usePedido();
  const productos = useProductos();
  const proveedores = useProveedores();
  const negocio = useNegocio();
  const { agregarAlPedido, quitarDelPedido, vaciarPedido } = useAccionesRed();
  const [confirmandoVaciar, setConfirmandoVaciar] = useState(false);

  const grupos = agruparPedido(pedido, productos, proveedores);
  const cantidadProductos = grupos.reduce((suma, g) => suma + g.lineas.length, 0);
  const total = grupos.reduce((suma, g) => suma + g.costoEstimado, 0);
  const texto = textoPedido(negocio.nombre, grupos);
  const enlaceWhatsApp = `https://wa.me/?text=${encodeURIComponent(texto)}`;

  function compartir() {
    const portapapeles = typeof navigator !== 'undefined' ? navigator.clipboard : undefined;
    if (portapapeles?.writeText) {
      void portapapeles.writeText(texto).catch(() => undefined);
      avisar({ tipo: 'exito', mensaje: 'Copiamos tu pedido y abrimos WhatsApp para mandarlo.' });
    } else {
      avisar({ tipo: 'exito', mensaje: 'Abrimos WhatsApp con tu pedido listo para mandar.' });
    }
  }

  function quitar(linea: LineaPedido) {
    quitarDelPedido(linea.producto.id);
    alQuitar();
    avisar({
      tipo: 'exito',
      mensaje: `Sacamos ${linea.producto.nombre} del pedido.`,
      accion: {
        etiqueta: 'Deshacer',
        onClick: () => agregarAlPedido(linea.producto.id, linea.cantidad),
      },
    });
  }

  return (
    <>
      {cantidadProductos === 0 ? (
        <Panel flat className="flex items-start gap-3">
          <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-sand-100 text-fg">
            <ShoppingCartIcon aria-hidden size={26} />
          </span>
          <div className="flex min-w-0 flex-col gap-1">
            <p className="m-0 text-body font-semibold">Todavía no armaste un pedido</p>
            <p className="m-0 text-body text-soft">
              Cuando un aviso diga que algo se termina, tocá «Agregar al pedido» y aparece acá,
              ordenado por proveedor y listo para mandar.
            </p>
          </div>
        </Panel>
      ) : (
        <Panel className="flex flex-col gap-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="m-0 text-body text-soft">
              {plural(cantidadProductos, 'producto', 'productos')} de{' '}
              {plural(grupos.length, 'proveedor', 'proveedores')}. Costo estimado:{' '}
              <strong className="text-fg">{pesos(total)}</strong>.
            </p>
            <div className="flex flex-wrap gap-2">
              <a
                href={enlaceWhatsApp}
                target="_blank"
                rel="noopener noreferrer"
                onClick={compartir}
                className={buttonClasses({ variant: 'primary' })}
              >
                <ShareNetworkIcon aria-hidden size={22} />
                Compartir pedido
                <span className="sr-only"> (se abre en otra pestaña)</span>
              </a>
              <Button variant="danger" onClick={() => setConfirmandoVaciar(true)}>
                Vaciar
              </Button>
            </div>
          </div>

          {grupos.map((grupo) => (
            <div key={grupo.proveedorId} className="flex flex-col gap-2">
              <h3 className="m-0 flex flex-wrap items-baseline justify-between gap-x-3 text-body">
                <span>{grupo.proveedor}</span>
                <span className="text-hint font-semibold text-muted">
                  ≈ {pesos(grupo.costoEstimado)}
                </span>
              </h3>
              <ul className="m-0 flex list-none flex-col divide-y divide-line p-0">
                {grupo.lineas.map((linea) => (
                  <li
                    key={linea.producto.id}
                    className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 py-3"
                  >
                    <div className="min-w-0 flex-1 basis-48">
                      <p className="m-0 text-body font-semibold break-words">
                        {linea.producto.nombre}
                      </p>
                      {linea.producto.unidadesPorBulto && (
                        <p className="m-0 text-hint text-muted">
                          Viene en bultos de {linea.producto.unidadesPorBulto}
                        </p>
                      )}
                    </div>
                    <div className="flex flex-wrap items-center gap-1">
                      <CantidadEditable linea={linea} />
                      <Button
                        iconOnly
                        variant="ghost"
                        aria-label={`Quitar ${linea.producto.nombre} del pedido`}
                        onClick={() => quitar(linea)}
                      >
                        <TrashIcon aria-hidden size={22} />
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </Panel>
      )}

      <ConfirmDialog
        open={confirmandoVaciar}
        titulo="¿Vaciar tu pedido?"
        confirmar="Sí, vaciar el pedido"
        conservar="No, dejarlo como está"
        onConservar={() => setConfirmandoVaciar(false)}
        onConfirmar={() => {
          vaciarPedido();
          setConfirmandoVaciar(false);
          alQuitar();
          avisar({ tipo: 'exito', mensaje: 'Vaciaste tu pedido.' });
        }}
      >
        <p className="m-0">
          Vas a sacar {plural(cantidadProductos, 'producto', 'productos')} de{' '}
          {plural(grupos.length, 'proveedor', 'proveedores')}. Los avisos de stock te lo van a
          volver a proponer.
        </p>
      </ConfirmDialog>
    </>
  );
}
