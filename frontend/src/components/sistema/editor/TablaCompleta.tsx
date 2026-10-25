import { useEffect, useMemo, useRef, useState } from 'react';
import { CATALOG } from '@/lib/sistema/catalogo';
import {
  filtrarFilas,
  hayFiltros,
  type FiltroDeColumna as Filtro,
  type Filtros,
} from '@/lib/sistema/filtros';
import { textoPlano } from '@/lib/sistema/formato';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { IconTile } from '../IconTile';
import { ValorDeCampo } from '../ValorDeCampo';
import { FiltroDeColumna } from './FiltroDeColumna';

const BAJO_STOCK = 'low-stock';

const ENFOCABLES = 'button:not(:disabled),input:not([hidden]),select,textarea,[tabindex="0"]';

// La tabla de un bloque a toda la pantalla: más lugar para leerla, y filtros por columna además de la búsqueda.
export function TablaCompleta({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  const verTablaCompleta = useSistemaStore((estado) => estado.verTablaCompleta);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const [consulta, setConsulta] = useState('');
  const [filtros, setFiltros] = useState<Filtros>({});
  const raiz = useRef<HTMLDivElement>(null);
  const cerrar = () => verTablaCompleta(null);

  const filas = useMemo(
    () => filtrarFilas(sistema, bloque, consulta, filtros),
    [sistema, bloque, consulta, filtros],
  );
  const filtrando = hayFiltros(consulta, filtros);

  // Cada lápiz dice a qué registro corresponde (por su primera columna), para distinguirlos con el lector de pantalla.
  const [primerCampo] = bloque.fields;
  const rotuloDeEdicion = (fila: Fila) => {
    const nombre = primerCampo ? textoPlano(sistema, primerCampo, fila[primerCampo.key]) : '';
    return nombre ? `Editar registro: ${nombre}` : 'Editar registro';
  };

  const cambiarFiltro = (clave: string, cambio: Partial<Filtro>) =>
    setFiltros((actuales) => ({ ...actuales, [clave]: { ...actuales[clave], ...cambio } }));
  const limpiar = () => {
    setConsulta('');
    setFiltros({});
  };

  // La pantalla de atrás no se desplaza (ni deja su barra a la vista) mientras la tabla está abierta. Se bloquea
  // `html` y no `body`, que es lo que toca `ModalHost` al abrir el formulario de un registro encima.
  useEffect(() => {
    const documento = document.documentElement;
    const previo = documento.style.overflow;
    documento.style.overflow = 'hidden';
    return () => {
      documento.style.overflow = previo;
    };
  }, []);

  // Esc cierra y Tab no sale de la vista; mientras hay un formulario abierto encima, eso le toca a él. Al cerrar, el foco
  // vuelve al botón que la abrió.
  const cierre = useRef(cerrar);
  useEffect(() => {
    cierre.current = cerrar;
  });
  useEffect(() => {
    const previo = document.activeElement as HTMLElement | null;
    const alTeclear = (evento: KeyboardEvent) => {
      if (useSistemaStore.getState().modal || !raiz.current) return;
      if (evento.key === 'Escape') {
        evento.stopPropagation();
        cierre.current();
        return;
      }
      if (evento.key !== 'Tab') return;
      const nodos = [...raiz.current.querySelectorAll<HTMLElement>(ENFOCABLES)];
      const inicio = nodos[0];
      const fin = nodos.at(-1);
      if (evento.shiftKey && document.activeElement === inicio) {
        evento.preventDefault();
        fin?.focus();
      } else if (!evento.shiftKey && document.activeElement === fin) {
        evento.preventDefault();
        inicio?.focus();
      }
    };
    raiz.current?.querySelector<HTMLElement>('[data-tabla-completa-volver]')?.focus();
    document.addEventListener('keydown', alTeclear, true);
    return () => {
      document.removeEventListener('keydown', alTeclear, true);
      if (previo?.isConnected) previo.focus();
    };
  }, []);

  return (
    <div className="tabla-completa" ref={raiz}>
      <section
        className="tabla-completa-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tabla-completa-titulo"
      >
        <header className="tabla-completa-cabecera">
          <IconTile name={CATALOG[bloque.type].icon} color={bloque.color} />
          <div className="tabla-completa-titulo">
            <h2 id="tabla-completa-titulo">{bloque.name}</h2>
            <p aria-live="polite">
              {filtrando
                ? `Mostrando ${filas.length} de ${bloque.rows.length} registros`
                : `${bloque.rows.length} registros`}
            </p>
          </div>
          <Btn
            cls="soft small"
            onClick={() => abrirModal({ tipo: 'registro', bloqueId: bloque.id })}
          >
            <Icon name="plus" /> Registrar
          </Btn>
          <Btn cls="secondary small" data-tabla-completa-volver="true" onClick={cerrar}>
            <Icon name="back" /> Volver al sistema
          </Btn>
        </header>

        <div className="tabla-completa-barra">
          <label className="searchbox">
            <Icon name="search" />
            <input
              className="input"
              type="search"
              placeholder={`Buscar en ${bloque.name.toLowerCase()}`}
              aria-label={`Buscar en ${bloque.name}`}
              value={consulta}
              onChange={(evento) => setConsulta(evento.target.value)}
            />
          </label>
          <Btn cls="ghost small" disabled={!filtrando} onClick={limpiar}>
            <Icon name="close" /> Limpiar filtros
          </Btn>
        </div>

        <div className="tabla-completa-tabla">
          <table className="data-table">
            <thead>
              <tr>
                {bloque.fields.map((campo) => (
                  <th scope="col" key={campo.key}>
                    {campo.label}
                  </th>
                ))}
                <th scope="col">
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
              <tr className="filtros-fila">
                {bloque.fields.map((campo) => (
                  <th key={campo.key}>
                    <FiltroDeColumna
                      campo={campo}
                      filtro={filtros[campo.key] ?? {}}
                      onCambio={(cambio) => cambiarFiltro(campo.key, cambio)}
                    />
                  </th>
                ))}
                <th />
              </tr>
            </thead>
            <tbody>
              {filas.map((fila) => (
                <tr key={fila.id} data-record={fila.id}>
                  {bloque.fields.map((campo) => (
                    <td
                      key={campo.key}
                      title={textoPlano(sistema, campo, fila[campo.key])}
                      className={
                        bloque.type === 'inventory' &&
                        campo.key === 'quantity' &&
                        Number(fila.quantity) <= Number(fila.minimum)
                          ? BAJO_STOCK
                          : ''
                      }
                    >
                      <ValorDeCampo sistema={sistema} campo={campo} valor={fila[campo.key]} />
                    </td>
                  ))}
                  <td className="row-actions">
                    <IconBtn
                      name="edit"
                      label={rotuloDeEdicion(fila)}
                      onClick={() =>
                        abrirModal({ tipo: 'registro', bloqueId: bloque.id, filaId: fila.id })
                      }
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!filas.length && (
            <p className="tabla-completa-vacia">
              {bloque.rows.length
                ? 'Ningún registro coincide con la búsqueda y los filtros.'
                : 'Todavía no hay registros.'}
            </p>
          )}
        </div>
      </section>
    </div>
  );
}
