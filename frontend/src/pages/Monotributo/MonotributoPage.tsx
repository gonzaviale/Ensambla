import { useMemo, useState, type CSSProperties } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import { Cifra, DiceSami, Seccion } from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { pesos, porcentaje } from '@/lib/gestion/formato';
import {
  estadoMonotributo,
  facturaSimulada,
  nombreDelMes,
  type EstadoMonotributo,
} from '@/lib/gestion/monotributo';
import { armarPedido } from '@/lib/gestion/pedidos';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { formatoFecha } from '@/lib/sistema/formato';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

// Monotributo: lo facturado contra el tope de la categoría, cuándo se pasa, qué
// corresponde en las próximas recategorizaciones, el vencimiento de la cuota y la factura C desde una venta.
export function MonotributoPage() {
  return (
    <MarcoDeNegocio
      pantalla="monotributo"
      titulo={
        <>
          Que el monotributo no te sorprenda.
          <br />
          Sami lo sigue con tus ventas.
        </>
      }
      bajada="Cuánto facturaste en 12 meses, cuándo te pasás de categoría, cuándo vence la cuota y la factura desde la venta."
    >
      {(negocio) => <Monotributo negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Monotributo({ negocio }: { negocio: Negocio }) {
  // Los pedidos confirmados hoy ya son ventas del cuaderno: cuentan en lo facturado en 12 meses.
  const estado = useMemo(
    () => estadoMonotributo(negocio.datos, negocio.hoy),
    [negocio.datos, negocio.hoy],
  );
  const { categoria } = estado;
  const cambio = estado.recategorizaciones.find(
    (item) => item.corresponde?.letra !== categoria.letra,
  );

  return (
    <>
      <span className="gn-aviso-mockup">
        <Icon name="bulb" /> Las categorías, los topes y las cuotas son de ejemplo. Las vigentes las
        publica ARCA y cambian dos veces por año.
      </span>

      <Facturar negocio={negocio} estado={estado} />

      <div className="gn-cifras">
        <Cifra
          etiqueta={`Categoría ${categoria.letra}`}
          valor={pesos(estado.total12)}
          detalle={`Facturado en 12 meses · tope ${pesos(categoria.tope)}`}
          tono={estado.usado >= 1 ? 'mal' : estado.usado >= 0.85 ? 'ojo' : 'neutro'}
        />
        <Cifra
          etiqueta="Te pasás del tope"
          valor={estado.superaEn ? nombreDelMes(estado.superaEn) : 'No en este año'}
          detalle={`A este ritmo: ${pesos(estado.ritmoMensual)} por mes`}
          tono={estado.superaEn ? 'ojo' : 'bien'}
        />
        <Cifra
          etiqueta="Próxima cuota"
          valor={pesos(categoria.cuota)}
          detalle={`Vence el ${formatoFecha(estado.vencimiento.fecha)}: faltan ${estado.vencimiento.dias} días`}
        />
      </div>

      <DiceSami pose={estado.superaEn ? 'think' : 'wink'}>
        <p>
          Usaste el <strong>{porcentaje(estado.usado)}</strong> del tope de tu categoría.
          {estado.superaEn && (
            <>
              {' '}
              Si seguís vendiendo como en los últimos 90 días, en{' '}
              <strong>{nombreDelMes(estado.superaEn)}</strong> lo facturado en 12 meses pasa los{' '}
              {pesos(categoria.tope)}.
            </>
          )}
          {cambio?.corresponde && (
            <>
              {' '}
              En la recategorización de <strong>{nombreDelMes(cambio.mes)}</strong> te va a
              corresponder la <strong>{cambio.corresponde.letra}</strong>: la cuota pasa de{' '}
              {pesos(categoria.cuota)} a {pesos(cambio.corresponde.cuota)} por mes. Ya lo podés
              sumar a tus gastos fijos en «¿Me conviene?».
            </>
          )}
        </p>
      </DiceSami>

      <Proyeccion estado={estado} />

      <Seccion
        titulo="Las próximas recategorizaciones"
        bajada="Son en enero y en julio, con lo facturado en los 12 meses anteriores."
      >
        <ul className="gn-recategorizaciones">
          {estado.recategorizaciones.map((item) => {
            const sube = item.corresponde?.letra !== categoria.letra;
            return (
              <li key={item.mes} data-sube={sube || undefined}>
                <strong>{nombreDelMes(item.mes)}</strong>
                <span>Vas a llevar {pesos(item.acumulado)}</span>
                <span className="tag" data-riesgo={sube ? 'medio' : 'bajo'}>
                  {item.corresponde
                    ? sube
                      ? `Pasás a la ${item.corresponde.letra}`
                      : `Seguís en la ${item.corresponde.letra}`
                    : 'Te pasás de la última categoría'}
                </span>
                {item.corresponde && <span>Cuota {pesos(item.corresponde.cuota)}</span>}
              </li>
            );
          })}
        </ul>
      </Seccion>
    </>
  );
}

/** Lo facturado en 12 meses, mes a mes hacia adelante, contra el tope. Cada barra lleva su valor escrito. */
function Proyeccion({ estado }: { estado: EstadoMonotributo }) {
  const escala =
    Math.max(estado.categoria.tope, ...estado.proyeccion.map((mes) => mes.acumulado12)) * 1.05;
  const tope = (estado.categoria.tope / escala) * 100;
  return (
    <Seccion
      titulo="Lo facturado en 12 meses, de acá a un año"
      bajada={`Cada mes suma lo que vendés hoy (${pesos(estado.ritmoMensual)}) y sale el mes más viejo. Los 3 meses más nuevos salen de tu cuaderno; los otros 9, de tus facturas anteriores.`}
    >
      <ul className="gn-barras" style={{ '--gn-tope': `${tope}%` } as CSSProperties}>
        {estado.proyeccion.map((mes) => (
          <li key={mes.mes} data-supera={mes.superaElTope || undefined}>
            <span className="gn-barras-mes">{nombreDelMes(mes.mes)}</span>
            <span className="gn-barras-pista">
              <i style={{ width: `${(mes.acumulado12 / escala) * 100}%` }} />
            </span>
            <span className="gn-barras-valor">
              {pesos(mes.acumulado12)}
              {mes.superaElTope && <small>pasa el tope</small>}
            </span>
          </li>
        ))}
      </ul>
      <p className="gn-gris">
        La línea marca el tope de la categoría {estado.categoria.letra}:{' '}
        {pesos(estado.categoria.tope)}.
      </p>
    </Seccion>
  );
}

function Facturar({ negocio, estado }: { negocio: Negocio; estado: EstadoMonotributo }) {
  const confirmados = useNegocioStore((store) => store.confirmados);
  const facturas = useNegocioStore((store) => store.facturas);
  const onEmitir = useNegocioStore((store) => store.emitirFactura);
  const navegacion = useNavegacion();
  // Se factura lo que se vendió: los pedidos confirmados. Los que faltan confirmar se ven, pero esperan.
  const ventas = useMemo(
    () =>
      negocio.pedidos
        .map((mensaje) => confirmados[mensaje.id] ?? armarPedido(mensaje, negocio.datos))
        .filter((pedido) => pedido.total > 0),
    [negocio.pedidos, negocio.datos, confirmados],
  );
  const [cuit, setCuit] = useState<Record<string, string>>({});
  const avisar = useSistemaStore((store) => store.avisar);
  const emitidas = Object.values(facturas);
  const vendidoHoy = ventas
    .filter((venta) => confirmados[venta.mensaje.id])
    .reduce((total, venta) => total + venta.total, 0);

  const emitir = (id: string, cliente: string, total: number) => {
    const receptor = cuit[id]?.trim()
      ? `${cliente} (CUIT ${cuit[id]?.trim()})`
      : 'Consumidor final';
    const factura = facturaSimulada(receptor, total, negocio.hoy, 41 + emitidas.length);
    onEmitir(id, factura);
    avisar(`Factura C ${factura.numero} lista. En el mockup es simulada: no se mandó a ARCA.`);
  };

  return (
    <Seccion
      titulo="Facturar las ventas de hoy"
      bajada="Factura C desde los pedidos que confirmaste, sin entrar a la web de ARCA. Si el cliente te pide factura con CUIT, escribilo."
    >
      {vendidoHoy > 0 && (
        <p className="gn-facturado-hoy" role="status">
          <Icon name="check-circle" />
          <span>
            Los pedidos que confirmaste hoy suman <strong>{pesos(vendidoHoy)}</strong> y ya cuentan
            en lo facturado en 12 meses: llevás {pesos(estado.total12)} de{' '}
            {pesos(estado.categoria.tope)}, el <strong>{porcentaje(estado.usado)}</strong> del tope
            de la categoría {estado.categoria.letra}.
          </span>
        </p>
      )}
      <ul className="gn-facturas">
        {ventas.map((venta) => {
          const id = venta.mensaje.id;
          const factura = facturas[id];
          return (
            <li key={id}>
              <div className="gn-facturas-fila">
                <span>
                  <strong>{venta.mensaje.cliente}</strong>
                  <small className="gn-gris">
                    {venta.lineas
                      .filter((linea) => linea.entrega > 0)
                      .map((linea) => `${linea.entrega} × ${linea.producto?.nombre}`)
                      .join(' · ')}
                  </small>
                </span>
                <strong>{pesos(venta.total)}</strong>
                {factura ? (
                  <span className="gn-enviado">
                    <Icon name="check-circle" /> Facturada
                  </span>
                ) : !confirmados[id] ? (
                  <span className="gn-facturas-accion">
                    <small className="gn-gris">Sin confirmar</small>
                    <Btn cls="ghost small" onClick={() => void navegacion.irAPedidos()}>
                      Confirmarlo
                    </Btn>
                  </span>
                ) : (
                  <span className="gn-facturas-accion">
                    <input
                      type="text"
                      inputMode="numeric"
                      placeholder="CUIT (opcional)"
                      aria-label={`CUIT de ${venta.mensaje.cliente}`}
                      value={cuit[id] ?? ''}
                      onChange={(evento) =>
                        setCuit((lista) => ({ ...lista, [id]: evento.target.value }))
                      }
                    />
                    <Btn
                      cls="soft small"
                      onClick={() => emitir(id, venta.mensaje.cliente, venta.total)}
                    >
                      Emitir factura C
                    </Btn>
                  </span>
                )}
              </div>
              {factura && (
                <dl className="gn-factura">
                  <div>
                    <dt>Factura C</dt>
                    <dd>{factura.numero}</dd>
                  </div>
                  <div>
                    <dt>A</dt>
                    <dd>{factura.cliente}</dd>
                  </div>
                  <div>
                    <dt>CAE</dt>
                    <dd>{factura.cae}</dd>
                  </div>
                  <div>
                    <dt>Vence el CAE</dt>
                    <dd>{formatoFecha(factura.venceCae)}</dd>
                  </div>
                  <div>
                    <dt>Total</dt>
                    <dd>{pesos(factura.total)}</dd>
                  </div>
                </dl>
              )}
            </li>
          );
        })}
      </ul>
      <p className="gn-gris">
        Simulada: en el producto, el CAE lo da el servicio web de facturación de ARCA con el
        certificado del comercio.
      </p>
    </Seccion>
  );
}
