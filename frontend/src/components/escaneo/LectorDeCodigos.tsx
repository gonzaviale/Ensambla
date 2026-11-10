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
      {abierta ? (
        <div className="escaneo-camara">
          <video
            key={sesion}
            ref={video}
            muted
            playsInline
            autoPlay
            aria-label="Vista previa del lector"
          />
          <p role="status">
            {iniciando
              ? 'Iniciando cámara…'
              : 'Buscando código… Mantené visibles todas las barras y los números.'}
          </p>
          {sinLectura && (
            <p className="escaneo-consejo-lector" role="status">
              Todavía no detectamos el código. Alejá un poco el teléfono para que enfoque, evitá
              reflejos y probá otra cámara si la imagen se ve borrosa.
            </p>
          )}
          {camaras.length > 1 && (
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
          <Icon name="camera" /> Escanear producto
        </Btn>
      )}
      {errorCamara && (
        <p className="escaneo-error" role="alert">
          {errorCamara}
        </p>
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
      <p className="escaneo-ayuda">
        Un lector USB o Bluetooth funciona como un teclado: lee el código y envía Enter. Después de
        cada lectura, tocá «Escanear producto» para leer el siguiente.
      </p>
    </section>
  );
}
