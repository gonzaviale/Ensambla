import { useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { MarcoDeNegocio } from '@/components/gestion/MarcoDeNegocio';
import {
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
  HOJA_DEL_DIA,
  type Renglon,
  type TipoDeRenglon,
} from '@/lib/gestion/datosDeEjemplo';
import { pesos, porcentaje } from '@/lib/gestion/formato';
import { proyectar } from '@/lib/gestion/proyeccion';
import { cierreDelDia, fiadoConLaHoja, type CuentaCorriente } from '@/lib/gestion/resultado';
import type { Negocio } from '@/hooks/gestion/useNegocio';
import { useNavegacion, useNombre } from '@/hooks/sistemas/useNavegacion';
import { useSistemaStore } from '@/stores/sistemaStore';

const TIPOS: Record<TipoDeRenglon, string> = {
  venta: 'Venta',
  fiado: 'Fiado',
  cobro: 'Cobro de fiado',
  pago: 'Pago a proveedor',
  gasto: 'Gasto',
  merma: 'Pérdida',
};

type Paso = 'foto' | 'leyendo' | 'revisar' | 'cerrado';

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
  const [paso, setPaso] = useState<Paso>('foto');
  const [renglones, setRenglones] = useState<Renglon[]>(HOJA_DEL_DIA);
  const [foto, setFoto] = useState<string | null>(null);

  useEffect(() => {
    if (paso !== 'leyendo') return;
    const lectura = window.setTimeout(() => setPaso('revisar'), 1400);
    return () => window.clearTimeout(lectura);
  }, [paso]);

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

  const cambiarTipo = (id: string, tipo: TipoDeRenglon) =>
    setRenglones((lista) =>
      lista.map((renglon) => (renglon.id === id ? { ...renglon, tipo, dudoso: false } : renglon)),
    );

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
                <strong>Seguí anotando como siempre.</strong> Al final del día sacale una foto a la
                hoja y yo separo ventas, fiado, cobros, pagos y pérdidas. Vos revisás antes de
                cerrar.
              </p>
            </DiceSami>
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
                Leí <strong>{renglones.length} renglones</strong>. Marqué en amarillo el que no
                tengo claro. Si me equivoqué en alguno, cambialo antes de cerrar.
              </p>
            </DiceSami>
            <ul className="gn-renglones">
              {renglones.map((renglon) => (
                <li key={renglon.id} data-dudoso={renglon.dudoso || undefined}>
                  <span className="gn-escrito">{renglon.escrito}</span>
                  <Icon name="arrow" />
                  <span className="gn-entendido">
                    <strong>{renglon.detalle}</strong>
                    <small>{pesos(renglon.importe)}</small>
                  </span>
                  <select
                    aria-label={`Qué es «${renglon.escrito}»`}
                    value={renglon.tipo}
                    onChange={(evento) =>
                      cambiarTipo(renglon.id, evento.target.value as TipoDeRenglon)
                    }
                  >
                    {Object.entries(TIPOS).map(([clave, etiqueta]) => (
                      <option key={clave} value={clave}>
                        {etiqueta}
                      </option>
                    ))}
                  </select>
                </li>
              ))}
            </ul>
            <div className="gn-revision-pie">
              <Btn cls="ghost" onClick={() => setPaso('foto')}>
                Otra foto
              </Btn>
              <Btn cls="primary" onClick={() => setPaso('cerrado')}>
                <Icon name="check" /> Cerrar el día
              </Btn>
            </div>
          </div>
        )}

        {cerrado && <ElDia renglones={renglones} margenDelMes={margenDelMes} />}
      </Seccion>

      <ElMes negocio={negocio} />

      <Fiado negocio={negocio} renglones={cerrado ? renglones : []} />

      <ResumenDelDomingo negocio={negocio} />
    </>
  );
}

function ElDia({ renglones, margenDelMes }: { renglones: Renglon[]; margenDelMes: number }) {
  const cierre = cierreDelDia(renglones, margenDelMes);
  const [contado, setContado] = useState('');
  const diferencia = contado === '' ? null : Number(contado) - cierre.cajon;

  return (
    <div className="gn-dia">
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
              detalle: 'El aceite roto',
            },
            { etiqueta: 'Gastos del día', valor: cierre.gastos, clase: 'resta', detalle: 'Bolsas' },
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
            <input
              type="number"
              inputMode="numeric"
              placeholder={String(Math.round(cierre.cajon))}
              value={contado}
              onChange={(evento) => setContado(evento.target.value)}
            />
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
        proyeccion: proyectar(datos, hoy, { tipo: 'precio', productoId: producto.id, cambio: 0.1 }),
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
