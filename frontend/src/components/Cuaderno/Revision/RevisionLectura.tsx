import { useMemo, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { ArrowBendDownRightIcon, CheckCircleIcon, WarningIcon } from '@phosphor-icons/react';
import { Button, InputField, Panel, Spinner } from '@/components/ui';
import { leerErrorServidor } from '@/hooks/errores';
import { urlFotoLote } from '@/hooks/lotes/fotos';
import { useConfirmarLectura } from '@/hooks/lotes/mutations';
import { useLote } from '@/hooks/lotes/queries';
import { useRevisionLectura } from '@/hooks/lotes/useRevisionLectura';
import {
  aConfirmacion,
  cantidadInvalidas,
  cantidadPorRevisar,
  claveErrorServidor,
  erroresDeEstructura,
  estaBloqueada,
  idCelda,
  reducirBorrador,
  siguienteDuda,
  type AccionRevision,
  type BorradorRevision,
  type Destino,
  type PosicionCelda,
} from '@/lib/revisionLectura';
import type { PropuestaLectura } from '@/types/lectura';
import { DestinoGuardado } from './DestinoGuardado';
import { EditorColumna } from './EditorColumna';
import { TablaRevision } from './TablaRevision';
import { VisorFoto } from './VisorFoto';

type Props = { loteId: string; propuesta: PropuestaLectura };

const FILA_DEL_SERVIDOR = /^filas\[(\d+)\]\.(.+)$/;

// El servidor ubica los errores como `filas[2].monto`: se pasan a la celda de esa fila y columna.
function erroresPorCelda(campos: Record<string, string>, borrador: BorradorRevision) {
  const porCelda: Record<string, string> = {};
  for (const [clave, mensaje] of Object.entries(campos)) {
    const partes = FILA_DEL_SERVIDOR.exec(clave);
    const fila = partes && borrador.filas[Number(partes[1])];
    if (fila && partes[2]) porCelda[claveErrorServidor(fila.id, partes[2])] = mensaje;
  }
  return porCelda;
}

function motivosParaNoGuardar(borrador: BorradorRevision, destino: Destino) {
  const motivos = erroresDeEstructura(borrador);
  const invalidas = cantidadInvalidas(borrador);
  if (invalidas > 0) {
    motivos.push(
      invalidas === 1 ? 'Hay 1 dato para corregir.' : `Hay ${invalidas} datos para corregir.`,
    );
  }
  if (destino.modo === 'nuevo' && !destino.nombreProyecto.trim()) {
    motivos.push('Escribí un nombre para el proyecto.');
  }
  if (destino.modo === 'proyecto' && !destino.proyectoId) motivos.push('Elegí un proyecto.');
  return motivos;
}

// H-30: la persona revisa lo que leyó la IA con la foto al lado, lo corrige y recién entonces lo guarda.
export function RevisionLectura({ loteId, propuesta }: Props) {
  const navigate = useNavigate();
  const lote = useLote(loteId);
  const { borrador, destino, dispatch, cambiarDestino, descartar } = useRevisionLectura(
    loteId,
    propuesta,
  );
  const confirmar = useConfirmarLectura(loteId);
  const [columnaEditando, setColumnaEditando] = useState<string>();
  const [pagina, setPagina] = useState(1);
  const [duda, setDuda] = useState<PosicionCelda>();

  const error = leerErrorServidor(confirmar.error);
  const erroresServidor = useMemo(
    () => erroresPorCelda(error?.campos ?? {}, borrador),
    [error, borrador],
  );
  const porRevisar = cantidadPorRevisar(borrador);
  const motivos = motivosParaNoGuardar(borrador, destino);
  const columna = borrador.columnas.find((item) => item.clave === columnaEditando);
  const tablaExistente = propuesta.bloqueExistenteId
    ? { id: propuesta.bloqueExistenteId, nombre: propuesta.nombre?.trim() || 'tu tabla' }
    : null;
  const paginas = [...(lote.data?.fotos ?? [])]
    .sort((a, b) => a.posicion - b.posicion)
    .map((foto) => ({ numero: foto.posicion + 1, url: urlFotoLote(loteId, foto.id) }));

  const actuar = (accion: AccionRevision) => {
    confirmar.reset();
    dispatch(accion);
  };

  // Al pasar a la siguiente duda, la anterior se da por mirada; si sigue inválida vuelve a aparecer.
  const irASiguienteDuda = () => {
    const base = duda ? reducirBorrador(borrador, { tipo: 'marcarRevisada', ...duda }) : borrador;
    if (duda) dispatch({ tipo: 'marcarRevisada', ...duda });
    const proxima = siguienteDuda(base, duda);
    setDuda(proxima);
    const celda = proxima && document.getElementById(idCelda(proxima));
    celda?.focus();
    celda?.scrollIntoView?.({ block: 'center' });
  };

  const guardar = () =>
    confirmar.mutate(aConfirmacion(borrador, destino), {
      onSuccess: (guardado) => {
        descartar();
        void navigate({
          to: '/proyectos/$proyectoId/usar',
          params: { proyectoId: guardado.proyectoId },
          search: { bloque: guardado.bloqueId },
        });
      },
    });

  const empezarDeNuevo = () => {
    descartar();
    void navigate({ to: '/cuaderno' });
  };

  return (
    <div className="flex flex-col gap-4">
      <Panel className="flex flex-col items-start gap-3">
        <CheckCircleIcon aria-hidden size={32} className="text-ok" />
        <h1 className="m-0 text-[24px]">Esto es lo que leímos de tu cuaderno</h1>
        <p className="text-soft">
          Revisá los datos con la foto al lado. Los marcados son los que no pudimos leer con
          seguridad. Podés corregir cualquier dato, quitar filas y cambiar las columnas. Nada se
          guarda hasta que toques «Guardar».
        </p>
      </Panel>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <Panel className="lg:sticky lg:top-4 lg:self-start">
          {paginas.length > 0 ? (
            <VisorFoto paginas={paginas} actual={pagina} onElegir={setPagina} />
          ) : lote.isError ? (
            <p role="alert" className="m-0 text-bad">
              No pudimos cargar las fotos. Igual podés revisar los datos.
            </p>
          ) : (
            <Spinner label="Cargando la foto…" />
          )}
        </Panel>

        <Panel className="flex flex-col gap-3">
          {destino.modo !== 'bloque' && (
            <InputField
              label="Nombre de la tabla"
              value={borrador.nombreBloque}
              onChange={(e) => actuar({ tipo: 'renombrarBloque', nombre: e.target.value })}
            />
          )}
          {propuesta.advertencias?.map((aviso) => (
            <p key={aviso} className="m-0 flex items-start gap-2 text-[13px] text-warn">
              <WarningIcon aria-hidden size={18} className="mt-0.5 shrink-0" />
              {aviso}
            </p>
          ))}

          <div className="flex flex-wrap items-center gap-3">
            <Button size="sm" disabled={porRevisar === 0} onClick={irASiguienteDuda}>
              <ArrowBendDownRightIcon aria-hidden size={16} />
              Siguiente duda
            </Button>
            <p role="status" className="m-0 text-[13px] text-soft">
              {porRevisar === 0
                ? 'No quedan datos por revisar.'
                : porRevisar === 1
                  ? 'Queda 1 dato por revisar.'
                  : `Quedan ${porRevisar} datos por revisar.`}
            </p>
          </div>

          <TablaRevision
            borrador={borrador}
            columnaEditando={columnaEditando}
            erroresServidor={erroresServidor}
            onAccion={actuar}
            onEditarColumna={setColumnaEditando}
            onEnfocarCelda={(posicion, paginaDeLaFila) => {
              setDuda(posicion);
              if (paginaDeLaFila) setPagina(paginaDeLaFila);
            }}
          />

          {columna && (
            <EditorColumna
              key={columna.clave}
              columna={columna}
              columnas={borrador.columnas}
              bloqueada={estaBloqueada(columna, destino)}
              nombreTabla={tablaExistente?.nombre ?? borrador.nombreBloque}
              onCambiar={(cambios) =>
                actuar({ tipo: 'editarColumna', clave: columna.clave, cambios })
              }
              onQuitar={() => {
                actuar({ tipo: 'quitarColumna', clave: columna.clave });
                setColumnaEditando(undefined);
              }}
              onCerrar={() => setColumnaEditando(undefined)}
            />
          )}
        </Panel>
      </div>

      <DestinoGuardado
        destino={destino}
        tablaExistente={tablaExistente}
        onCambiar={(siguiente) => {
          confirmar.reset();
          cambiarDestino(siguiente);
        }}
      />

      {confirmar.isError && (
        <p role="alert" className="m-0 text-bad">
          {error?.mensaje}
        </p>
      )}
      {motivos.length > 0 && (
        <ul className="m-0 flex list-none flex-col gap-1 p-0 text-[14px] text-soft">
          {motivos.map((motivo) => (
            <li key={motivo}>{motivo}</li>
          ))}
        </ul>
      )}

      <div className="flex flex-wrap gap-2">
        <Button
          variant="primary"
          disabled={motivos.length > 0 || confirmar.isPending}
          onClick={guardar}
        >
          {confirmar.isPending ? 'Guardando…' : 'Guardar'}
        </Button>
        <Button disabled={confirmar.isPending} onClick={empezarDeNuevo}>
          Descartar y subir otras fotos
        </Button>
      </div>
    </div>
  );
}
