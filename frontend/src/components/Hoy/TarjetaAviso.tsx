import { useId } from 'react';
import { Link, useNavigate } from '@tanstack/react-router';
import { CheckCircleIcon } from '@phosphor-icons/react';
import { Badge, Button, Panel, buttonClasses } from '@/components/ui';
import { useAccionesRed, usePedido, useProductos } from '@/hooks/red/useRed';
import { pesos } from '@/lib/red/formato';
import type { Alerta } from '@/types/red';
import { META_AVISO, type MensajeHoy } from './avisoMeta';

type TarjetaAvisoProps = {
  alerta: Alerta;
  /** La tarjeta con la acción principal de la pantalla (la de más plata en juego). */
  destacada?: boolean;
  onIgnorar: (alerta: Alerta) => void;
  avisar: (mensaje: MensajeHoy) => void;
};

// Un aviso: qué pasa en una línea, por qué, cuánta plata hay en juego y UN botón de un toque.
export function TarjetaAviso({ alerta, destacada, onIgnorar, avisar }: TarjetaAvisoProps) {
  const meta = META_AVISO[alerta.tipo];
  const Icono = meta.Icono;
  const tituloId = useId();
  const navigate = useNavigate();
  const productos = useProductos();
  const pedido = usePedido();
  const { aplicarPrecio, agregarAlPedido } = useAccionesRed();
  const accion = alerta.accion;
  const variante = destacada ? 'solid' : 'primary';

  const enElPedido =
    accion.tipo === 'agregar-al-pedido'
      ? pedido.find((item) => item.productoId === accion.productoId)
      : undefined;

  function ejecutar() {
    if (accion.tipo === 'aplicar-precio') {
      const nombre = productos.find((p) => p.id === accion.productoId)?.nombre ?? 'El producto';
      aplicarPrecio(accion.productoId, accion.precioNuevo);
      avisar({
        tipo: 'exito',
        mensaje: `Listo: ${nombre} ahora cuesta ${pesos(accion.precioNuevo)}. Falta cambiar la etiqueta en el mostrador`,
        accion: { etiqueta: 'Ir al mostrador', onClick: () => void navigate({ to: '/mostrador' }) },
      });
    } else if (accion.tipo === 'agregar-al-pedido') {
      const nombre = productos.find((p) => p.id === accion.productoId)?.nombre ?? 'El producto';
      agregarAlPedido(accion.productoId, accion.cantidad);
      avisar({
        tipo: 'exito',
        mensaje: `Agregamos ${accion.cantidad} de ${nombre} a tu pedido.`,
      });
    }
  }

  return (
    <li className="list-none">
      <Panel
        tight
        data-recorrido={destacada ? 'hoy-aviso' : undefined}
        className="flex flex-col gap-3"
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <Badge tone={meta.tono} icon={<Icono size={18} weight="bold" />}>
            {meta.etiqueta}
          </Badge>
          {alerta.impacto > 0 && (
            <p className="m-0 text-hint font-semibold text-soft">
              ≈ {pesos(alerta.impacto)} por mes en juego
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <h4 id={tituloId} className="m-0 text-body leading-snug font-bold">
            {alerta.titulo}
          </h4>
          <p className="m-0 text-hint text-soft">{alerta.porque}</p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {accion.tipo === 'aplicar-precio' && (
            <Button variant={variante} aria-describedby={tituloId} onClick={ejecutar}>
              {alerta.etiquetaAccion}
            </Button>
          )}

          {accion.tipo === 'agregar-al-pedido' &&
            (enElPedido ? (
              <Badge tone="ok" icon={<CheckCircleIcon size={18} weight="fill" />}>
                En tu pedido: {enElPedido.cantidad}
              </Badge>
            ) : (
              <Button variant={variante} aria-describedby={tituloId} onClick={ejecutar}>
                {alerta.etiquetaAccion}
              </Button>
            ))}

          {(accion.tipo === 'revisar-lista' || accion.tipo === 'confirmar-cambio') && (
            <Link
              to="/listas/$listaId"
              params={{ listaId: accion.listaId }}
              aria-describedby={tituloId}
              className={buttonClasses({ variant: variante })}
            >
              {alerta.etiquetaAccion}
            </Link>
          )}

          {(accion.tipo === 'ver-oferta' ||
            accion.tipo === 'ver-proveedores' ||
            accion.tipo === 'comparar') && (
            <Link
              to="/red"
              aria-describedby={tituloId}
              className={buttonClasses({ variant: variante })}
            >
              {alerta.etiquetaAccion}
            </Link>
          )}

          <Button variant="ghost" aria-describedby={tituloId} onClick={() => onIgnorar(alerta)}>
            Ignorar
          </Button>
        </div>
      </Panel>
    </li>
  );
}
