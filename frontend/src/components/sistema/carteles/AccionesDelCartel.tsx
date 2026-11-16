import { useState } from 'react';
import { enlaceDeWhatsapp } from '@/lib/carteles';

type Props = {
  textoPosteo: string;
  descargando: boolean;
  onDescargar: () => void;
};

export function AccionesDelCartel({ textoPosteo, descargando, onDescargar }: Props) {
  const [copiado, setCopiado] = useState(false);
  const copiar = async () => {
    await navigator.clipboard.writeText(textoPosteo);
    setCopiado(true);
  };
  return (
    <div className="carteles-acciones">
      <button type="button" className="btn primary" disabled={descargando} onClick={onDescargar}>
        Descargar cartel
      </button>
      <a className="btn secondary" href={enlaceDeWhatsapp(textoPosteo)} target="_blank" rel="noreferrer">
        Compartir por WhatsApp
      </a>
      <button type="button" className="btn secondary" onClick={() => void copiar()}>
        Copiar el texto del posteo
      </button>
      {copiado ? <p role="status">Copiamos el texto del posteo.</p> : null}
    </div>
  );
}
