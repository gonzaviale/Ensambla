import { useRef, useState, type DragEvent } from 'react';
import { useLecturaDeImagen } from '@/hooks/sistemas/useLecturaDeImagen';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { CATALOG } from '@/lib/sistema/catalogo';
import {
  allBlocks,
  clone,
  findBlock,
  importRows,
  makeBlock,
  makeSystem,
  validateRow,
} from '@/lib/sistema/core';
import type { Bloque, Sistema, TipoBloque, Valor } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';
import { Btn } from '../Btn';
import { ControlDeCampo } from '../editor/CampoDeFormulario';
import { Icon } from '../Icon';
import { IconTile } from '../IconTile';
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

type Fila = Record<string, Valor>;

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
// La lectura es simulada (POST /importaciones/lectura); la foto elegida se muestra como referencia.
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
  const [filas, setFilas] = useState<Fila[]>([]);
  const [marcadas, setMarcadas] = useState<boolean[]>([]);
  const [omitirDuplicados, setOmitirDuplicados] = useState(true);
  const [descripcion, setDescripcion] = useState('');
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
      const resultado = await lectura.mutateAsync({
        contexto,
        tipo: destino.type,
        campos: destino.fields,
        vinculos,
      });
      setArchivo(imagen);
      setUrl(dataUrl);
      setDescripcion(resultado.descripcion);
      setFilas(resultado.filas);
      setMarcadas(resultado.filas.map(() => true));
      setPaso('revisar');
    } catch {
      setError('No pudimos leer la imagen. Probá de nuevo.');
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

  const editarCelda = (indice: number, clave: string, valor: Valor) =>
    setFilas((actuales) =>
      actuales.map((fila, i) =>
        i === indice ? { ...fila, [clave]: valor, __example: false } : fila,
      ),
    );

  const incorporar = () => {
    const elegidas = filas.filter((_, indice) => marcadas[indice]);
    if (!elegidas.length) {
      setError('Seleccioná al menos un registro.');
      return;
    }
    const invalida = elegidas.findIndex(
      (fila) => Object.keys(validateRow(destino, fila, sistema ?? makeSystem('_'))).length,
    );
    if (invalida >= 0) {
      const errores = validateRow(destino, elegidas[invalida] ?? {}, sistema ?? makeSystem('_'));
      setError(`Revisá el registro ${invalida + 1}: ${Object.values(errores)[0]}`);
      return;
    }
    if (sistema) {
      // Primero se ensaya sobre una copia: si algo falla no se incorpora nada.
      const ensayo = clone(sistema);
      let bloqueDeEnsayo = findBlock(ensayo, destinoId ?? undefined);
      if (!bloqueDeEnsayo) {
        bloqueDeEnsayo = clone(destino);
        ensayo.sections[0]?.blocks.push(bloqueDeEnsayo);
      }
      const control = importRows(bloqueDeEnsayo, elegidas, ensayo, omitirDuplicados);
      if (control.errors.length) {
        setError(
          `${Object.values(control.errors[0] ?? {})[0] ?? ''} Revisá el lote antes de incorporarlo.`,
        );
        return;
      }
      let resultado: ReturnType<typeof importRows> | undefined;
      useSistemaStore.getState().commit((s) => {
        let objetivo = findBlock(s, destinoId ?? undefined);
        if (!objetivo) {
          objetivo = clone(destino);
          s.sections[0]?.blocks.push(objetivo);
        }
        resultado = importRows(objetivo, elegidas, s, omitirDuplicados);
      });
      cerrarModal();
      avisarResultado(resultado);
      return;
    }
    const nuevo = makeSystem(NOMBRE_DEL_SISTEMA_NUEVO[tipo] ?? 'Mi sistema', [tipo]);
    const bloqueNuevo = nuevo.sections[0]?.blocks[0];
    const resultado = bloqueNuevo
      ? importRows(bloqueNuevo, elegidas, nuevo, omitirDuplicados)
      : undefined;
    cerrarModal();
    void navegacion.crearYAbrir(nuevo);
    avisarResultado(resultado);
  };

  const avisarResultado = (resultado: { added: number; skipped: number } | undefined) => {
    if (!resultado) return;
    useSistemaStore
      .getState()
      .avisar(
        `${resultado.added} registros incorporados${resultado.skipped ? ` · ${resultado.skipped} duplicados omitidos` : ''}.`,
      );
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
      <Modal>
        <div className="icon-tile" style={varsDeColor('blue')}>
          <Icon name="camera" />
        </div>
        <h2 id="modal-title">Traé lo que ya tenés.</h2>
        <p>Probá cómo sería pasar de una foto o captura a datos que podés revisar y organizar.</p>
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
                      {sistema ? 'Crear bloque: ' : 'Crear sistema de '}
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
          <Icon name="upload" />
          <strong>Elegí una foto o arrastrala acá</strong>
          <p>
            Una hoja, un cuaderno o una captura de pantalla.
            <br />
            JPG, PNG o WEBP · hasta 10 MB
          </p>
          <span className="btn secondary small">Elegir imagen</span>
        </button>
        <div className="simulation-label">
          <strong>Lectura de imágenes simulada</strong>
          La imagen que elijas se muestra como referencia. Para probar el recorrido, vamos a
          proponer datos de ejemplo que podés editar antes de confirmar.
        </div>
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
      <Modal ancho>
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
    <Modal ancho>
      <div className="row">
        <IconTile tipo={destino.type} />
        <span className="tag">PASO 2 / REVISAR</span>
      </div>
      <h2 id="modal-title">Vos tenés la última palabra.</h2>
      <p>Corregí lo que haga falta y elegí qué registros sumar a {destino.name.toLowerCase()}.</p>
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
            <strong>{filas.length} registros de ejemplo · lectura simulada</strong>
            Los datos de esta tabla son una propuesta de prueba. Podés cambiarlos antes de
            incorporarlos.
          </div>
          <div className="table-scroll">
            <table className="data-table import-table">
              <thead>
                <tr>
                  <th scope="col">Sumar</th>
                  {destino.fields.map((campo) => (
                    <th scope="col" key={campo.key}>
                      {campo.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filas.map((fila, indice) => (
                  <tr key={indice}>
                    <td>
                      <input
                        type="checkbox"
                        checked={marcadas[indice] ?? false}
                        aria-label={`Incorporar registro ${indice + 1}`}
                        onChange={(evento) =>
                          setMarcadas((actuales) =>
                            actuales.map((marca, i) =>
                              i === indice ? evento.target.checked : marca,
                            ),
                          )
                        }
                      />
                    </td>
                    {destino.fields.map((campo) => (
                      <td key={campo.key}>
                        <ControlDeCampo
                          sistema={sistema}
                          campo={{ ...campo, required: false }}
                          valor={fila[campo.key]}
                          id={`imp-${indice}-${campo.key}`}
                          etiqueta={`${campo.label}, registro ${indice + 1}`}
                          onCambio={(valor) => editarCelda(indice, campo.key, valor)}
                        />
                        <span className="field-error" id={`imp-${indice}-${campo.key}-error`} />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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
