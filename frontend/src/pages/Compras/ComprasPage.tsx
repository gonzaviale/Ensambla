import { useEffect, useMemo, useState } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { CampoDePlata, DiceSami, MensajeDeWhatsapp, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import {
  cambiosDeCosto,
  faltantes,
  mensajeParaProveedor,
  planDeCompras,
  type Faltante,
  type OrdenDeCompra,
  type Urgencia,
} from '@/lib/gestion/compras';
import { COSTOS_DEL_REMITO } from '@/lib/gestion/datosDeEjemplo';
import { pesos, porcentaje } from '@/lib/gestion/formato';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

const URGENCIAS: Record<Urgencia, string> = {
  agotado: 'Agotado',
  urgente: 'Urgente',
  pronto: 'Esta semana',
};

const duracion = (faltante: Faltante) =>
  faltante.producto.stock === 0
    ? 'Ya no hay'
    : faltante.diasDeStock < 1
      ? 'Menos de un día'
      : `${Math.floor(faltante.diasDeStock)} días`;

// Compras: del ritmo de venta al pedido por proveedor, ajustado a la plata disponible, enviado por
// WhatsApp y recibido con la foto del remito, que actualiza el stock y los costos.
export function ComprasPage() {
  return (
    <MarcoDeNegocio
      pantalla="compras"
      titulo={
        <>
          Sami te avisa antes de que se acabe.
          <br />Y te arma el pedido.
        </>
      }
      bajada="Mira a qué ritmo vendés cada producto, cuándo pasa cada proveedor y cuánta plata tenés para comprar."
    >
      {(negocio) => <Compras negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Compras({ negocio }: { negocio: Negocio }) {
  const lista = useMemo(() => faltantes(negocio.datos, negocio.hoy), [negocio.datos, negocio.hoy]);
  const caja = useNegocioStore((estado) => estado.caja);
  const setCaja = useNegocioStore((estado) => estado.cambiarCaja);
  const enviadas = useNegocioStore((estado) => estado.ordenesEnviadas);
  const enviarOrden = useNegocioStore((estado) => estado.enviarOrden);
  const plan = useMemo(() => planDeCompras(lista, caja, enviadas), [lista, caja, enviadas]);
  const avisar = useSistemaStore((store) => store.avisar);

  const enviar = (orden: OrdenDeCompra) => {
    enviarOrden(orden);
    avisar(
      `Le mandamos el pedido a ${orden.proveedor.nombre}. Cuando llegue, sacale una foto al remito.`,
    );
  };

  if (lista.length === 0)
    return (
      <DiceSami pose="wink">
        <p>
          Tenés stock para llegar a la próxima entrega de cada proveedor. No hace falta pedir nada.
        </p>
      </DiceSami>
    );

  const agotados = lista.filter((faltante) => faltante.urgencia === 'agotado').length;

  return (
    <>
      <DiceSami lado="derecha">
        <p>
          Mirando lo que vendiste en los últimos 30 días, <strong>{lista.length} productos</strong>{' '}
          no llegan a la próxima entrega
          {agotados > 0 && (
            <>
              {' '}
              y <strong>{agotados === 1 ? '1 ya se agotó' : `${agotados} ya se agotaron`}</strong>
            </>
          )}
          . Te armé un pedido por proveedor con una semana de colchón, según la plata que tengas.
        </p>
      </DiceSami>

      <Seccion
        titulo="Qué pedir y a quién"
        bajada="Lo agotado y lo urgente va siempre. Lo demás entra mientras alcance la plata; lo que no, queda para la próxima vuelta."
      >
        <div className="gn-caja">
          <label>
            <span>
              Plata para compras esta semana
              <small className="gn-gris">
                Lo que queda en la caja después de apartar el cambio y los gastos fijos que vencen
              </small>
            </span>
            <CampoDePlata
              valor={caja}
              onCambiar={setCaja}
              etiqueta="Plata para compras esta semana"
            />
          </label>
          <dl className="gn-caja-cuenta">
            {plan.enviado > 0 && (
              <div>
                <dt>Ya pedido</dt>
                <dd>{pesos(plan.enviado)}</dd>
              </div>
            )}
            <div>
              <dt>{plan.enviado > 0 ? 'Falta mandar' : 'Los pedidos suman'}</dt>
              <dd>{pesos(plan.total)}</dd>
            </div>
            <div data-tono={plan.quedan < 0 ? 'mal' : 'bien'}>
              <dt>{plan.quedan < 0 ? 'Te faltan' : 'Te quedan'}</dt>
              <dd>{pesos(Math.abs(plan.quedan))}</dd>
            </div>
          </dl>
          {plan.quedan < 0 && (
            <p className="gn-diferencia" data-tono="mal">
              Lo urgente ya pasa tu plata por {pesos(-plan.quedan)}. Cobrar el fiado atrasado te
              alcanza para cubrirlo.
            </p>
          )}
        </div>

        <div className="gn-ordenes">
          {plan.ordenes.map((orden) => (
            <Orden
              key={orden.proveedor.id}
              orden={orden}
              negocio={negocio}
              onEnviar={() => enviar(orden)}
            />
          ))}
        </div>
      </Seccion>
    </>
  );
}

/** Un producto del pedido: cuánto queda, cuánto dura al ritmo de venta y cuánto pedir. */
function ItemDeCompra({ item, postergado = false }: { item: Faltante; postergado?: boolean }) {
  const ritmo =
    item.ritmo >= 1
      ? `vendés ${Math.round(item.ritmo)} por día`
      : `vendés ${Math.max(1, Math.round(item.ritmo * 7))} por semana`;
  return (
    <li data-postergado={postergado || undefined}>
      <span className="gn-items-que">
        <strong>{item.producto.nombre}</strong>
        <small className="gn-gris">
          Quedan {item.producto.stock} · {ritmo}
        </small>
      </span>
      {postergado ? (
        <span className="gn-items-cuando">
          <small className="gn-gris">Te dura {duracion(item).toLowerCase()}</small>
          <span className="tag outline">Para la próxima</span>
        </span>
      ) : (
        <span className="gn-items-cuando">
          <span className="tag" data-urgencia={item.urgencia}>
            {URGENCIAS[item.urgencia]}
          </span>
          <small className="gn-gris">
            {item.producto.stock === 0 ? 'Ya no hay' : `Te dura ${duracion(item).toLowerCase()}`}
          </small>
        </span>
      )}
      <span className="gn-items-cuanto">
        <strong>Pedir {item.pedir}</strong>
        <small className="gn-gris">{pesos(item.subtotal)}</small>
      </span>
    </li>
  );
}

function Orden({
  orden,
  negocio,
  onEnviar,
}: {
  orden: OrdenDeCompra;
  negocio: Negocio;
  onEnviar: () => void;
}) {
  const recepcion = useNegocioStore((store) => store.recepcion[orden.proveedor.id] ?? 'enviado');
  const cambiarRecepcion = useNegocioStore((store) => store.cambiarRecepcion);
  const precios = useNegocioStore((store) => store.precios);
  const cambiarPrecio = useNegocioStore((store) => store.cambiarPrecio);
  const [verMensaje, setVerMensaje] = useState(false);
  const avisar = useSistemaStore((store) => store.avisar);
  const estado = orden.enviada ? recepcion : 'borrador';
  const proveedorId = orden.proveedor.id;

  useEffect(() => {
    if (recepcion !== 'leyendo') return;
    const lectura = window.setTimeout(() => cambiarRecepcion(proveedorId, 'recibido'), 1300);
    return () => window.clearTimeout(lectura);
  }, [recepcion, proveedorId, cambiarRecepcion]);

  const cambios = cambiosDeCosto(
    orden.items.map((item) => item.producto),
    COSTOS_DEL_REMITO,
  );

  return (
    <article className="gn-orden" data-estado={estado}>
      <header>
        <div>
          <h3>{orden.proveedor.nombre}</h3>
          <small className="gn-gris">
            {orden.proveedor.telefono} · pasa cada {orden.proveedor.entregaCadaDias} días
          </small>
        </div>
        <strong>{pesos(orden.total)}</strong>
      </header>
      <ul className="gn-items">
        {orden.items.map((item) => (
          <ItemDeCompra key={item.producto.id} item={item} />
        ))}
        {!orden.enviada &&
          orden.postergados.map((item) => (
            <ItemDeCompra key={item.producto.id} item={item} postergado />
          ))}
      </ul>
      {orden.enviada && orden.postergados.length > 0 && (
        <p className="gn-gris">
          Quedó para la próxima: {orden.postergados.map((item) => item.producto.nombre).join(', ')}.
        </p>
      )}

      {estado === 'borrador' &&
        (orden.items.length === 0 ? (
          <p className="gn-gris">
            Con la plata de esta semana, todo lo de {orden.proveedor.nombre} espera a la próxima
            vuelta.
          </p>
        ) : (
          <>
            {verMensaje && (
              <MensajeDeWhatsapp
                para={orden.proveedor.nombre}
                texto={mensajeParaProveedor(orden, negocio.sistema.name)}
              />
            )}
            <div className="gn-orden-acciones">
              <Btn
                cls="ghost small"
                aria-expanded={verMensaje}
                onClick={() => setVerMensaje(!verMensaje)}
              >
                <Icon name="eye" /> {verMensaje ? 'Ocultar el mensaje' : 'Ver el mensaje'}
              </Btn>
              <Btn cls="primary small" onClick={onEnviar}>
                Mandar por WhatsApp
              </Btn>
            </div>
          </>
        ))}

      {estado === 'enviado' && (
        <div className="gn-orden-acciones">
          <span className="gn-enviado">
            <Icon name="clock" /> Pedido enviado. Esperando la entrega.
          </span>
          <Btn cls="secondary small" onClick={() => cambiarRecepcion(proveedorId, 'leyendo')}>
            <Icon name="camera" /> Llegó: foto del remito
          </Btn>
        </div>
      )}

      {estado === 'leyendo' && (
        <p className="gn-enviado" role="status">
          <Icon name="spark" /> Sami está leyendo el remito…
        </p>
      )}

      {estado === 'recibido' && (
        <div className="gn-recepcion">
          <p className="gn-enviado">
            <Icon name="check-circle" /> Sumé al stock{' '}
            {orden.items.map((item) => `${item.pedir} ${item.producto.nombre}`).join(', ')}.
          </p>
          {cambios.length === 0 ? (
            <p className="gn-gris">Llegó todo al mismo costo que la última vez.</p>
          ) : (
            <>
              <DiceSami pose="think">
                <p>
                  <strong>
                    {orden.proveedor.nombre} subió{' '}
                    {cambios.length === 1 ? 'un producto' : `${cambios.length} productos`}.
                  </strong>{' '}
                  Si no cambiás el precio, cada venta te deja menos. Este es el precio que mantiene
                  tu margen:
                </p>
              </DiceSami>
              <ul className="gn-subas">
                {cambios.map((cambio) => {
                  const listo = precios[cambio.producto.id] === cambio.precioSugerido;
                  return (
                    <li key={cambio.producto.id}>
                      <span>
                        <strong>{cambio.producto.nombre}</strong>
                        <small className="gn-gris">
                          Costo {pesos(cambio.producto.costo)} → {pesos(cambio.costoNuevo)} (+
                          {porcentaje(cambio.variacion)})
                        </small>
                      </span>
                      <span className="gn-subas-precio">
                        {pesos(cambio.producto.precio)} →{' '}
                        <strong>{pesos(cambio.precioSugerido)}</strong>
                        <small className="gn-gris">margen {porcentaje(cambio.margen)}</small>
                      </span>
                      {listo ? (
                        <span className="gn-enviado">
                          <Icon name="check" /> Actualizado
                        </span>
                      ) : (
                        <Btn
                          cls="soft small"
                          onClick={() => {
                            cambiarPrecio(cambio.producto.id, cambio.precioSugerido);
                            avisar(
                              `${cambio.producto.nombre} ahora sale ${pesos(cambio.precioSugerido)}.`,
                            );
                          }}
                        >
                          Usar este precio
                        </Btn>
                      )}
                    </li>
                  );
                })}
              </ul>
            </>
          )}
        </div>
      )}
    </article>
  );
}
