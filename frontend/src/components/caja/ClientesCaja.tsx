import { AntiguedadDeudaCaja } from './AntiguedadDeudaCaja';
import { useState } from 'react';
import { ClienteCajaForm } from './ClienteCajaForm';
import { CuentaClienteCaja } from './CuentaClienteCaja';
import { useCuentasCaja } from '@/hooks/caja/useCuentasCaja';
import { dineroCaja, fechaCaja, idCaja } from '@/lib/caja';
import { hayPagoDeudaPendiente } from '@/lib/cobrosDeudasCaja';
import { Icon } from '@/components/sistema/Icon';
import { useSistemaStore } from '@/stores/sistemaStore';
import { allBlocks, makeBlock, normalize } from '@/lib/sistema/core';
import { clientesCaja, guardarClienteCaja } from '@/lib/clientesCaja';
import type { ErroresFila, Valor, Bloque, Fila } from '@/lib/sistema/tipos';
import type { VentaCaja, CobroDeudaCaja } from '@/types/caja';
import { claseDeSeleccion, tonoDe, tonoPorPosicion, type Tono } from '@/lib/sistema/tonos';

const TONO_DE_LOS_FILTROS: Tono = 'petroleo';

type CuentaCliente = {
  id: string;
  nombre: string;
  telefono: string;
  origen?: { tabla: Bloque; fila: Fila };
  saldo: number;
  pendienteDesde: string | null;
  ventas: VentaCaja[];
  cobros: CobroDeudaCaja[];
};

export function ClientesCaja({
  sistemaId,
  inventarioId,
}: {
  sistemaId: string;
  inventarioId: string;
}) {
  const cuentas = useCuentasCaja(sistemaId, inventarioId);
  const usuarioId = useSistemaStore((s) => s.usuarioId);
  const sistema = useSistemaStore((s) => s.sistemas.find((item) => item.id === sistemaId));
  const pendiente = useSistemaStore((s) => s.pendientes.includes(sistemaId));
  const [consulta, setConsulta] = useState('');
  const [tablaElegida, setTablaElegida] = useState('');
  const [edicion, setEdicion] = useState<{ tablaId: string; clienteId: string | null } | null>(
    null,
  );
  const [tablaNueva] = useState(() => makeBlock('clients'));
  const [mostrarTodos, setMostrarTodos] = useState(false);
  const [cuentaAbierta, setCuentaAbierta] = useState<string | null>(null);
  if (!sistema) return null;
  const tablas = allBlocks(sistema).filter((b) => b.type === 'clients');
  const tabla = tablas.find((b) => b.id === tablaElegida) ?? tablas[0];
  const tablaFormulario = tablas.find((b) => b.id === edicion?.tablaId) ?? tablaNueva;
  const cliente = tablaFormulario.rows.find((fila) => fila.id === edicion?.clienteId);
  const porCliente = new Map<string, CuentaCliente>();
  for (const origen of clientesCaja(sistema)) {
    const id = idCaja(origen.fila.id);
    porCliente.set(id, {
      id,
      nombre: String(origen.fila.name ?? ''),
      telefono: String(origen.fila.phone ?? ''),
      origen,
      saldo: 0,
      pendienteDesde: null,
      ventas: [],
      cobros: [],
    });
  }
  for (const cuenta of cuentas.data?.cuentas ?? []) {
    const id = idCaja(cuenta.clienteId);
    const contacto = porCliente.get(id);
    porCliente.set(id, {
      id,
      nombre: cuenta.clienteNombre,
      telefono: '',
      ...contacto,
      saldo: cuenta.saldo,
      pendienteDesde: cuenta.pendienteDesde,
      ventas: cuenta.ventas,
      cobros: cuenta.cobros,
    });
  }
  const saldoDe = (cuenta: CuentaCliente) => Math.round(cuenta.saldo * 100);
  const clientes = [...porCliente.values()].filter(
    (cuenta) =>
      (mostrarTodos ||
        saldoDe(cuenta) > 0 ||
        hayPagoDeudaPendiente(usuarioId, sistemaId, inventarioId, cuenta.id)) &&
      normalize(`${cuenta.nombre} ${cuenta.telefono}`).includes(normalize(consulta)),
  );
  const conDeuda = [...porCliente.values()].filter((cuenta) => saldoDe(cuenta) > 0);
  const abierta = cuentaAbierta ? porCliente.get(cuentaAbierta) : undefined;
  const guardar = (valores: Record<string, Valor>): ErroresFila => {
    if (!edicion) return {};
    let errores: ErroresFila = {};
    useSistemaStore.getState().actualizarSistema(sistemaId, (copia) => {
      errores = guardarClienteCaja(copia, edicion.tablaId, valores, edicion.clienteId).errores;
    });
    if (!Object.keys(errores).length) {
      setEdicion(null);
      setMostrarTodos(true);
    }
    return errores;
  };
  return (
    <section className="caja-clientes" aria-label="Clientes de caja">
      <div className="caja-entre">
        <div>
          <h2>Clientes</h2>
          <p className="caja-ayuda">Los mismos contactos que usás en Mi negocio.</p>
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
      {cuentas.data && (
        <p className="caja-saldo solido" data-tono={tonoDe('fiado')}>
          Te deben{' '}
          <strong>{dineroCaja(conDeuda.reduce((sum, cuenta) => sum + saldoDe(cuenta), 0))}</strong>{' '}
          · {conDeuda.length} clientes en esta caja
        </p>
      )}
      {cuentas.isPending && <p role="status">Cargando las cuentas de clientes…</p>}
      {cuentas.error && (
        <p role="alert" className="caja-error">
          No pudimos cargar los saldos.
          <button type="button" className="btn secondary" onClick={() => void cuentas.refetch()}>
            Reintentar
          </button>
        </p>
      )}
      <div className="caja-opciones" aria-label="Filtrar clientes">
        <button
          type="button"
          className={claseDeSeleccion(!mostrarTodos)}
          data-tono={TONO_DE_LOS_FILTROS}
          aria-pressed={!mostrarTodos}
          onClick={() => setMostrarTodos(false)}
        >
          Con deuda
        </button>
        <button
          type="button"
          className={claseDeSeleccion(mostrarTodos)}
          data-tono={TONO_DE_LOS_FILTROS}
          aria-pressed={mostrarTodos}
          onClick={() => setMostrarTodos(true)}
        >
          Todos
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
        {pendiente ? 'Guardando cambios de clientes…' : 'Clientes guardados'}
      </p>
      {!clientes.length && (
        <p className="caja-vacio">
          {consulta
            ? 'No encontramos clientes con esa búsqueda.'
            : !mostrarTodos
              ? 'No hay clientes con fiado para mostrar. Podés mirar Todos.'
              : 'Todavía no cargaste clientes.'}
        </p>
      )}
      <ul className="caja-clientes-lista">
        {clientes.map((cuenta, posicion) => (
          <li key={cuenta.id}>
            <InicialDeCliente nombre={cuenta.nombre} posicion={posicion} />
            <div>
              <strong>{cuenta.nombre}</strong>
              <AntiguedadDeudaCaja ventas={cuenta.ventas} />
              {hayPagoDeudaPendiente(usuarioId, sistemaId, inventarioId, cuenta.id) && (
                <span>Hay un pago pendiente de verificar</span>
              )}
              {cuenta.telefono && <span>{cuenta.telefono}</span>}
              <span>
                {cuentas.data
                  ? saldoDe(cuenta) > 0
                    ? `Debe ${dineroCaja(saldoDe(cuenta))}`
                    : 'No debe nada'
                  : 'Saldo sin actualizar'}
              </span>
              {saldoDe(cuenta) > 0 && (
                <small>Pendiente desde {fechaCaja(cuenta.pendienteDesde ?? '')}</small>
              )}
              {tablas.length > 1 && cuenta.origen && <small>{cuenta.origen.tabla.name}</small>}
            </div>
            <div className="caja-opciones">
              <button
                type="button"
                className="btn petrol"
                disabled={!cuentas.data}
                aria-label={`Ver cuenta de ${cuenta.nombre}`}
                onClick={() => setCuentaAbierta(cuenta.id)}
              >
                Ver cuenta
              </button>
              {cuenta.origen && (
                <button
                  type="button"
                  className="btn secondary"
                  aria-label={`Editar a ${cuenta.nombre}`}
                  onClick={() =>
                    setEdicion({
                      tablaId: cuenta.origen?.tabla.id ?? '',
                      clienteId: cuenta.origen?.fila.id ?? null,
                    })
                  }
                >
                  Editar
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
      {abierta && (
        <CuentaClienteCaja
          sistemaId={sistemaId}
          inventarioId={inventarioId}
          clienteId={abierta.id}
          nombre={abierta.nombre}
          ventas={abierta.ventas}
          cobros={abierta.cobros}
          onCerrar={() => setCuentaAbierta(null)}
        />
      )}
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

function InicialDeCliente({ nombre, posicion }: { nombre: string; posicion: number }) {
  return (
    <span
      className="caja-cliente-inicial solido"
      data-tono={tonoPorPosicion(posicion)}
      aria-hidden="true"
    >
      {inicialDe(nombre)}
    </span>
  );
}

const inicialDe = (nombre: string) => nombre.trim().charAt(0).toUpperCase() || '?';
