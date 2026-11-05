import { useEffect, useMemo, useState } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { DiceSami, MensajeDeWhatsapp, Seccion } from '@/components/gestion/piezas';
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
import { CAJA_PARA_COMPRAS, COSTOS_DEL_REMITO } from '@/lib/gestion/datosDeEjemplo';
import { pesos, porcentaje } from '@/lib/gestion/formato';
import type { Negocio } from '@/hooks/gestion/useNegocio';
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
  const [caja, setCaja] = useState(CAJA_PARA_COMPRAS);
  const plan = useMemo(() => planDeCompras(lista, caja), [lista, caja]);

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
      <DiceSami>
        <p>
          Mirando lo que vendiste en los últimos 30 días, <strong>{lista.length} productos</strong>{' '}
          no llegan a la próxima entrega
          {agotados > 0 && (
            <>
              {' '}
              y <strong>{agotados} ya se agotaron</strong>
            </>
          )}
          . Te armé un pedido por proveedor con una semana de colchón.
        </p>
      </DiceSami>

      <Seccion titulo="Lo que se está por acabar" bajada="Del que dura menos al que dura más.">
        <div className="gn-tabla-scroll">
          <table className="gn-tabla">
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col">Stock</th>
                <th scope="col">Vendés</th>
                <th scope="col">Te dura</th>
                <th scope="col">Pedir</th>
                <th scope="col">Proveedor</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((faltante) => (
                <tr key={faltante.producto.id}>
                  <th scope="row">{faltante.producto.nombre}</th>
                  <td>{faltante.producto.stock}</td>
                  <td>
                    {faltante.ritmo >= 1
                      ? `${Math.round(faltante.ritmo)} por día`
                      : `${Math.round(faltante.ritmo * 7)} por semana`}
                  </td>
                  <td>
                    <span className="tag" data-urgencia={faltante.urgencia}>
                      {URGENCIAS[faltante.urgencia]}
                    </span>{' '}
                    {duracion(faltante)}
                  </td>
                  <td>
                    <strong>{faltante.pedir}</strong> · {pesos(faltante.subtotal)}
                  </td>
                  <td>
                    {faltante.proveedor.nombre}
                    <small className="gn-gris">
                      Pasa cada {faltante.proveedor.entregaCadaDias} días
                    </small>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Seccion>

      <Seccion
        titulo="Cuánto podés gastar"
        bajada="Sale de tu caja: lo que queda después de apartar el cambio y los gastos fijos que vencen."
      >
        <div className="gn-caja">
          <label>
            <span>Para compras esta semana</span>
            <input
              type="range"
              min={50000}
              max={250000}
              step={5000}
              value={caja}
              onChange={(evento) => setCaja(Number(evento.target.value))}
            />
            <strong>{pesos(caja)}</strong>
          </label>
          <p>
            El pedido suma <strong>{pesos(plan.total)}</strong>.{' '}
            {plan.postergados.length === 0
              ? 'Entra todo.'
              : `Lo agotado y lo urgente va siempre. Para la próxima vuelta dejo ${plan.postergados
                  .map(
                    (faltante) =>
                      `${faltante.producto.nombre} (te dura ${duracion(faltante).toLowerCase()})`,
                  )
                  .join(', ')}.`}
          </p>
          {plan.total > caja && (
            <p className="gn-diferencia" data-tono="mal">
              Lo urgente ya pasa tu presupuesto por {pesos(plan.total - caja)}. Cobrar el fiado
              atrasado te alcanza para cubrirlo.
            </p>
          )}
        </div>
      </Seccion>

      <Seccion
        titulo="Pedidos por proveedor"
        bajada="Revisá el mensaje antes de mandarlo. Nada sale sin tu OK."
      >
        <div className="gn-ordenes">
          {plan.ordenes.map((orden) => (
            <Orden key={orden.proveedor.id} orden={orden} negocio={negocio} />
          ))}
        </div>
      </Seccion>
    </>
  );
}

type EstadoDeOrden = 'borrador' | 'enviado' | 'leyendo' | 'recibido';

function Orden({ orden, negocio }: { orden: OrdenDeCompra; negocio: Negocio }) {
  const [estado, setEstado] = useState<EstadoDeOrden>('borrador');
  const [verMensaje, setVerMensaje] = useState(false);
  const [actualizados, setActualizados] = useState<string[]>([]);
  const avisar = useSistemaStore((store) => store.avisar);

  useEffect(() => {
    if (estado !== 'leyendo') return;
    const lectura = window.setTimeout(() => setEstado('recibido'), 1300);
    return () => window.clearTimeout(lectura);
  }, [estado]);

  const cambios = cambiosDeCosto(
    orden.items.map((item) => item.producto),
    COSTOS_DEL_REMITO,
  );

  const enviar = () => {
    setEstado('enviado');
    setVerMensaje(false);
    avisar(
      `Le mandamos el pedido a ${orden.proveedor.nombre}. Cuando llegue, sacale una foto al remito.`,
    );
  };

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
      <ul>
        {orden.items.map((item) => (
          <li key={item.producto.id}>
            <span>
              {item.pedir} × {item.producto.nombre}
            </span>
            <span className="gn-gris">{pesos(item.subtotal)}</span>
          </li>
        ))}
      </ul>

      {estado === 'borrador' && (
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
            <Btn cls="primary small" onClick={enviar}>
              Mandar por WhatsApp
            </Btn>
          </div>
        </>
      )}

      {estado === 'enviado' && (
        <div className="gn-orden-acciones">
          <span className="gn-enviado">
            <Icon name="clock" /> Pedido enviado. Esperando la entrega.
          </span>
          <Btn cls="secondary small" onClick={() => setEstado('leyendo')}>
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
                  const listo = actualizados.includes(cambio.producto.id);
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
                            setActualizados((lista) => [...lista, cambio.producto.id]);
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
