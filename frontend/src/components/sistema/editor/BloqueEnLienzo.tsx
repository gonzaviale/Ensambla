import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type DragEvent,
  type FormEvent,
  type PointerEvent,
} from 'react';
import type { CargaDeArrastre, LadoDelCambio } from '@/hooks/sistemas/useArrastreDelEditor';
import { useTandas } from '@/hooks/sistemas/useTandas';
import { CATALOG } from '@/lib/sistema/catalogo';
import { normalize, totals } from '@/lib/sistema/core';
import { textoPlano } from '@/lib/sistema/formato';
import { alPedirFila, TANDA_DE_REGISTROS } from '@/lib/sistema/tandas';
import type { Ancho, Bloque, Sistema } from '@/lib/sistema/tipos';
import {
  cambiarAncho,
  duplicarBloque,
  moverBloque,
  pedirQuitarBloque,
  guardarRegistro,
} from '@/stores/accionesDelEditor';
import { useSistemaStore, type ModoDelEditor } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { varsDeColor } from '../colores';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { IconTile } from '../IconTile';
import { ValorDeCampo } from '../ValorDeCampo';
import { WidthButtons } from '../WidthButtons';
import { CampoDeFormulario } from './CampoDeFormulario';
import { enfocarPrimerError, leerValores } from './leerFormulario';

type BloqueEnLienzoProps = {
  bloque: Bloque;
  sistema: Sistema;
  modo: ModoDelEditor;
  seleccionado: boolean;
  primero: boolean;
  ultimo: boolean;
  arrastrando: boolean;
  sobre: boolean;
  /** Si otro bloque se suelta encima, este se corre hacia ese lado: ahí se dibuja la barra. */
  ladoDelCambio?: LadoDelCambio;
  /** Ancho que se está probando con el tirador, antes de soltarlo. */
  anchoProvisorio?: number;
  onArrastrar: (evento: DragEvent<HTMLElement>, carga: CargaDeArrastre) => void;
  onFinArrastre: () => void;
  onTirador: (evento: PointerEvent<HTMLButtonElement>, bloque: Bloque, nodo: HTMLElement) => void;
};

const ENCABEZADOS_BAJOS_STOCK = 'low-stock';

function Resumen({ sistema }: { sistema: Sistema }) {
  const total = totals(sistema);
  const indicadores: [number, string][] = [
    [total.records, 'Registros en tu cuaderno'],
    [total.pending, 'Pedidos pendientes'],
    [total.stock, 'Unidades en inventario'],
    [total.clients, 'Clientes registrados'],
  ];
  return (
    <div className="summary-block">
      {indicadores.map(([valor, etiqueta]) => (
        <div key={etiqueta}>
          <div className="summary-value">{valor}</div>
          <div className="summary-label">{etiqueta}</div>
        </div>
      ))}
    </div>
  );
}

// La tabla del bloque: los registros se dibujan de a 10 (con el scroll del propio bloque) y la búsqueda filtra sobre
// todos. Un cuaderno con cientos de registros no estira la página ni dibuja de más.
function Registros({
  bloque,
  sistema,
  consulta,
}: {
  bloque: Bloque;
  sistema: Sistema;
  consulta: string;
}) {
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const buscado = normalize(consulta);
  const coincidentes = useMemo(
    () =>
      buscado
        ? bloque.rows.filter((fila) =>
            normalize(
              bloque.fields.map((campo) => textoPlano(sistema, campo, fila[campo.key])).join(' '),
            ).includes(buscado),
          )
        : bloque.rows,
    [bloque.rows, bloque.fields, sistema, buscado],
  );
  const { visibles, quedan, sumar, mostrarHasta, alDesplazar } = useTandas(
    coincidentes.length,
    TANDA_DE_REGISTROS,
    buscado,
  );

  // Si piden llevar la vista a un registro que todavía no se dibujó, se dibujan los que hagan falta hasta él.
  useEffect(
    () =>
      alPedirFila(({ bloqueId, filaId }) => {
        if (bloqueId !== bloque.id) return;
        const indice = coincidentes.findIndex((fila) => fila.id === filaId);
        if (indice >= 0) mostrarHasta(indice);
      }),
  );

  if (!bloque.rows.length) {
    return (
      <div className="data-empty">
        <Icon name={CATALOG[bloque.type].icon} />
        Todavía no hay registros.
        <br />
        <button
          type="button"
          className="text-link"
          data-accion="add-record"
          onClick={() => abrirModal({ tipo: 'registro', bloqueId: bloque.id })}
        >
          Cargá el primero <Icon name="arrow" />
        </button>
      </div>
    );
  }
  const filas = coincidentes.slice(0, visibles);
  return (
    <>
      <div className="table-scroll" onScroll={alDesplazar}>
        <table className="data-table">
          <thead>
            <tr>
              {bloque.fields.map((campo) => (
                <th scope="col" key={campo.key}>
                  {campo.label}
                  {campo.type === 'relation' ? ' ↗' : ''}
                </th>
              ))}
              <th scope="col">
                <span className="sr-only">Acciones</span>
              </th>
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
                        ? ENCABEZADOS_BAJOS_STOCK
                        : ''
                    }
                  >
                    <ValorDeCampo sistema={sistema} campo={campo} valor={fila[campo.key]} />
                  </td>
                ))}
                <td className="row-actions">
                  <IconBtn
                    name="edit"
                    label="Editar registro"
                    data-accion="edit-record"
                    onClick={() =>
                      abrirModal({ tipo: 'registro', bloqueId: bloque.id, filaId: fila.id })
                    }
                  />
                </td>
              </tr>
            ))}
            {!filas.length && (
              <tr>
                <td colSpan={bloque.fields.length + 1}>
                  <span className="empty-preview">No hay registros que coincidan.</span>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {quedan > 0 && (
        <button type="button" className="tabla-mas" onClick={sumar}>
          Mostrar {Math.min(TANDA_DE_REGISTROS, quedan)} más · quedan {quedan}
        </button>
      )}
    </>
  );
}

function FormularioEnLinea({ bloque, sistema }: { bloque: Bloque; sistema: Sistema }) {
  const formulario = useRef<HTMLFormElement>(null);
  const [errores, setErrores] = useState<Record<string, string>>({});

  const guardar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const resultado = guardarRegistro(bloque.id, leerValores(evento.currentTarget, bloque), null);
    if (resultado) {
      setErrores(resultado);
      requestAnimationFrame(() => enfocarPrimerError(formulario.current));
      return;
    }
    setErrores({});
    formulario.current?.reset();
  };

  return (
    <form ref={formulario} className="inline-form" onSubmit={guardar} noValidate>
      {bloque.fields.map((campo) => (
        <CampoDeFormulario
          key={campo.key}
          sistema={sistema}
          campo={campo}
          valor=""
          prefijo={bloque.id}
          error={errores[campo.key]}
        />
      ))}
      <button className="btn primary" type="submit">
        <Icon name="plus" />
        Guardar registro
      </button>
      <div className="form-error" role="alert">
        {errores._form}
      </div>
    </form>
  );
}

// Un bloque del lienzo, en modo Construir (con herramientas, ancho y tirador) o Usar.
export function BloqueEnLienzo({
  bloque,
  sistema,
  modo,
  seleccionado,
  primero,
  ultimo,
  arrastrando,
  sobre,
  ladoDelCambio,
  anchoProvisorio,
  onArrastrar,
  onFinArrastre,
  onTirador,
}: BloqueEnLienzoProps) {
  const definicion = CATALOG[bloque.type];
  const seleccionar = useSistemaStore((estado) => estado.seleccionar);
  const abrirModal = useSistemaStore((estado) => estado.abrirModal);
  const verTablaCompleta = useSistemaStore((estado) => estado.verTablaCompleta);
  const [consulta, setConsulta] = useState('');
  const nodo = useRef<HTMLElement>(null);

  const estilo: CSSProperties = {
    ...varsDeColor(bloque.color),
    ...(anchoProvisorio ? { gridColumn: `span ${anchoProvisorio}` } : {}),
  };

  return (
    <section
      ref={nodo}
      className={`block ${seleccionado ? 'selected' : ''} ${arrastrando ? 'dragging' : ''} ${sobre && !ladoDelCambio ? 'drag-over' : ''} ${ladoDelCambio ? `cambio-${ladoDelCambio}` : ''}`
        .replace(/\s+/g, ' ')
        .trim()}
      style={estilo}
      data-block={bloque.id}
      data-tour="done"
      data-tour-id={bloque.id}
      data-width={bloque.width}
      data-color={bloque.color}
      aria-label={bloque.name}
    >
      <header className="block-head">
        <IconTile name={definicion.icon} color={bloque.color} />
        <div className="block-title">
          <h2>{bloque.name}</h2>
          <p>{bloque.description}</p>
        </div>
        {modo === 'build' && (
          <div className="block-tools" data-tour="move" data-tour-id={bloque.id}>
            <button
              type="button"
              className="icon-btn"
              title="Arrastrar bloque"
              aria-label="Arrastrar bloque"
              draggable
              data-accion="noop"
              data-drag-kind="block"
              data-value={bloque.id}
              onDragStart={(evento) => onArrastrar(evento, { tipo: 'bloque', valor: bloque.id })}
              onDragEnd={onFinArrastre}
            >
              <Icon name="grip" />
            </button>
            <IconBtn
              name="up"
              label="Mover arriba"
              data-accion="move-up"
              disabled={primero}
              onClick={() => moverBloque(bloque.id, 'arriba')}
            />
            <IconBtn
              name="down"
              label="Mover abajo"
              data-accion="move-down"
              disabled={ultimo}
              onClick={() => moverBloque(bloque.id, 'abajo')}
            />
            <IconBtn
              name="settings"
              label="Personalizar bloque"
              data-accion="select-block"
              data-tour="customize"
              data-tour-id={bloque.id}
              onClick={() => seleccionar(bloque.id)}
            />
            <IconBtn
              name="copy"
              label="Duplicar bloque"
              data-accion="duplicate-block"
              onClick={() => duplicarBloque(bloque.id)}
            />
            <IconBtn
              name="trash"
              label="Quitar bloque"
              data-accion="delete-block"
              onClick={() => pedirQuitarBloque(bloque.id)}
            />
          </div>
        )}
      </header>
      {modo === 'build' && (
        <div className="block-width-row">
          <span>
            <Icon name="grid" />
            Ancho de esta pieza
          </span>
          <WidthButtons
            ancho={bloque.width}
            onElegir={(ancho: Ancho) => cambiarAncho(bloque.id, ancho)}
          />
        </div>
      )}
      {bloque.type === 'summary' ? (
        <Resumen sistema={sistema} />
      ) : bloque.type === 'note' ? (
        <div className="note-content">{bloque.content}</div>
      ) : bloque.type === 'form' ? (
        <>
          <FormularioEnLinea bloque={bloque} sistema={sistema} />
          <Registros bloque={bloque} sistema={sistema} consulta="" />
        </>
      ) : (
        <>
          <div className="block-bar">
            <label className="searchbox">
              <Icon name="search" />
              <input
                className="input"
                placeholder={`Buscar en ${bloque.name.toLowerCase()}`}
                aria-label={`Buscar en ${bloque.name}`}
                data-filter-block={bloque.id}
                value={consulta}
                onChange={(evento) => setConsulta(evento.target.value)}
              />
            </label>
            <span className="record-count">{bloque.rows.length} registros</span>
            <IconBtn
              name="expand"
              label={`Ver ${bloque.name} a pantalla completa`}
              data-accion="table-fullscreen"
              onClick={() => verTablaCompleta(bloque.id)}
            />
            <Btn
              cls="soft small"
              data-accion="add-record"
              data-tour="openRecord"
              data-tour-id={bloque.id}
              onClick={() => abrirModal({ tipo: 'registro', bloqueId: bloque.id })}
            >
              <Icon name="plus" /> Registrar
            </Btn>
          </div>
          <Registros bloque={bloque} sistema={sistema} consulta={consulta} />
        </>
      )}
      {modo === 'build' && (
        <>
          <button
            type="button"
            className="resize-grip"
            data-resize-block={bloque.id}
            aria-label={`Arrastrar para cambiar el ancho de ${bloque.name}`}
            title="Arrastrá para cambiar el ancho"
            onPointerDown={(evento) => nodo.current && onTirador(evento, bloque, nodo.current)}
          >
            ↔
          </button>
        </>
      )}
    </section>
  );
}
