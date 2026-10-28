import { useRef, useState, type DragEvent } from 'react';
import { prepararFoto } from '@/hooks/sistemas/useTaller';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { Icon } from '../Icon';
import { Modal } from '../Modal';
import { Sami } from '../Sami';

const TIPOS_DE_IMAGEN = ['image/png', 'image/jpeg', 'image/webp'];
const MAXIMO_BYTES = 10 * 1024 * 1024;

type Props = {
  sistemaId: string | null;
  bloqueId: string | null;
  contexto: 'registros' | 'negocio';
};

// Traer datos de una foto: se elige la imagen y listo, la charla sigue con Sami. Es él quien la mira, cuenta qué leyó,
// pregunta si algo está mal o es de otro cuaderno y ofrece sumar tablas. Acá no se revisa nada.
export function ModalImportar({ sistemaId }: Props) {
  const navegacion = useNavegacion();
  const archivoInput = useRef<HTMLInputElement>(null);
  const [error, setError] = useState('');
  const [encima, setEncima] = useState(false);
  const [preparando, setPreparando] = useState(false);

  const elegirImagen = async (imagen: File | undefined) => {
    if (!imagen || preparando) return;
    if (!TIPOS_DE_IMAGEN.includes(imagen.type)) {
      setError('Elegí una imagen JPG, PNG o WEBP.');
      return;
    }
    if (imagen.size > MAXIMO_BYTES) {
      setError('La imagen supera los 10 MB. Elegí una más liviana.');
      return;
    }
    setError('');
    setPreparando(true);
    const foto = await prepararFoto(imagen);
    if (typeof foto === 'string') {
      setPreparando(false);
      setError(foto);
      return;
    }
    await navegacion.irATallerConFoto(foto, sistemaId);
  };

  const soltarImagen = (evento: DragEvent<HTMLElement>) => {
    evento.preventDefault();
    setEncima(false);
    void elegirImagen(evento.dataTransfer.files[0]);
  };

  return (
    <Modal clase="modal-importar">
      <div className="sami-intro">
        <Sami pose="think" />
        <div>
          <h2 id="modal-title">Mostrale tu cuaderno a Sami.</h2>
          <p>
            Subí una foto de cómo anotás hoy. Sami la mira, te cuenta qué entendió y te pregunta si
            está bien.
          </p>
        </div>
      </div>
      <input
        ref={archivoInput}
        type="file"
        id="photo-file"
        accept="image/png,image/jpeg,image/webp"
        hidden
        onChange={(evento) => void elegirImagen(evento.target.files?.[0])}
      />
      <button
        type="button"
        className={`upload-zone full ${encima ? 'drop-active' : ''}`.trim()}
        id="photo-drop"
        disabled={preparando}
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
        <strong>{preparando ? 'Preparando la foto…' : 'Elegí una foto o arrastrala acá'}</strong>
        <p>
          Una hoja, un cuaderno o una captura de pantalla.
          <br />
          JPG, PNG o WEBP · hasta 10 MB
        </p>
        <span className="btn secondary small">Elegir imagen</span>
      </button>
      <div className="simulation-label">
        <strong>La foto se la mandamos a Sami</strong>
        Puede equivocarse: vas a poder corregirlo en la charla antes de guardar nada.
      </div>
      <div className="form-error" id="import-error" role="alert">
        {error}
      </div>
    </Modal>
  );
}
