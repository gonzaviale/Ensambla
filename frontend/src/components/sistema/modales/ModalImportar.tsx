import { useRef, useState, type DragEvent } from 'react';
import { leerErrorServidor } from '@/hooks/errores';
import { LECTURA_CON_IA, useLecturaDeImagen } from '@/hooks/sistemas/useLecturaDeImagen';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { useRevisionDeLectura } from '@/hooks/sistemas/useRevisionDeLectura';
import { CATALOG } from '@/lib/sistema/catalogo';
import { allBlocks, clone, findBlock, importRows, makeBlock, makeSystem } from '@/lib/sistema/core';
import {
  aIncorporacion,
  erroresDeLosRegistros,
  motivosPendientes,
  type AccionDeRevision,
} from '@/lib/sistema/revision';
import type { Bloque, Sistema, TipoBloque } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
import { RevisionDeLectura } from '../importar/RevisionDeLectura';
import { Modal } from '../Modal';
import { Sami } from '../Sami';
import { varsDeColor } from '../colores';

const TIPOS_DE_IMAGEN = ['image/png', 'image/jpeg', 'image/webp'];
const MAXIMO_BYTES = 10 * 1024 * 1024;
const TIPOS_SIN_SISTEMA: TipoBloque[] = ['inventory', 'sales', 'clients'];
const NOMBRE_DEL_SISTEMA_NUEVO: Partial<Record<TipoBloque, string>> = {
  inventory: 'Mi inventario',
  sales: 'Mi registro de ventas',
  clients: 'Mi registro de clientes',
};

type Props = {
  sistemaId: string | null;
  bloqueId: string | null;
  contexto: 'registros' | 'negocio';
};

const Cuaderno = () => (
  <div className="fake-notebook" aria-label="Ejemplo de una anotación">
    <strong>Mis anotaciones</strong>
    Yerba · 24 unidades
    <br />
    Fideos · 30 unidades
    <br />
    Gaseosa · 18 unidades
  </div>
);

// Traer datos de una foto: elegir la imagen, revisar lo que "se leyó" y recién ahí incorporarlo.
// Con una foto, la lee la IA del backend (POST /importaciones/lectura) en los registros de un bloque; en el
// modo completo, o sin foto («Probar con un ejemplo»), la lectura es de muestra.
export function ModalImportar({ sistemaId, bloqueId, contexto }: Props) {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const navegacion = useNavegacion();
  const lectura = useLecturaDeImagen();
  const archivoInput = useRef<HTMLInputElement>(null);

  const sistema: Sistema | undefined = sistemas.find((item) => item.id === sistemaId);
  const destinos = sistema ? allBlocks(sistema).filter((bloque) => bloque.fields.length) : [];
  const elegido = findBlock(sistema, bloqueId ?? undefined) ?? destinos[0];

  const [destinoId, setDestinoId] = useState<string | null>(elegido?.id ?? null);
  const [tipo, setTipo] = useState<TipoBloque>(elegido?.type ?? 'inventory');
  const [paso, setPaso] = useState<'elegir' | 'revisar'>('elegir');
  const [archivo, setArchivo] = useState<File | null>(null);
  const [url, setUrl] = useState<string | null>(null);
  const { revision, cargar, actuar } = useRevisionDeLectura();
  const [intentado, setIntentado] = useState(false);
  const [omitirDuplicados, setOmitirDuplicados] = useState(true);
  const [descripcion, setDescripcion] = useState('');
  const [leidaPorIa, setLeidaPorIa] = useState(false);
  const [error, setError] = useState('');
  const [encima, setEncima] = useState(false);

  // El bloque de destino: uno que ya existe en el sistema o uno nuevo del tipo elegido.
  const destino: Bloque = (destinoId && findBlock(sistema, destinoId)) || makeBlock(tipo);

  const leer = async (imagen: File | null, dataUrl: string | null) => {
    setError('');
    const vinculos: Record<string, string[]> = {};
    for (const campo of destino.fields.filter((item) => item.type === 'relation')) {
      vinculos[campo.key] = (findBlock(sistema, campo.target)?.rows ?? []).map((fila) => fila.id);
    }
    try {
      // La IA lee los registros de un bloque; lo que Sami entiende del negocio sigue siendo de muestra.
      const aLeer = contexto === 'registros' ? imagen : null;
      const resultado = await lectura.mutateAsync({
        pedido: {
          nombre: destino.name,
          contexto,
          tipo: destino.type,
          campos: destino.fields,
          vinculos,
          destinoExistente: Boolean(destinoId && findBlock(sistema, destinoId)),
        },
        imagen: aLeer,
      });
      setLeidaPorIa(Boolean(aLeer) && LECTURA_CON_IA);
      setArchivo(imagen);
      setUrl(dataUrl);
      setDescripcion(resultado.descripcion);
      cargar(resultado);
      setIntentado(false);
      setPaso('revisar');
    } catch (falla) {
      setError(leerErrorServidor(falla)?.mensaje ?? 'No pudimos leer la imagen. Probá de nuevo.');
    }
  };

  const elegirImagen = (imagen: File | undefined) => {
    if (!imagen) return;
    if (!TIPOS_DE_IMAGEN.includes(imagen.type)) {
      setError('Elegí una imagen JPG, PNG o WEBP.');
      return;
    }
    if (imagen.size > MAXIMO_BYTES) {
      setError('La imagen supera los 10 MB. Elegí una más liviana.');
      return;
    }
    const lector = new FileReader();
    lector.onload = () => void leer(imagen, String(lector.result));
    lector.onerror = () => setError('No se pudo abrir esta imagen. Probá con otra.');
    lector.readAsDataURL(imagen);
  };

  const soltarImagen = (evento: DragEvent<HTMLElement>) => {
    evento.preventDefault();
    setEncima(false);
    elegirImagen(evento.dataTransfer.files[0]);
  };

  const cambiarDestino = (valor: string) => {
    if (findBlock(sistema, valor)) {
      setDestinoId(valor);
      setTipo(findBlock(sistema, valor)?.type ?? tipo);
    } else {
      setDestinoId(null);
      setTipo(valor as TipoBloque);
    }
  };

  // Cualquier corrección de la revisión borra el aviso anterior: se vuelve a comprobar al incorporar.
  const corregir = (accion: AccionDeRevision) => {
    setError('');
    actuar(accion);
  };

  const incorporar = () => {
    setIntentado(true);
    const motivos = motivosPendientes(revision, destino.fields);
    if (motivos[0]) {
      setError(motivos[0]);
      return;
    }
    const problemas = erroresDeLosRegistros(destino, revision, sistema ?? makeSystem('_'));
    const invalida = revision.filas.findIndex((fila) => problemas[fila.id]);
    if (invalida >= 0) {
      const errores = problemas[revision.filas[invalida]?.id ?? ''] ?? {};
      setError(`Revisá el registro ${invalida + 1}: ${Object.values(errores)[0] ?? ''}`);
      return;
    }
    // Los campos nuevos aceptados y los registros se incorporan juntos; lo descartado no entra.
    const { bloque: conCamposNuevos, filas } = aIncorporacion(destino, revision);
    const camposNuevos = conCamposNuevos.fields.length - destino.fields.length;
    if (sistema) {
      // Primero se ensaya sobre una copia: si algo falla no se incorpora nada (ni campos ni registros).
      const ensayo = clone(sistema);
      let bloqueDeEnsayo = findBlock(ensayo, destinoId ?? undefined);
      if (bloqueDeEnsayo) {
        bloqueDeEnsayo.fields = clone(conCamposNuevos.fields);
      } else {
        bloqueDeEnsayo = clone(conCamposNuevos);
        ensayo.sections[0]?.blocks.push(bloqueDeEnsayo);
      }
      const control = importRows(bloqueDeEnsayo, filas, ensayo, omitirDuplicados);
      if (control.errors.length) {
        setError(
          `${Object.values(control.errors[0] ?? {})[0] ?? ''} Revisá el lote antes de incorporarlo.`,
        );
        return;
      }
      let resultado: ReturnType<typeof importRows> | undefined;
      useSistemaStore.getState().commit((s) => {
        let objetivo = findBlock(s, destinoId ?? undefined);
        if (objetivo) {
          objetivo.fields = clone(conCamposNuevos.fields);
        } else {
          objetivo = clone(conCamposNuevos);
          s.sections[0]?.blocks.push(objetivo);
        }
        resultado = importRows(objetivo, filas, s, omitirDuplicados);
      });
      cerrarModal();
      avisarResultado(resultado, camposNuevos);
      return;
    }
    const nuevo = makeSystem(NOMBRE_DEL_SISTEMA_NUEVO[tipo] ?? 'Mi cuaderno', [tipo]);
    const bloqueNuevo = nuevo.sections[0]?.blocks[0];
    const resultado = bloqueNuevo
      ? importRows(bloqueNuevo, filas, nuevo, omitirDuplicados)
      : undefined;
    cerrarModal();
    void navegacion.crearYAbrir(nuevo);
    avisarResultado(resultado, 0);
  };

  const avisarResultado = (
    resultado: { added: number; skipped: number } | undefined,
    camposNuevos: number,
  ) => {
    if (!resultado) return;
    const omitidos = resultado.skipped ? ` · ${resultado.skipped} duplicados omitidos` : '';
    const campos = camposNuevos
      ? ` · ${camposNuevos === 1 ? '1 campo nuevo' : `${camposNuevos} campos nuevos`}`
      : '';
    useSistemaStore
      .getState()
      .avisar(`${resultado.added} registros incorporados${omitidos}${campos}.`);
  };

  const usarDescripcion = () => {
    const texto = descripcion.trim();
    if (texto.length < 5) return;
    cerrarModal();
    const almacen = useSistemaStore.getState();
    void navegacion.irATaller().then(() => almacen.actualizarTaller({ entrada: texto }));
  };

  const alVolver = () => {
    setPaso('elegir');
    setError('');
  };

  /* Paso 1: elegir la imagen */
  if (paso === 'elegir') {
    return (
      <Modal clase="modal-importar">
        <div className="icon-tile importar-icono" style={varsDeColor('coral')}>
          <Icon name="camera" />
        </div>
        <h2 id="modal-title">Cargar datos desde una foto</h2>
        <p>Elegí dónde van los datos y subí una foto o captura. Antes de guardar, los revisás.</p>
        {contexto === 'negocio' ? (
          <div className="info-box" style={{ marginBottom: 18 }}>
            Podés mostrarle a Sami un ejemplo de cómo anotás hoy para contarle sobre tu negocio.
          </div>
        ) : (
          <div className="field">
            <label htmlFor="import-target">¿Dónde querés llevar los datos?</label>
            <select
              id="import-target"
              value={destinoId ?? tipo}
              onChange={(evento) => cambiarDestino(evento.target.value)}
            >
              {destinos.length
                ? destinos.map((bloque) => (
                    <option key={bloque.id} value={bloque.id}>
                      {bloque.name}
                    </option>
                  ))
                : TIPOS_SIN_SISTEMA.map((opcion) => (
                    <option key={opcion} value={opcion}>
                      {sistema ? 'Crear bloque: ' : 'Crear cuaderno de '}
                      {CATALOG[opcion].name.toLowerCase()}
                    </option>
                  ))}
            </select>
          </div>
        )}
        <input
          ref={archivoInput}
          type="file"
          id="photo-file"
          accept="image/png,image/jpeg,image/webp"
          hidden
          onChange={(evento) => elegirImagen(evento.target.files?.[0])}
        />
        <button
          type="button"
          className={`upload-zone full ${encima ? 'drop-active' : ''}`.trim()}
          id="photo-drop"
          onClick={() => archivoInput.current?.click()}
          onDragOver={(evento) => {
            evento.preventDefault();
            setEncima(true);
          }}
          onDragLeave={() => setEncima(false)}
          onDrop={soltarImagen}
        >
          <span className="upload-icono">
            <Icon name="upload" />
          </span>
          <strong>Elegí una foto o arrastrala acá</strong>
          <p>
            Una hoja, un cuaderno o una captura de pantalla.
            <br />
            JPG, PNG o WEBP · hasta 10 MB
          </p>
          <span className="btn secondary small">Elegir imagen</span>
        </button>
        {LECTURA_CON_IA && contexto === 'registros' ? (
          <div className="simulation-label">
            <strong>Lectura con inteligencia artificial</strong>
            La foto se envía a un servicio de IA para leerla y te proponemos los registros. Puede
            equivocarse: revisalos antes de confirmar. «Probar con un ejemplo» usa datos de muestra.
          </div>
        ) : (
          <div className="simulation-label">
            <strong>Lectura de imágenes simulada</strong>
            La imagen que elijas se muestra como referencia. Para probar el recorrido, vamos a
            proponer datos de ejemplo que podés editar antes de confirmar.
          </div>
        )}
        <div className="form-error" id="import-error" role="alert">
          {error}
        </div>
        <div className="modal-footer spread">
          <span className="small-text muted" style={{ fontSize: 10 }}>
            La foto es opcional.
          </span>
          <Btn cls="primary" disabled={lectura.isPending} onClick={() => void leer(null, null)}>
            Probar con un ejemplo <Icon name="arrow" />
          </Btn>
        </div>
      </Modal>
    );
  }

  /* Paso 2 (negocio): lo que Sami entendió, para llevarlo al taller */
  if (contexto === 'negocio') {
    return (
      <Modal ancho clase="modal-importar">
        <div className="sami-intro">
          <Sami pose="think" />
          <div>
            <h2 id="modal-title">Esto me da un punto de partida.</h2>
            <p>Revisemos juntos lo que podrías contarme.</p>
          </div>
        </div>
        <div className="import-layout">
          <div className="import-visual">
            {url ? (
              <img className="upload-preview" src={url} alt="La imagen que elegiste" />
            ) : (
              <Cuaderno />
            )}
          </div>
          <div>
            <div className="simulation-label">
              <strong>Interpretación de ejemplo</strong>
              Este texto es simulado. Editalo para que describa lo que hacés.
            </div>
            <label className="field-label" htmlFor="photo-description">
              Sobre tu negocio
            </label>
            <textarea
              id="photo-description"
              style={{ marginTop: 9 }}
              value={descripcion}
              onChange={(evento) => setDescripcion(evento.target.value)}
            />
          </div>
        </div>
        <div className="modal-footer">
          <Btn cls="secondary" onClick={alVolver}>
            Elegir otra imagen
          </Btn>
          <Btn cls="primary" onClick={usarDescripcion}>
            Usar esta descripción <Icon name="arrow" />
          </Btn>
        </div>
      </Modal>
    );
  }

  /* Paso 2: revisar los registros leídos */
  return (
    <Modal ancho clase="modal-importar modal-revisar">
      <div className="row">
        <IconTile tipo={destino.type} color="coral" className="importar-icono" />
        <span className="tag paso-importar">Paso 2 · Revisar</span>
      </div>
      <h2 id="modal-title">Confirmar registros</h2>
      <p>
        Corregí lo que haga falta, quitá lo que no quieras y sumá el resto a{' '}
        {destino.name.toLowerCase()}.
      </p>
      <div className="import-layout">
        <div className="import-visual">
          {url ? (
            <img className="upload-preview" src={url} alt="La imagen que elegiste" />
          ) : (
            <Cuaderno />
          )}
          <p className="file-label">{archivo ? archivo.name : 'Anotación de ejemplo'}</p>
        </div>
        <div>
          <div className="simulation-label" style={{ marginTop: 0 }}>
            {leidaPorIa ? (
              <>
                <strong>{revision.filas.length} registros leídos de tu foto</strong>
                La lectura puede equivocarse: revisá lo marcado y corregí lo que haga falta antes de
                incorporarlos.
              </>
            ) : (
              <>
                <strong>{revision.filas.length} registros de ejemplo · lectura simulada</strong>
                Estos datos son una propuesta de prueba. Podés cambiarlos antes de incorporarlos.
              </>
            )}
          </div>
          <RevisionDeLectura
            revision={revision}
            destino={destino}
            sistema={sistema}
            errores={
              intentado ? erroresDeLosRegistros(destino, revision, sistema ?? makeSystem('_')) : {}
            }
            onAccion={corregir}
          />
          <label className="check-row" style={{ marginTop: 17 }}>
            <input
              type="checkbox"
              id="skip-duplicates"
              checked={omitirDuplicados}
              onChange={(evento) => setOmitirDuplicados(evento.target.checked)}
            />
            Evitar duplicados por {destino.fields[0]?.label.toLowerCase() ?? 'nombre'}
          </label>
          <div className="form-error" id="import-error" role="alert">
            {error}
          </div>
        </div>
      </div>
      <div className="modal-footer spread">
        <Btn cls="secondary" onClick={alVolver}>
          Volver a la imagen
        </Btn>
        <Btn cls="primary" onClick={incorporar}>
          Incorporar registros <Icon name="check" />
        </Btn>
      </div>
    </Modal>
  );
}
