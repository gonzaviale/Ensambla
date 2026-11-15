import { useState } from 'react';
import { ClienteCajaForm } from './ClienteCajaForm';
import { useSistemaStore } from '@/stores/sistemaStore';
import { allBlocks, makeBlock, normalize } from '@/lib/sistema/core';
import { clientesCaja, guardarClienteCaja } from '@/lib/clientesCaja';
import type { ErroresFila, Valor } from '@/lib/sistema/tipos';

export function SelectorClienteCaja({
  sistemaId,
  ventasId,
  clienteId,
  bloqueado,
  onCliente,
}: {
  sistemaId: string;
  ventasId: string;
  clienteId: string;
  bloqueado: boolean;
  onCliente: (id: string) => void;
}) {
  const sistema = useSistemaStore((s) => s.sistemas.find((item) => item.id === sistemaId));
  const [consulta, setConsulta] = useState('');
  const [creando, setCreando] = useState(false);
  const [nueva] = useState(() => makeBlock('clients'));
  if (!sistema) return null;
  const tablas = allBlocks(sistema);
  const destino = tablas
    .find((b) => b.id === ventasId)
    ?.fields.find((f) => f.key === 'customer')?.target;
  const tabla = tablas.find((b) => b.type === 'clients' && (!destino || b.id === destino));
  const clientes = clientesCaja(sistema).filter((c) => !destino || c.tabla.id === destino);
  const elegido = clientes.find((c) => c.fila.id === clienteId);
  const guardar = (valores: Record<string, Valor>): ErroresFila => {
    let errores: ErroresFila = {};
    let id = '';
    useSistemaStore.getState().actualizarSistema(sistemaId, (copia) => {
      const resultado = guardarClienteCaja(copia, tabla?.id ?? '', valores, null);
      errores = resultado.errores;
      id = resultado.cliente?.id ?? '';
    });
    if (id) {
      onCliente(id);
      setCreando(false);
      setConsulta('');
    }
    return errores;
  };
  return (
    <section className="caja-selector-cliente" aria-label="Cliente de la venta">
      {elegido && (
        <p role="status">
          Cliente: <strong>{String(elegido.fila.name)}</strong>
        </p>
      )}
      <label>
        Buscar cliente para la venta
        <input
          value={consulta}
          disabled={bloqueado}
          placeholder="Nombre o teléfono"
          onChange={(e) => setConsulta(e.target.value)}
        />
      </label>
      <div className="caja-opciones caja-opciones-clientes">
        {clientes
          .filter(({ fila }) =>
            normalize(`${fila.name} ${fila.phone ?? ''}`).includes(normalize(consulta)),
          )
          .map(({ fila }) => (
            <button
              key={fila.id}
              type="button"
              disabled={bloqueado}
              aria-pressed={clienteId === fila.id}
              onClick={() => onCliente(fila.id)}
            >
              {String(fila.name)}
              {fila.phone ? ` · ${String(fila.phone)}` : ''}
            </button>
          ))}
      </div>
      {!clientes.length && <p className="caja-ayuda">Todavía no hay clientes cargados.</p>}
      <button
        type="button"
        className="btn secondary"
        disabled={bloqueado}
        onClick={() => setCreando(true)}
      >
        Crear cliente
      </button>
      {creando && (
        <ClienteCajaForm
          sistema={sistema}
          tabla={tabla ?? nueva}
          onGuardar={guardar}
          onCerrar={() => setCreando(false)}
        />
      )}
    </section>
  );
}
