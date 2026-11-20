import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { SelectorClienteCaja } from './SelectorClienteCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';
import { dineroCaja, leerPrecio } from '@/lib/caja';
import { clientesCaja } from '@/lib/clientesCaja';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { MedioCaja, PagoCaja, SolicitudCobro } from '@/types/caja';
import { tonoDe } from '@/lib/sistema/tonos';
import { aspectoDelMedio, iconoDelMedio, nombreDelMedio } from '@/lib/caja/mediosDePago';
import {
  ESTADOS_DE_LA_VENTA,
  aspectoDelEstado,
  type EstadoDeLaVenta,
} from '@/lib/caja/estadosDeLaVenta';

const medios: MedioCaja[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA'];

export function CobroCaja({
  total,
  sistemaId,
  ventasId,
  ocupada,
  incierta,
  error,
  onConfirmar,
  onCerrar,
}: {
  total: number;
  sistemaId: string;
  ventasId: string;
  ocupada: boolean;
  incierta: boolean;
  error: string;
  onConfirmar: (
    medio: SolicitudCobro['medio'],
    recibido: number | null,
    pagos?: PagoCaja[],
    clienteId?: string,
  ) => void;
  onCerrar: () => void;
}) {
  const [medio, setMedio] = useState<MedioCaja>('EFECTIVO');
  const [segundo, setSegundo] = useState<MedioCaja>('TRANSFERENCIA');
  const [dividido, setDividido] = useState(false);
  const [primeraParte, setPrimeraParte] = useState('');
  const [recibido, setRecibido] = useState('');
  const [tipo, setTipo] = useState<EstadoDeLaVenta>('pagada');
  const [importeParcial, setImporteParcial] = useState('');
  const [clienteId, setClienteId] = useState('');
  const [eligiendoCliente, setEligiendoCliente] = useState(false);
  const pendiente = useSistemaStore((s) => s.pendientes.includes(sistemaId));
  const sistema = useSistemaStore((s) => s.sistemas.find((item) => item.id === sistemaId));
  const cliente = clientesCaja(sistema).find((c) => c.fila.id === clienteId);
  const cambios = useConfirmarCambios();
  const solicitarCierre = async () => {
    if (ocupada || incierta) return;
    if (
      !(dividido || recibido || medio !== 'EFECTIVO' || tipo !== 'pagada' || clienteId) ||
      (await cambios.confirmar(
        'Se descartarán los datos de este pago. La compra seguirá en Caja para que puedas cobrarla después.',
      ))
    )
      onCerrar();
  };
  let aCobrar = total;
  let importeValido = true;
  try {
    if (tipo === 'parcial') aCobrar = leerPrecio(importeParcial);
    if (tipo === 'fiada') aCobrar = 0;
  } catch {
    importeValido = false;
  }
  if (tipo === 'parcial' && aCobrar >= total) importeValido = false;
  let parte = aCobrar,
    entregado = 0,
    valido = importeValido && (tipo === 'pagada' || !!clienteId);
  try {
    if (dividido && tipo !== 'fiada') parte = leerPrecio(primeraParte);
  } catch {
    valido = false;
  }
  if (dividido && tipo !== 'fiada' && (parte >= aCobrar || segundo === medio)) valido = false;
  const restante = aCobrar - parte;
  const efectivo = medio === 'EFECTIVO' ? parte : dividido && segundo === 'EFECTIVO' ? restante : 0;
  try {
    entregado = recibido ? leerPrecio(recibido) : efectivo;
  } catch {
    if (efectivo > 0) valido = false;
  }
  if (efectivo > 0 && entregado < efectivo) valido = false;
  const bloqueado = ocupada || incierta;
  const confirmar = () => {
    const pagos: PagoCaja[] =
      tipo === 'fiada'
        ? []
        : dividido
          ? [
              { medio, importe: parte / 100 },
              { medio: segundo, importe: restante / 100 },
            ]
          : [{ medio, importe: aCobrar / 100 }];
    onConfirmar(
      dividido && tipo !== 'fiada' ? 'MIXTO' : medio,
      efectivo > 0 && recibido ? entregado / 100 : null,
      pagos,
      clienteId || undefined,
    );
  };
  return (
    <>
      <DialogoCaja
        titulo={incierta ? 'Verificar la venta' : 'Guardar el cobro'}
        clase="caja-dialogo-cobro"
        tono={tonoDe('ventas')}
        onCerrar={() => void solicitarCierre()}
        bloqueado={bloqueado}
      >
        <div className="caja-cobro-cuerpo">
          <div className="caja-cobro-importe">
            <div>
              <span className="caja-antetitulo">TOTAL DE LA VENTA</span>
              <p className="caja-importe-previo">{dineroCaja(total)}</p>
            </div>
            {tipo !== 'pagada' && (
              <div className="caja-cobro-saldo" role="status">
                <span>Queda pendiente</span>
                <strong>{importeValido ? dineroCaja(total - aCobrar) : 'Revisá el importe'}</strong>
              </div>
            )}
          </div>
          <fieldset disabled={bloqueado}>
            <legend className="sr-only">Cómo queda la venta</legend>
            <div className="caja-opciones">
              {ESTADOS_DE_LA_VENTA.map((estado) => (
                <button
                  key={estado.valor}
                  type="button"
                  {...aspectoDelEstado(estado, tipo === estado.valor)}
                  aria-pressed={tipo === estado.valor}
                  onClick={() => {
                    setTipo(estado.valor);
                    setRecibido('');
                    setDividido(false);
                  }}
                >
                  {estado.texto}
                </button>
              ))}
            </div>
          </fieldset>
          <div className="caja-cobro-herramientas">
            <button
              type="button"
              className="btn secondary caja-cobro-cliente"
              disabled={bloqueado}
              aria-label={
                cliente
                  ? 'Cliente: ' + String(cliente.fila.name)
                  : tipo === 'pagada'
                    ? 'Agregar cliente (opcional)'
                    : 'Elegir quién debe'
              }
              title={cliente ? String(cliente.fila.name) : undefined}
              onClick={() => setEligiendoCliente(true)}
            >
              <Icon name="users" />
              {cliente
                ? String(cliente.fila.name)
                : tipo === 'pagada'
                  ? 'Cliente opcional'
                  : 'Elegir cliente'}
            </button>
            {tipo !== 'fiada' && (
              <button
                type="button"
                className="btn secondary caja-dividir"
                aria-label={dividido ? 'Usar un solo medio' : 'Dividir pago'}
                aria-pressed={dividido}
                disabled={bloqueado}
                onClick={() => {
                  setDividido(!dividido);
                  setRecibido('');
                }}
              >
                {dividido ? 'Pago mixto' : 'Dividir pago'}
              </button>
            )}
          </div>
          {tipo !== 'fiada' && (
            <fieldset disabled={bloqueado}>
              <legend className="sr-only">{dividido ? 'Primer medio' : 'Medio de pago'}</legend>
              <div className="caja-opciones">
                {medios.map((valor) => (
                  <button
                    key={valor}
                    type="button"
                    {...aspectoDelMedio(valor, medio === valor)}
                    aria-pressed={medio === valor}
                    onClick={() => {
                      setMedio(valor);
                      if (segundo === valor)
                        setSegundo(medios.find((m) => m !== valor) ?? 'TRANSFERENCIA');
                      setRecibido('');
                    }}
                  >
                    <Icon name={iconoDelMedio(valor)} />
                    {nombreDelMedio(valor)}
                  </button>
                ))}
              </div>
            </fieldset>
          )}
          <div className="caja-cobro-importes">
            {tipo === 'parcial' && (
              <label>
                Paga ahora ($)
                <input
                  inputMode="decimal"
                  value={importeParcial}
                  disabled={bloqueado}
                  onChange={(e) => setImporteParcial(e.target.value)}
                  placeholder="0,00"
                />
              </label>
            )}
            {dividido && tipo !== 'fiada' && (
              <>
                <label>
                  {nombreDelMedio(medio)} ($)
                  <input
                    aria-label={'Importe en ' + nombreDelMedio(medio).toLowerCase() + ' ($)'}
                    inputMode="decimal"
                    value={primeraParte}
                    disabled={bloqueado}
                    onChange={(e) => setPrimeraParte(e.target.value)}
                    placeholder="0,00"
                  />
                </label>
                <label>
                  El resto con
                  <select
                    aria-label="El resto se paga con"
                    value={segundo}
                    disabled={bloqueado}
                    onChange={(e) => {
                      setSegundo(e.target.value as MedioCaja);
                      setRecibido('');
                    }}
                  >
                    {medios
                      .filter((m) => m !== medio)
                      .map((valor) => (
                        <option key={valor} value={valor}>
                          {nombreDelMedio(valor)}
                        </option>
                      ))}
                  </select>
                </label>
              </>
            )}
            {efectivo > 0 && (
              <label>
                Recibido ($)
                <input
                  aria-label="Efectivo recibido ($)"
                  inputMode="decimal"
                  value={recibido}
                  disabled={bloqueado}
                  placeholder="Importe exacto"
                  onChange={(e) => setRecibido(e.target.value)}
                />
              </label>
            )}
          </div>
          <div className="caja-cobro-estado" role="status" data-valido={valido}>
            {dividido && tipo !== 'fiada' && (
              <span>
                {restante > 0
                  ? nombreDelMedio(segundo) + ': ' + dineroCaja(restante)
                  : 'Las dos partes deben ser mayores a cero.'}
              </span>
            )}
            <span>
              {valido
                ? efectivo > 0
                  ? 'Vuelto: ' + dineroCaja(entregado - efectivo)
                  : tipo === 'fiada'
                    ? 'Queda fiado a nombre del cliente.'
                    : 'Confirmá que recibiste el pago.'
                : 'Revisá los importes y el cliente.'}
            </span>
          </div>
          {error && (
            <p className="caja-error" role="alert">
              {error}
            </p>
          )}
          {incierta && (
            <p className="caja-nota">
              Verificá la misma operación para saber si se registró. No vuelvas a cobrarle a la
              persona.
            </p>
          )}
        </div>
        <div className="caja-acciones">
          <button
            type="button"
            className="btn secondary"
            disabled={bloqueado}
            onClick={() => void solicitarCierre()}
          >
            Volver
          </button>
          <button
            type="button"
            className="btn primary"
            disabled={ocupada || pendiente || (!incierta && !valido)}
            onClick={confirmar}
          >
            {ocupada
              ? 'Confirmando…'
              : incierta
                ? 'Verificar misma operación'
                : tipo === 'fiada'
                  ? 'Guardar fiado'
                  : tipo === 'parcial'
                    ? 'Guardar venta y pago'
                    : 'Confirmar cobro'}
          </button>
        </div>
      </DialogoCaja>
      {eligiendoCliente && (
        <DialogoCaja titulo="Cliente de la venta" onCerrar={() => setEligiendoCliente(false)}>
          <SelectorClienteCaja
            sistemaId={sistemaId}
            ventasId={ventasId}
            clienteId={clienteId}
            bloqueado={bloqueado}
            onCliente={(id) => {
              setClienteId(id);
              setEligiendoCliente(false);
            }}
          />
        </DialogoCaja>
      )}
      {cambios.confirmacion && (
        <ConfirmacionCambios
          mensaje={cambios.confirmacion}
          onConservar={cambios.conservar}
          onDescartar={cambios.aceptar}
        />
      )}
    </>
  );
}
