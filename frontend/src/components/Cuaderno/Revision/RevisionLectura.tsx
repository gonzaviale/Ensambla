import { useMemo, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import {
  ArrowBendDownRightIcon,
  FloppyDiskIcon,
  ImageIcon,
  PencilSimpleIcon,
  PlusIcon,
  WarningIcon,
} from '@phosphor-icons/react';
import {
  Button,
  ConfirmDialog,
  PageHeading,
  Panel,
  Sheet,
  Spinner,
  Stepper,
  StickyActionBar,
  Toast,
} from '@/components/ui';
import { leerErrorServidor } from '@/hooks/errores';
import { urlFotoLote } from '@/hooks/lotes/fotos';
import { useConfirmarLectura } from '@/hooks/lotes/mutations';
import { useLote } from '@/hooks/lotes/queries';
import { useConexiones } from '@/hooks/lotes/useConexiones';
import { useRevisionLectura } from '@/hooks/lotes/useRevisionLectura';
import { useEsEscritorio } from '@/hooks/useMediaQuery';
import {
  aConfirmacion,
  cantidadInvalidas,
  cantidadPorRevisar,
  claveErrorServidor,
  erroresDeEstructura,
  estaBloqueada,
  idCelda,
  siguienteDuda,
  type AccionRevision,
  type BorradorRevision,
  type Destino,
  type FilaRevision,
  type PosicionCelda,
} from '@/lib/revisionLectura';
import type { PropuestaLectura } from '@/types/lectura';
import { DestinoGuardado } from './DestinoGuardado';
import { EditorColumna } from './EditorColumna';
import { FilasTarjetas } from './FilasTarjetas';
import { TablaRevision } from './TablaRevision';
import { VisorFoto } from './VisorFoto';

type Props = { loteId: string; propuesta: PropuestaLectura };

const FILA_DEL_SERVIDOR = /^filas\[(\d+)\]\.(.+)$/;
const PASOS = ['Revisá', '¿Dónde guardamos?', 'Guardar'];

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

function motivosDeDatos(borrador: BorradorRevision) {
  const motivos = erroresDeEstructura(borrador);
  const invalidas = cantidadInvalidas(borrador);
  if (invalidas > 0) {
    motivos.push(
      invalidas === 1 ? 'Hay 1 dato para corregir.' : `Hay ${invalidas} datos para corregir.`,
    );
  }
  return motivos;
}

function motivosDeDestino(destino: Destino, borrador: BorradorRevision) {
  const motivos: string[] = [];
  if (
    destino.modo !== 'bloque' &&
    borrador.columnas.some((columna) => columna.tipo === 'RELACION')
  ) {
    motivos.push(
      'Una columna conectada con otra tabla sólo se puede guardar en tu tabla existente.',
    );
  }
  if (destino.modo === 'nuevo' && !destino.nombreProyecto.trim()) {
    motivos.push('Escribí un nombre para el sistema.');
  }
  if (destino.modo === 'proyecto' && !destino.proyectoId) motivos.push('Elegí un sistema.');
  return motivos;
}

const textoPorRevisar = (n: number) =>
  n === 0
    ? 'No quedan datos por revisar.'
    : n === 1
      ? 'Queda 1 dato por revisar.'
      : `Quedan ${n} datos por revisar.`;

type FilaQuitada = { fila: FilaRevision; indice: number };

// H-30: la persona revisa lo que leyó la IA con la foto al lado, lo corrige y recién entonces lo guarda.
// Tres pasos en una sola pantalla: revisar los datos, elegir dónde guardarlos y guardar.
export function RevisionLectura({ loteId, propuesta }: Props) {
  const navigate = useNavigate();
  const esEscritorio = useEsEscritorio();
  const lote = useLote(loteId);
  const { borrador, destino, dispatch, cambiarDestino, descartar } = useRevisionLectura(
    loteId,
    propuesta,
  );
  const confirmar = useConfirmarLectura(loteId);
  const conexiones = useConexiones(borrador.columnas, destino);
  const [columnaEditando, setColumnaEditando] = useState<string>();
  const [pagina, setPagina] = useState(1);
  const [duda, setDuda] = useState<PosicionCelda>();
  const [verFoto, setVerFoto] = useState(false);
  const [descartando, setDescartando] = useState(false);
  const [quitada, setQuitada] = useState<FilaQuitada & { numero: number }>();

  const error = leerErrorServidor(confirmar.error);
  const erroresServidor = useMemo(
    () => erroresPorCelda(error?.campos ?? {}, borrador),
    [error, borrador],
  );
  const porRevisar = cantidadPorRevisar(borrador);
  const motivosDatos = motivosDeDatos(borrador);
  const motivosDestino = motivosDeDestino(destino, borrador);
  const motivos = [...motivosDatos, ...motivosDestino];
  const paso = porRevisar > 0 || motivosDatos.length > 0 ? 0 : motivosDestino.length > 0 ? 1 : 2;
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

  // Lleva el foco al próximo dato dudoso o inválido. No lo da por mirado: eso lo hace la persona
  // con «Está bien» o corrigiéndolo.
  const irAlProximoDato = () => {
    const proxima = siguienteDuda(borrador, duda);
    setDuda(proxima);
    const celda = proxima && document.getElementById(idCelda(proxima));
    celda?.focus();
    celda?.scrollIntoView?.({ block: 'center' });
  };

  // Para un motivo que no es un dato (nombre, destino) se lleva a la sección que lo resuelve.
  const irAlMotivo = () => {
    if (porRevisar > 0) return irAlProximoDato();
    const destinoEl = document.getElementById('destino');
    destinoEl?.scrollIntoView?.({ block: 'start' });
    destinoEl?.querySelector<HTMLElement>('input')?.focus();
  };

  const quitarFila = (filaId: string) => {
    const indice = borrador.filas.findIndex((fila) => fila.id === filaId);
    const fila = borrador.filas[indice];
    if (!fila) return;
    actuar({ tipo: 'quitarFila', filaId });
    setQuitada({ fila, indice, numero: indice + 1 });
  };

  const deshacerQuitar = () => {
    if (quitada) actuar({ tipo: 'restaurarFila', fila: quitada.fila, indice: quitada.indice });
    setQuitada(undefined);
  };

  const enfocarCelda = (posicion: PosicionCelda, paginaDeLaFila: number | null) => {
    setDuda(posicion);
    if (paginaDeLaFila) setPagina(paginaDeLaFila);
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

  const visor = (
    <VisorFoto
      paginas={paginas}
      actual={pagina}
      onElegir={setPagina}
      altoClassName={esEscritorio ? 'max-h-[calc(100dvh-14rem)]' : 'max-h-[60dvh]'}
    />
  );

  return (
    <div className="flex flex-col gap-6">
      <Stepper pasos={PASOS} actual={paso} label="Pasos para guardar tu cuaderno" />
      <div className="rounded-2xl border border-blue/20 bg-blue-100 p-5 sm:p-6">
        <PageHeading
          kicker="Del papel a tu sistema"
          title="Esto es lo que leímos de tu cuaderno"
          lead="Revisá los datos con la foto al lado. Los marcados son los que no pudimos leer con seguridad. Nada se guarda hasta que toques «Guardar mi cuaderno»."
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(16rem,1fr)_minmax(0,2fr)]">
        {esEscritorio && (
          <Panel className="bg-cream lg:sticky lg:top-24 lg:self-start">
            {paginas.length > 0 ? (
              visor
            ) : lote.isError ? (
              <p role="alert" className="m-0 font-semibold text-bad">
                No pudimos cargar las fotos. Igual podés revisar los datos.
              </p>
            ) : (
              <Spinner label="Cargando la foto…" />
            )}
          </Panel>
        )}

        <section
          aria-labelledby="revisar-titulo"
          className="flex min-w-0 flex-col gap-4 rounded-2xl border border-line/70 bg-surface p-4 sm:p-5"
        >
          <h2 id="revisar-titulo" className="m-0 text-h3 text-petrol">
            1. Revisá los datos
          </h2>

          {!esEscritorio && paginas.length > 0 && (
            <Button className="self-start" onClick={() => setVerFoto(true)}>
              <ImageIcon aria-hidden size={22} />
              Ver mi cuaderno
            </Button>
          )}

          {propuesta.advertencias?.map((aviso) => (
            <p key={aviso} className="m-0 flex items-start gap-2 text-hint font-semibold text-warn">
              <WarningIcon aria-hidden size={20} weight="fill" className="mt-0.5 shrink-0" />
              {aviso}
            </p>
          ))}

          <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
            <Button disabled={porRevisar === 0} onClick={irAlProximoDato}>
              <ArrowBendDownRightIcon aria-hidden size={20} />
              Ir al próximo dato para revisar
            </Button>
            <p role="status" className="m-0 text-base font-semibold text-soft">
              {textoPorRevisar(porRevisar)}
            </p>
          </div>

          {!esEscritorio && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-hint text-soft">Columnas:</span>
              {borrador.columnas.map((item) => (
                <Button
                  key={item.clave}
                  size="compact"
                  aria-label={`Editar la columna «${item.nombre}»`}
                  onClick={() => setColumnaEditando(item.clave)}
                >
                  <PencilSimpleIcon aria-hidden size={18} />
                  {item.nombre || 'Sin nombre'}
                </Button>
              ))}
            </div>
          )}

          {esEscritorio ? (
            <TablaRevision
              borrador={borrador}
              conexiones={conexiones}
              columnaEditando={columnaEditando}
              erroresServidor={erroresServidor}
              onAccion={actuar}
              onEditarColumna={setColumnaEditando}
              onEnfocarCelda={enfocarCelda}
              onQuitarFila={quitarFila}
            />
          ) : (
            <FilasTarjetas
              borrador={borrador}
              conexiones={conexiones}
              erroresServidor={erroresServidor}
              onAccion={actuar}
              onEnfocarCelda={enfocarCelda}
              onQuitarFila={quitarFila}
              onVerFoto={(hoja) => {
                if (hoja) setPagina(hoja);
                setVerFoto(true);
              }}
            />
          )}

          {borrador.filas.length === 0 && (
            <p className="m-0 text-body text-soft">
              No quedan filas. Agregá una a mano o volvé a subir las fotos.
            </p>
          )}

          <div className="flex flex-wrap gap-3">
            <Button
              disabled={borrador.columnas.length === 0}
              onClick={() => actuar({ tipo: 'agregarFila', id: crypto.randomUUID() })}
            >
              <PlusIcon aria-hidden size={20} />
              Agregar fila
            </Button>
            <Button onClick={() => actuar({ tipo: 'agregarColumna' })}>
              <PlusIcon aria-hidden size={20} />
              Agregar columna
            </Button>
          </div>
        </section>
      </div>

      <Panel className="flex flex-col gap-5">
        <DestinoGuardado
          destino={destino}
          tablaExistente={tablaExistente}
          nombreTabla={borrador.nombreBloque}
          onCambiarNombreTabla={(nombre) => actuar({ tipo: 'renombrarBloque', nombre })}
          onCambiar={(siguiente) => {
            confirmar.reset();
            cambiarDestino(siguiente);
          }}
        />
      </Panel>

      {confirmar.isError && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {error?.mensaje}
        </p>
      )}
      {motivos.length > 0 && (
        <ul className="m-0 flex list-none flex-col gap-1 p-0 text-base text-soft">
          {motivos.map((motivo) => (
            <li key={motivo}>{motivo}</li>
          ))}
        </ul>
      )}

      <StickyActionBar label="Guardar el cuaderno">
        <div className="min-w-0 sm:flex-1">
          {motivos.length > 0 ? (
            <p className="m-0 flex flex-wrap items-center gap-x-2 text-base font-semibold">
              Faltan {motivos.length === 1 ? '1 cosa' : `${motivos.length} cosas`} para guardar.
              <Button variant="ghost" size="compact" onClick={irAlMotivo}>
                Ir
                <span className="sr-only"> al primer pendiente</span>
              </Button>
            </p>
          ) : (
            <p className="m-0 text-base font-semibold text-ok">
              {porRevisar === 0
                ? 'Todo listo para guardar.'
                : `Podés guardar. ${textoPorRevisar(porRevisar)}`}
            </p>
          )}
          <p className="m-0 text-caption text-muted max-sm:hidden">
            Tus cambios se guardan en este dispositivo.
          </p>
        </div>
        <div className="flex w-full items-center gap-2 sm:w-auto sm:gap-3">
          <Button
            variant="ghost"
            disabled={confirmar.isPending}
            onClick={() => setDescartando(true)}
          >
            Descartar
          </Button>
          <Button
            variant="solid"
            size="lg"
            className="flex-1 px-3 text-base sm:flex-none sm:px-6 sm:text-body"
            loading={confirmar.isPending}
            disabled={motivos.length > 0}
            onClick={guardar}
          >
            {!confirmar.isPending && (
              <FloppyDiskIcon aria-hidden size={24} className="max-sm:hidden" />
            )}
            {confirmar.isPending ? 'Guardando…' : 'Guardar mi cuaderno'}
          </Button>
        </div>
      </StickyActionBar>

      {columna && (
        <EditorColumna
          key={columna.clave}
          columna={columna}
          columnas={borrador.columnas}
          bloqueada={estaBloqueada(columna, destino)}
          nombreTabla={tablaExistente?.nombre ?? borrador.nombreBloque}
          onCambiar={(cambios) => actuar({ tipo: 'editarColumna', clave: columna.clave, cambios })}
          onQuitar={() => {
            actuar({ tipo: 'quitarColumna', clave: columna.clave });
            setColumnaEditando(undefined);
          }}
          onCerrar={() => setColumnaEditando(undefined)}
        />
      )}

      <Sheet
        open={verFoto && !esEscritorio}
        onClose={() => setVerFoto(false)}
        title="Mi cuaderno"
        description="Tocá «Acercar» para ver la letra más grande."
        tall
      >
        {visor}
      </Sheet>

      <ConfirmDialog
        open={descartando}
        titulo="¿Descartar lo que corregiste?"
        confirmar="Sí, descartar"
        conservar="Seguir revisando"
        onConfirmar={empezarDeNuevo}
        onConservar={() => setDescartando(false)}
      >
        <p className="m-0">
          Se pierde lo que corregiste en este cuaderno y volvés a subir las fotos.
        </p>
      </ConfirmDialog>

      {quitada && (
        <Toast
          tipo="exito"
          mensaje={`Quitaste la fila ${quitada.numero}.`}
          accion={{ etiqueta: 'Deshacer', onClick: deshacerQuitar }}
          duracion={8000}
          onClose={() => setQuitada(undefined)}
        />
      )}
    </div>
  );
}
