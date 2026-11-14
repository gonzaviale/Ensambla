import { useState } from 'react';
import { ClienteCajaForm } from './ClienteCajaForm';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import { allBlocks, makeBlock, normalize } from '@/lib/sistema/core';
import { clientesCaja, guardarClienteCaja } from '@/lib/clientesCaja';
import type { ErroresFila, Valor } from '@/lib/sistema/tipos';

export function ClientesCaja({ sistemaId }: { sistemaId: string }) {
  const sistema = useSistemaStore((s) => s.sistemas.find((item) => item.id === sistemaId));
  const pendiente = useSistemaStore((s) => s.pendientes.includes(sistemaId));
  const [consulta, setConsulta] = useState('');
  const [tablaElegida, setTablaElegida] = useState('');
  const [edicion, setEdicion] = useState<{ tablaId: string; clienteId: string | null } | null>(
    null,
  );
  const [tablaNueva] = useState(() => makeBlock('clients'));
  if (!sistema) return null;
  const tablas = allBlocks(sistema).filter((b) => b.type === 'clients');
  const tabla = tablas.find((b) => b.id === tablaElegida) ?? tablas[0];
  const tablaFormulario = tablas.find((b) => b.id === edicion?.tablaId) ?? tablaNueva;
  const cliente = tablaFormulario.rows.find((fila) => fila.id === edicion?.clienteId);
  const clientes = clientesCaja(sistema).filter(({ fila }) =>
    normalize(`${fila.name ?? ''} ${fila.phone ?? ''}`).includes(normalize(consulta)),
  );
  const guardar = (valores: Record<string, Valor>): ErroresFila => {
    if (!edicion) return {};
    let errores: ErroresFila = {};
    useSistemaStore.getState().actualizarSistema(sistemaId, (copia) => {
      errores = guardarClienteCaja(copia, edicion.tablaId, valores, edicion.clienteId).errores;
    });
    if (!Object.keys(errores).length) setEdicion(null);
    return errores;
  };
  return (
    <section className="caja-clientes" aria-label="Clientes de caja">
      <div className="caja-entre">
        <div>
          <h2>Clientes</h2>
          <p className="caja-ayuda">Los mismos contactos que usás en Mi Negocio.</p>
        </div>
        <button
          type="button"
          className="btn primary"
          onClick={() => setEdicion({ tablaId: tabla?.id ?? '', clienteId: null })}
        >
          <Icon name="plus" />
          Crear cliente
        </button>
      </div>
      {tablas.length > 1 && (
        <label>
          Guardar nuevos clientes en
          <select value={tabla?.id ?? ''} onChange={(e) => setTablaElegida(e.target.value)}>
            {tablas.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </label>
      )}
      <label>
        Buscar cliente
        <input
          type="search"
          value={consulta}
          onChange={(e) => setConsulta(e.target.value)}
          placeholder="Nombre o teléfono"
        />
      </label>
      <p className="caja-ayuda" role="status">
        {pendiente ? 'Guardando cambios de clientes…' : 'Clientes sincronizados'}
      </p>
      {!clientes.length && (
        <p className="caja-vacio">
          {consulta ? 'No encontramos clientes con esa búsqueda.' : 'Todavía no cargaste clientes.'}
        </p>
      )}
      <ul className="caja-clientes-lista">
        {clientes.map(({ tabla: origen, fila }) => (
          <li key={fila.id}>
            <div>
              <strong>{String(fila.name ?? '')}</strong>
              {fila.phone && <span>{String(fila.phone)}</span>}
              {tablas.length > 1 && <small>{origen.name}</small>}
            </div>
            <button
              type="button"
              className="btn secondary"
              aria-label={`Editar a ${String(fila.name)}`}
              onClick={() => setEdicion({ tablaId: origen.id, clienteId: fila.id })}
            >
              Editar
            </button>
          </li>
        ))}
      </ul>
      {edicion && (
        <ClienteCajaForm
          sistema={sistema}
          tabla={tablaFormulario}
          cliente={cliente}
          onGuardar={guardar}
          onCerrar={() => setEdicion(null)}
        />
      )}
    </section>
  );
}
