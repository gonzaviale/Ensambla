import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import {
  CampoDePlata,
  Cifra,
  CuentaPasoAPaso,
  DiceSami,
  MensajeDeWhatsapp,
  Seccion,
} from '@/components/gestion/piezas';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { faltantes } from '@/lib/gestion/compras';
import {
  CAMBIO_INICIAL,
  GASTOS_FIJOS,
  type Renglon,
  type TipoDeRenglon,
} from '@/lib/gestion/datosDeEjemplo';
import { conPuntos, montoDe, pesos, porcentaje } from '@/lib/gestion/formato';
import {
  destinosDelCuaderno,
  llevaCliente,
  llevaMedio,
  pendientesDeLaHoja,
  repartoPorBloque,
  TIPOS_DE_RENGLON,
  type Destino,
} from '@/lib/gestion/hoja';
import { renglonesDePedidos } from '@/lib/gestion/elDia';
import { proyectar, supuestoDePrecio } from '@/lib/gestion/proyeccion';
import {
  cierreDelDia,
  fiadoConLaHoja,
  type Cliente,
  type CuentaCorriente,
} from '@/lib/gestion/resultado';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

const MEDIOS = { efectivo: 'En efectivo', cuenta: 'Por QR o transferencia' } as const;
const MEDIOS_CORTOS = { efectivo: 'en efectivo', cuenta: 'por QR o transferencia' } as const;

// El cierre del día: la foto de la hoja de hoy, la revisión de lo que leyó Sami y el cierre con
// la ganancia de verdad separada de la plata del cajón. Abajo, el mes con la cuenta completa y el fiado.
export function CierrePage() {
  return (
    <MarcoDeNegocio
      pantalla="cierre"
      titulo={
        <>
          Sacale una foto a la hoja de hoy.
          <br />
          Sami te cierra el día.
        </>
      }
      bajada="Cuánto vendiste, cuánto ganaste de verdad y cuánta plata tiene que haber en el cajón. No son lo mismo."
    >
      {(negocio) => <Cierre negocio={negocio} />}
    </MarcoDeNegocio>
  );
}

function Cierre({ negocio }: { negocio: Negocio }) {
  const paso = useNegocioStore((estado) => estado.pasoDelCierre);
  const setPaso = useNegocioStore((estado) => estado.cambiarPasoDelCierre);
  const renglones = useNegocioStore((estado) => estado.hoja);
  const cambiarHoja = useNegocioStore((estado) => estado.cambiarHoja);
  const creados = useNegocioStore((estado) => estado.bloquesCreados);
  const sumarBloque = useNegocioStore((estado) => estado.crearBloque);
  const confirmados = useNegocioStore((estado) => estado.confirmados);
  const [foto, setFoto] = useState<string | null>(null);
  // Los pedidos de WhatsApp confirmados ya están en el cuaderno: entran al día sin pasar por la foto.
  const dePedidos = useMemo(() => renglonesDePedidos(Object.values(confirmados)), [confirmados]);
  const todos = [...renglones, ...dePedidos];

  useEffect(() => {
    if (paso !== 'leyendo') return;
    const lectura = window.setTimeout(() => setPaso('revisar'), 1400);
    return () => window.clearTimeout(lectura);
  }, [paso, setPaso]);

  useEffect(
    () => () => {
      if (foto) URL.revokeObjectURL(foto);
    },
    [foto],
  );

  const alElegirFoto = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    if (archivo) setFoto(URL.createObjectURL(archivo));
    setPaso('leyendo');
  };

  const avisar = useSistemaStore((estado) => estado.avisar);
  const destinos = useMemo(
    () => destinosDelCuaderno(negocio.sistema, creados),
    [negocio.sistema, creados],
  );
  const pendientes = pendientesDeLaHoja(renglones, destinos);

  const cambiar = (cambiado: Renglon) =>
    cambiarHoja(renglones.map((renglon) => (renglon.id === cambiado.id ? cambiado : renglon)));

  const crearBloque = (destino: Destino) => {
    sumarBloque(destino.tipoDeBloque);
    avisar(`Al cerrar el día sumo el bloque «${destino.bloque}» a tu cuaderno.`);
  };

  const cerrar = () => {
    setPaso('cerrado');
    avisar(`Anoté los ${renglones.length} renglones de la hoja en tu cuaderno.`);
  };

  const { resultado } = negocio;
  const margenDelMes = resultado.ventas > 0 ? resultado.margenBruto / resultado.ventas : 0;
  const cerrado = paso === 'cerrado';

  return (
    <>
      <Seccion
        titulo="Hoy"
        bajada={cerrado ? 'Día cerrado. Si algo no cuadra, volvé a revisar la hoja.' : undefined}
        acciones={
          cerrado ? (
            <Btn cls="ghost small" onClick={() => setPaso('revisar')}>
              <Icon name="edit" /> Revisar la hoja
            </Btn>
          ) : undefined
        }
      >
        {paso === 'foto' && (
          <div className="gn-foto">
            <DiceSami>
              <p>
                <strong>Seguí anotando como siempre, todo en la misma hoja.</strong> Al final del
                día sacale una foto: yo paso cada renglón al bloque que corresponde de tu cuaderno
                (ventas, fiado, gastos, pérdidas) y te cierro la caja. Vos revisás antes de cerrar.
                Para cargar un solo bloque, como una lista de precios, usá la foto desde el
                cuaderno.
              </p>
            </DiceSami>
            {dePedidos.length > 0 && (
              <p className="gn-gris">
                Ya tengo {dePedidos.length === 1 ? 'un pedido' : `${dePedidos.length} pedidos`} de
                WhatsApp que confirmaste por{' '}
                {pesos(dePedidos.reduce((total, renglon) => total + renglon.importe, 0))}: con la
                foto sumo el resto del día.
              </p>
            )}
            <div className="gn-foto-acciones">
              <label className="btn primary">
                <Icon name="camera" /> Sacar la foto
                <input
                  type="file"
                  accept="image/*"
                  capture="environment"
                  hidden
                  onChange={alElegirFoto}
                />
              </label>
              <Btn cls="secondary" onClick={() => setPaso('leyendo')}>
                <Icon name="image" /> Usar la hoja de ejemplo
              </Btn>
            </div>
          </div>
        )}

        {paso === 'leyendo' && (
          <div className="gn-leyendo" role="status">
            {foto && <img src={foto} alt="La hoja de hoy" />}
            <DiceSami pose="think">
              <p>Estoy leyendo la hoja… separo cada renglón y lo cruzo con tu inventario.</p>
            </DiceSami>
          </div>
        )}

        {paso === 'revisar' && (
          <div className="gn-revision">
            <DiceSami pose="wink">
              <p>
                Leí <strong>{renglones.length} renglones</strong> y elegí en qué bloque de tu
                cuaderno va cada uno. Marqué en amarillo el que no tengo claro. Si leí mal un
                importe o un nombre, tocá «Corregir».
              </p>
            </DiceSami>
            <ul className="gn-renglones">
              {renglones.map((renglon) => (
                <RenglonDeLaHoja
                  key={renglon.id}
                  renglon={renglon}
                  destinos={destinos}
                  clientes={negocio.datos.clientes}
                  onCambiar={cambiar}
                  onCrearBloque={crearBloque}
                />
              ))}
            </ul>
            {dePedidos.length > 0 && <DePedidos renglones={dePedidos} destinos={destinos} />}
            <div className="gn-revision-pie">
              {!pendientes.listo && (
                <p className="gn-gris" role="status">
                  {[
                    pendientes.sinBloque.length > 0 &&
                      `Falta crear ${pendientes.sinBloque.map((bloque) => `«${bloque}»`).join(' y ')} o cambiar el destino`,
                    pendientes.sinCliente > 0 &&
                      (pendientes.sinCliente === 1
                        ? 'Un fiado no dice de quién es'
                        : `${pendientes.sinCliente} fiados no dicen de quién son`),
                  ]
                    .filter(Boolean)
                    .join('. ')}
                  .
                </p>
              )}
              <Btn cls="ghost" onClick={() => setPaso('foto')}>
                Otra foto
              </Btn>
              <Btn cls="primary" disabled={!pendientes.listo} onClick={cerrar}>
                <Icon name="check" /> Cerrar el día
              </Btn>
            </div>
          </div>
        )}

        {cerrado && (
          <ElDia
            renglones={todos}
            margenDelMes={margenDelMes}
            reparto={repartoPorBloque(renglones, destinos)}
            deWhatsapp={dePedidos.length}
          />
        )}
      </Seccion>

      <ElMes negocio={negocio} />

      <Fiado negocio={negocio} renglones={cerrado ? renglones : []} />

      <ResumenDelDomingo negocio={negocio} />
    </>
  );
}

function RenglonDeLaHoja({
  renglon,
  destinos,
  clientes,
  onCambiar,
  onCrearBloque,
}: {
  renglon: Renglon;
  destinos: Record<TipoDeRenglon, Destino>;
  clientes: Cliente[];
  onCambiar: (renglon: Renglon) => void;
  onCrearBloque: (destino: Destino) => void;
}) {
  const [corrigiendo, setCorrigiendo] = useState(Boolean(renglon.dudoso));
  const destino = destinos[renglon.tipo];
  const sinCliente = llevaCliente(renglon.tipo) && !renglon.cliente?.trim();
  const abierto = corrigiendo || sinCliente;
  const listaDeClientes = `clientes-${renglon.id}`;

  const cambiarTipo = (tipo: TipoDeRenglon) =>
    onCambiar({
      ...renglon,
      tipo,
      dudoso: false,
      medio: llevaMedio(tipo) ? (renglon.medio ?? 'efectivo') : undefined,
    });

  return (
    <li data-dudoso={renglon.dudoso || undefined}>
      <div className="gn-renglon-fila">
        <span className="gn-escrito">{renglon.escrito}</span>
        <Icon name="arrow" />
        <span className="gn-entendido">
          <strong>{renglon.detalle}</strong>
          <small>
            {pesos(renglon.importe)}
            {llevaMedio(renglon.tipo) && renglon.medio && ` · ${MEDIOS_CORTOS[renglon.medio]}`}
            {llevaCliente(renglon.tipo) && renglon.cliente && ` · ${renglon.cliente}`}
          </small>
        </span>
        <label className="gn-destino">
          <span>Va a</span>
          <select
            aria-label={`A qué bloque va «${renglon.escrito}»`}
            value={renglon.tipo}
            onChange={(evento) => cambiarTipo(evento.target.value as TipoDeRenglon)}
          >
            {TIPOS_DE_RENGLON.map((tipo) => (
              <option key={tipo} value={tipo}>
                {destinos[tipo].bloque}
                {destinos[tipo].falta ? ' (no lo tenés)' : ''} · {destinos[tipo].como}
              </option>
            ))}
          </select>
        </label>
        <Btn cls="ghost small" aria-expanded={abierto} onClick={() => setCorrigiendo(!corrigiendo)}>
          <Icon name="edit" /> Corregir
        </Btn>
      </div>

      {renglon.dudoso && renglon.motivo && (
        <p className="gn-renglon-nota">
          <Icon name="bulb" />
          <span>{renglon.motivo}</span>
        </p>
      )}

      {destino.falta && (
        <div className="gn-renglon-falta">
          <span>
            Tu cuaderno no tiene un bloque de {destino.bloque}. Lo creo, o elegí otro destino
            arriba.
          </span>
          <Btn cls="soft small" onClick={() => onCrearBloque(destino)}>
            <Icon name="plus" /> Crear «{destino.bloque}»
          </Btn>
        </div>
      )}

      {abierto && (
        <div className="gn-renglon-editor">
          <label>
            <span>Qué es</span>
            <input
              type="text"
              value={renglon.detalle}
              onChange={(evento) => onCambiar({ ...renglon, detalle: evento.target.value })}
            />
          </label>
          <label>
            <span>Importe</span>
            <CampoDePlata
              valor={renglon.importe}
              onCambiar={(importe) => onCambiar({ ...renglon, importe })}
            />
          </label>
          {llevaMedio(renglon.tipo) && (
            <label>
              <span>Cómo se pagó</span>
              <select
                value={renglon.medio ?? 'efectivo'}
                onChange={(evento) =>
                  onCambiar({ ...renglon, medio: evento.target.value as keyof typeof MEDIOS })
                }
              >
                {Object.entries(MEDIOS).map(([clave, etiqueta]) => (
                  <option key={clave} value={clave}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </label>
          )}
          {llevaCliente(renglon.tipo) && (
            <label>
              <span>De quién</span>
              <input
                type="text"
                list={listaDeClientes}
                value={renglon.cliente ?? ''}
                aria-invalid={sinCliente || undefined}
                placeholder="Elegí o escribí el cliente"
                onChange={(evento) => onCambiar({ ...renglon, cliente: evento.target.value })}
              />
              <datalist id={listaDeClientes}>
                {clientes.map((cliente) => (
                  <option key={cliente.id} value={cliente.nombre} />
                ))}
              </datalist>
            </label>
          )}
          <Btn
            cls="secondary small"
            disabled={sinCliente}
            onClick={() => {
              onCambiar({ ...renglon, dudoso: false });
              setCorrigiendo(false);
            }}
          >
            <Icon name="check" /> Listo
          </Btn>
        </div>
      )}
    </li>
  );
}

const detallesDe = (renglones: Renglon[], tipo: TipoDeRenglon) =>
  renglones
    .filter((renglon) => renglon.tipo === tipo)
    .map((renglon) => renglon.detalle)
    .join(', ') || undefined;

/** Los pedidos de WhatsApp confirmados: ya están en el cuaderno, así que no se corrigen acá. */
function DePedidos({
  renglones,
  destinos,
}: {
  renglones: Renglon[];
  destinos: Record<TipoDeRenglon, Destino>;
}) {
  return (
    <div className="gn-de-pedidos">
      <h3>Entraron por WhatsApp</h3>
      <p className="gn-gris">
        Los pedidos que confirmaste ya están en tu cuaderno. Cuentan en el cierre, pero no hace
        falta anotarlos en la hoja.
      </p>
      <ul className="gn-renglones">
        {renglones.map((renglon) => (
          <li key={renglon.id} data-whatsapp>
            <div className="gn-renglon-fila">
              <span className="gn-escrito">
                <span className="tag">WhatsApp</span>
              </span>
              <Icon name="arrow" />
              <span className="gn-entendido">
                <strong>{renglon.detalle}</strong>
                <small>
                  {pesos(renglon.importe)}
                  {renglon.medio && ` · ${MEDIOS_CORTOS[renglon.medio]}`}
                </small>
              </span>
              <span className="gn-destino">
                <span>Va a</span>
                <strong>
                  {destinos[renglon.tipo].bloque} · {destinos[renglon.tipo].como}
                </strong>
              </span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

function ElDia({
  renglones,
  margenDelMes,
  reparto,
  deWhatsapp,
}: {
  renglones: Renglon[];
  margenDelMes: number;
  /** Dónde quedó anotado lo de la hoja. */
  reparto: { bloque: string; cantidad: number }[];
  /** Cuántos pedidos de WhatsApp entraron al día. */
  deWhatsapp: number;
}) {
  const cierre = cierreDelDia(renglones, margenDelMes);
  const [contado, setContado] = useState('');
  const diferencia = contado === '' ? null : montoDe(contado) - cierre.cajon;

  return (
    <div className="gn-dia">
      <DiceSami pose="wink">
        <p>
          Lo anoté en tu cuaderno:{' '}
          {reparto
            .map(
              ({ bloque, cantidad }) =>
                `${cantidad} ${cantidad === 1 ? 'renglón' : 'renglones'} en ${bloque}`,
            )
            .join(', ')}
          .
          {deWhatsapp > 0 &&
            ` ${deWhatsapp === 1 ? 'El pedido de WhatsApp ya estaba' : `Los ${deWhatsapp} pedidos de WhatsApp ya estaban`}: también cuentan.`}{' '}
          Ahora, lo que el cuaderno solo no te dice:
        </p>
      </DiceSami>
      <div className="gn-cifras">
        <Cifra
          etiqueta="Vendiste"
          valor={pesos(cierre.vendido)}
          detalle={`${pesos(cierre.fiadoNuevo)} fiado`}
        />
        <Cifra
          etiqueta="Ganaste de verdad"
          valor={pesos(cierre.ganancia)}
          tono={cierre.ganancia >= 0 ? 'bien' : 'mal'}
          detalle="Después de costos, pérdidas y la parte del día de tus gastos fijos"
        />
        <Cifra
          etiqueta="En el cajón tiene que haber"
          valor={pesos(cierre.cajon)}
          detalle={`Y ${pesos(cierre.enLaCuenta)} en la cuenta (QR y transferencias)`}
          tono="ojo"
        />
      </div>

      <div className="gn-dos">
        <CuentaPasoAPaso
          titulo="Por qué ganaste menos de lo que vendiste"
          pasos={[
            { etiqueta: 'Vendiste', valor: cierre.vendido },
            {
              etiqueta: 'Costo de lo que vendiste',
              valor: cierre.costoDeLoVendido,
              clase: 'resta',
              detalle: `Lo que te costó la mercadería que salió (margen del mes: ${porcentaje(margenDelMes)})`,
            },
            { etiqueta: 'Margen', valor: cierre.margenBruto, clase: 'total' },
            {
              etiqueta: 'Comisiones de cobro',
              valor: cierre.comisiones,
              clase: 'resta',
              detalle: 'QR y transferencias',
            },
            {
              etiqueta: 'Pérdidas',
              valor: cierre.mermas,
              clase: 'resta',
              detalle: detallesDe(renglones, 'merma'),
            },
            {
              etiqueta: 'Gastos del día',
              valor: cierre.gastos,
              clase: 'resta',
              detalle: detallesDe(renglones, 'gasto'),
            },
            {
              etiqueta: 'Parte del día de los gastos fijos',
              valor: cierre.fijosDelDia,
              clase: 'resta',
              detalle: 'Alquiler, luz, monotributo e internet, repartidos en 30 días',
            },
            { etiqueta: 'Ganaste de verdad', valor: cierre.ganancia, clase: 'total' },
          ]}
        />
        <div className="gn-columna">
          <CuentaPasoAPaso
            titulo="Por qué el cajón no es tu ganancia"
            pasos={[
              { etiqueta: 'Cambio al abrir', valor: CAMBIO_INICIAL },
              { etiqueta: 'Ventas en efectivo', valor: cierre.ventasEnEfectivo },
              { etiqueta: 'Cobros de fiado', valor: cierre.cobrosDeFiado },
              {
                etiqueta: 'Pago a proveedores',
                valor: cierre.pagosAProveedores,
                clase: 'resta',
                detalle: 'Sale plata, pero no es una pérdida: es mercadería',
              },
              { etiqueta: 'Gastos pagados', valor: cierre.gastos, clase: 'resta' },
              { etiqueta: 'Tiene que haber', valor: cierre.cajon, clase: 'total' },
            ]}
          />
          <label className="gn-contar">
            <span>¿Cuánto contaste en el cajón?</span>
            <span className="gn-monto">
              <span aria-hidden="true">$</span>
              <input
                type="text"
                inputMode="numeric"
                placeholder={conPuntos(cierre.cajon)}
                value={contado === '' ? '' : conPuntos(montoDe(contado)) || '0'}
                onChange={(evento) => setContado(evento.target.value)}
              />
            </span>
          </label>
          {diferencia !== null && (
            <p
              className="gn-diferencia"
              data-tono={Math.abs(diferencia) < 100 ? 'bien' : 'mal'}
              role="status"
            >
              {Math.abs(diferencia) < 100
                ? 'Cierra justo. ¡Bien!'
                : diferencia < 0
                  ? `Faltan ${pesos(-diferencia)}. ¿Hubo una venta fiada o un gasto sin anotar?`
                  : `Sobran ${pesos(diferencia)}. ¿Quedó una venta sin anotar?`}
            </p>
          )}
        </div>
      </div>
      {cierre.fiadoNuevo > 0 && (
        <DiceSami>
          <p>
            Ojo: los {pesos(cierre.fiadoNuevo)} que fiaste hoy cuentan en lo vendido, pero no están
            en el cajón. Ya los sumé a la cuenta de cada cliente.
          </p>
        </DiceSami>
      )}
    </div>
  );
}

function ElMes({ negocio }: { negocio: Negocio }) {
  const { resultado } = negocio;
  const diaDelMes = new Date().getDate();
  const avance =
    resultado.diaDeEquilibrio === null ? 0 : Math.min(1, diaDelMes / resultado.diaDeEquilibrio);

  return (
    <Seccion
      titulo="Tus últimos 30 días, con la cuenta completa"
      bajada="Sale de las ventas y el inventario de tu cuaderno."
    >
      <div className="gn-comparacion">
        <div data-tono="viejo">
          <span>Antes te mostrábamos</span>
          <strong>{pesos(resultado.cuentaVieja)}</strong>
          <small>Ventas menos el costo de la mercadería</small>
        </div>
        <Icon name="arrow" />
        <div data-tono="real">
          <span>Lo que ganaste de verdad</span>
          <strong>{pesos(resultado.ganancia)}</strong>
          <small>Con comisiones, pérdidas y gastos fijos</small>
        </div>
      </div>

      <div className="gn-dos">
        <CuentaPasoAPaso
          pasos={[
            {
              etiqueta: 'Ventas',
              valor: resultado.ventas,
              detalle: `${resultado.unidades} unidades`,
            },
            {
              etiqueta: 'Comisiones de cobro',
              valor: resultado.comisiones,
              clase: 'resta',
              detalle: 'Posnet, QR y transferencias',
            },
            { etiqueta: 'Ventas netas', valor: resultado.ventasNetas, clase: 'total' },
            {
              etiqueta: 'Costo de lo vendido',
              valor: resultado.costoDeLoVendido,
              clase: 'resta',
              detalle: 'Sólo la mercadería que salió, no la que compraste',
            },
            {
              etiqueta: 'Pérdidas',
              valor: resultado.mermas,
              clase: 'resta',
              detalle: 'Vencido, roto, consumo propio',
            },
            ...GASTOS_FIJOS.map((gasto) => ({
              etiqueta: gasto.concepto,
              valor: gasto.mensual,
              clase: 'resta' as const,
            })),
            { etiqueta: 'Ganancia de verdad', valor: resultado.ganancia, clase: 'total' },
          ]}
        />
        <div className="gn-columna">
          <div className="gn-equilibrio">
            <span className="gn-cifra-etiqueta">Punto de equilibrio</span>
            <strong>
              {resultado.equilibrioMensual === null
                ? 'No se llega'
                : `${pesos(resultado.equilibrioMensual / 30)} por día`}
            </strong>
            <p>
              Es lo que tenés que vender para pagar los gastos fijos. Vendés{' '}
              <strong>{pesos(resultado.ventas / 30)}</strong> por día.
            </p>
            {resultado.diaDeEquilibrio !== null && (
              <>
                <div
                  className="gn-barra"
                  role="img"
                  aria-label={`Gastos fijos del mes cubiertos al ${porcentaje(avance)}`}
                >
                  <i style={{ width: `${avance * 100}%` }} />
                  <b
                    style={{ left: `${Math.min(100, (resultado.diaDeEquilibrio / 30) * 100)}%` }}
                  />
                </div>
                <p className="gn-barra-texto">
                  A este ritmo cubrís los fijos el <strong>día {resultado.diaDeEquilibrio}</strong>{' '}
                  de cada mes. Hoy es {diaDelMes}:{' '}
                  {avance >= 1
                    ? 'desde hoy, lo que entra es ganancia.'
                    : `vas ${porcentaje(avance)}.`}
                </p>
              </>
            )}
          </div>
          <Cifra
            etiqueta="Vendido a fiado en el mes"
            valor={pesos(resultado.vendidoAFiado)}
            detalle="Está en las ventas, pero no en tu caja hasta que te paguen"
            tono="ojo"
          />
        </div>
      </div>
    </Seccion>
  );
}

const RIESGOS = { bajo: 'Al día', medio: 'Hace rato', alto: 'Atrasado' } as const;

function Fiado({ negocio, renglones }: { negocio: Negocio; renglones: Renglon[] }) {
  const cuentas = useMemo(
    () => fiadoConLaHoja(negocio.fiado, renglones),
    [negocio.fiado, renglones],
  );
  const [abierto, setAbierto] = useState<string | null>(null);
  const [enviados, setEnviados] = useState<string[]>([]);
  const avisar = useSistemaStore((estado) => estado.avisar);
  const total = cuentas.reduce((suma, cuenta) => suma + cuenta.debe, 0);
  const atrasado = cuentas
    .filter((cuenta) => cuenta.riesgo === 'alto')
    .reduce((suma, cuenta) => suma + cuenta.debe, 0);

  const mensaje = (cuenta: CuentaCorriente) =>
    `Hola, ${cuenta.cliente.nombre}. ¿Cómo andás? Te escribo de ${negocio.sistema.name}: te quedó pendiente ${pesos(cuenta.debe)}. Si te queda cómodo, lo podés pagar acá: mpago.la/ensambla-demo. ¡Gracias!`;

  const enviar = (cuenta: CuentaCorriente) => {
    setEnviados((lista) => [...lista, cuenta.cliente.id]);
    setAbierto(null);
    avisar(
      `Le mandamos el recordatorio a ${cuenta.cliente.nombre}. Cuando pague, el cobro entra solo en tu caja.`,
    );
  };

  return (
    <Seccion
      titulo="Fiado"
      bajada={
        <>
          Tenés <strong>{pesos(total)}</strong> en la calle
          {atrasado > 0 && (
            <>
              {' '}
              y <strong>{pesos(atrasado)}</strong> tienen más de 45 días
            </>
          )}
          . Vos elegís a quién recordarle.
        </>
      }
    >
      <ul className="gn-fiado">
        {cuentas.map((cuenta) => {
          const enviado = enviados.includes(cuenta.cliente.id);
          return (
            <li key={cuenta.cliente.id}>
              <div className="gn-fiado-fila">
                <span className="gn-fiado-quien">
                  <strong>{cuenta.cliente.nombre}</strong>
                  <small>
                    {cuenta.dias === 0 ? 'Desde hoy' : `Desde hace ${cuenta.dias} días`}
                  </small>
                </span>
                <span className="tag" data-riesgo={cuenta.riesgo}>
                  {RIESGOS[cuenta.riesgo]}
                </span>
                <strong className="gn-fiado-monto">{pesos(cuenta.debe)}</strong>
                {enviado ? (
                  <span className="gn-enviado">
                    <Icon name="check-circle" /> Recordatorio enviado
                  </span>
                ) : (
                  <Btn
                    cls="soft small"
                    aria-expanded={abierto === cuenta.cliente.id}
                    onClick={() =>
                      setAbierto(abierto === cuenta.cliente.id ? null : cuenta.cliente.id)
                    }
                  >
                    Recordarle
                  </Btn>
                )}
              </div>
              {abierto === cuenta.cliente.id && (
                <div className="gn-borrador">
                  <MensajeDeWhatsapp para={cuenta.cliente.nombre} texto={mensaje(cuenta)} />
                  <div className="gn-borrador-acciones">
                    <Btn cls="ghost small" onClick={() => setAbierto(null)}>
                      Ahora no
                    </Btn>
                    <Btn cls="primary small" onClick={() => enviar(cuenta)}>
                      Mandar por WhatsApp
                    </Btn>
                  </div>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </Seccion>
  );
}

function ResumenDelDomingo({ negocio }: { negocio: Negocio }) {
  const nombre = useNombre();
  const navegacion = useNavegacion();
  const { resultado, datos, hoy, fiado } = negocio;

  const texto = useMemo(() => {
    const urgente = faltantes(datos, hoy)[0];
    const deudor = fiado[0];
    const masVendidos = [...datos.productos]
      .map((producto) => ({
        producto,
        proyeccion: proyectar(datos, hoy, [supuestoDePrecio(producto, 0.1)]),
      }))
      .sort((a, b) => b.proyeccion.diferencia - a.proyeccion.diferencia);
    const precio = masVendidos[0];
    const lineas = [
      `Hola, ${nombre}. Tu semana en ${negocio.sistema.name}:`,
      `Vendiste ${pesos((resultado.ventas / 30) * 7)} y ganaste de verdad ${pesos((resultado.ganancia / 30) * 7)}.`,
      '',
      'Tres cosas para esta semana:',
      urgente
        ? `1) Pedir ${urgente.producto.nombre}: ${urgente.producto.stock === 0 ? 'se agotó' : `te dura ${Math.max(1, Math.floor(urgente.diasDeStock))} días`}. Ya armé el pedido a ${urgente.proveedor.nombre}.`
        : '1) Stock al día: no hace falta pedir nada.',
      deudor
        ? `2) Cobrarle a ${deudor.cliente.nombre}: te debe ${pesos(deudor.debe)} desde hace ${deudor.dias} días.`
        : '2) Nadie te debe: ¡bien!',
      precio && precio.proyeccion.diferencia > 0
        ? `3) Subir un 10 % ${precio.producto.nombre}: ganarías ${pesos(precio.proyeccion.diferencia)} más por mes.`
        : '3) Tus precios están bien para lo que se vende.',
      '',
      'Respondé 1, 2 o 3 y lo preparo.',
    ];
    return lineas.join('\n');
  }, [datos, fiado, hoy, negocio.sistema.name, nombre, resultado]);

  return (
    <Seccion
      titulo="El resumen del domingo"
      bajada="Sami te lo manda por WhatsApp. Respondés con un número y lo prepara."
      acciones={
        <div className="row">
          <Btn cls="ghost small" onClick={() => void navegacion.irACompras()}>
            Ver compras
          </Btn>
          <Btn cls="ghost small" onClick={() => void navegacion.irAProyeccion()}>
            Probar precios
          </Btn>
        </div>
      }
    >
      <MensajeDeWhatsapp para={nombre} texto={texto} />
    </Seccion>
  );
}
