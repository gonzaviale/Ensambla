import type { RefObject } from 'react';
import type { CamaraDelLector } from '@/types/escaneo';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';

type Props = {
  codigo: string;
  buscando: boolean;
  abierta: boolean;
  iniciando: boolean;
  sinLectura: boolean;
  sesion: number;
  camaras: CamaraDelLector[];
  camaraId: string;
  camaraActiva: string;
  camaraActualId: string;
  errorCamara: string;
  video: RefObject<HTMLVideoElement | null>;
  entradaCodigo: RefObject<HTMLInputElement | null>;
  onCodigo: (codigo: string) => void;
  onBuscar: () => void;
  onAbrir: () => void;
  onCerrar: () => void;
  onReiniciar: () => void;
  onCamara: (id: string) => void;
};
export function LectorDeCodigos({
  codigo,
  buscando,
  abierta,
  iniciando,
  sinLectura,
  sesion,
  camaras,
  camaraId,
  camaraActiva,
  camaraActualId,
  errorCamara,
  video,
  entradaCodigo,
  onCodigo,
  onBuscar,
  onAbrir,
  onCerrar,
  onReiniciar,
  onCamara,
}: Props) {
  return (
    <section className="escaneo-panel escaneo-lector" aria-label="Lector de códigos">
      <div className="escaneo-panel-titulo">
        <span className="escaneo-numero-paso">1</span>
        <div>
          <h2>Leé el código del producto</h2>
          <p>Con la cámara o con tu lector de códigos.</p>
        </div>
      </div>
      {errorCamara && (
        <div className="escaneo-aviso escaneo-aviso-error" role="alert">
          <Icon name="camera" />
          <div>
            <strong>La cámara necesita atención</strong>
            <p>{errorCamara}</p>
          </div>
        </div>
      )}
      {abierta ? (
        <div className="escaneo-camara">
          <div className="escaneo-estado-camara" role="status">
            <Icon name={iniciando ? 'clock' : 'camera'} />
            <div>
              <strong>{iniciando ? 'Iniciando cámara…' : 'Cámara lista para leer'}</strong>
              <p>
                {iniciando
                  ? 'Preparando la imagen del lector.'
                  : 'Apuntá al código y mantené visibles todas las barras.'}
              </p>
            </div>
          </div>
          <video
            key={sesion}
            ref={video}
            muted
            playsInline
            autoPlay
            aria-label="Vista previa del lector"
          />
          {sinLectura && (
            <div className="escaneo-aviso escaneo-aviso-existente" role="status">
              <Icon name="help" />
              <div>
                <strong>Todavía no detectamos el código</strong>
                <p>
                  Alejá un poco el teléfono para que enfoque, evitá reflejos y probá otra cámara si
                  la imagen se ve borrosa.
                </p>
              </div>
            </div>
          )}
          {camaraActiva && (
            <p className="escaneo-ayuda">
              {camaraId ? 'Cámara elegida' : 'Cámara automática'}: {camaraActiva}
            </p>
          )}
          <div className="row escaneo-controles-lector">
            <Btn cls="secondary" disabled={iniciando} onClick={onReiniciar}>
              Reiniciar lector
            </Btn>
            <Btn cls="ghost" onClick={onCerrar}>
              Cerrar cámara
            </Btn>
          </div>
        </div>
      ) : (
        <Btn cls="primary escaneo-boton-camara" disabled={buscando} onClick={onAbrir}>
          <Icon name="camera" /> {errorCamara ? 'Reintentar cámara' : 'Escanear producto'}
        </Btn>
      )}
      {(abierta || errorCamara) && camaras.length > 0 && (
        <label>
          Cámara del lector
          <span className="escaneo-control escaneo-control-elegir">
            <select
              value={camaraId || camaraActualId}
              disabled={iniciando}
              onChange={(e) => onCamara(e.target.value)}
            >
              <option value="">Elegir automáticamente</option>
              {camaras.map((camara) => (
                <option key={camara.id} value={camara.id}>
                  {camara.nombre}
                </option>
              ))}
            </select>
            <Icon name="down" />
          </span>
        </label>
      )}
      <form
        className="escaneo-codigo"
        onSubmit={(e) => {
          e.preventDefault();
          onBuscar();
        }}
      >
        <label htmlFor="escaneo-codigo">Código de barras</label>
        <div className="escaneo-entrada-codigo">
          <input
            ref={entradaCodigo}
            id="escaneo-codigo"
            inputMode="numeric"
            autoComplete="off"
            placeholder="Escaneá o escribí el EAN / UPC"
            value={codigo}
            onChange={(e) => onCodigo(e.target.value)}
            disabled={buscando || abierta}
          />
          <button
            type="submit"
            className="btn secondary"
            disabled={buscando || abierta || !codigo.trim()}
          >
            <Icon name="search" /> {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>
      <details className="escaneo-ayuda-lector">
        <summary>¿Usás un lector USB o Bluetooth?</summary>
        <p className="escaneo-ayuda">
          Funciona como un teclado: lee el código y envía Enter. Después de cada lectura, podés
          escanear o ingresar el siguiente código.
        </p>
      </details>
    </section>
  );
}
