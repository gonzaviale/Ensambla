import { useEffect, useState } from 'react';
import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { guardarArchivo } from '@/lib/descargas';
import {
  FORMATOS_DE_IMAGEN,
  formatoPara,
  type ClaveDeFormato,
} from '@/lib/promociones/formatosDeImagen';
import { imagenDePromocion } from '@/lib/promociones/imagenDePromocion';
import { compartir } from '@/lib/sistema/compartir';
import type { Promocion } from '@/types/promociones';

type Props = {
  negocio: string;
  canal: string;
  promocion: Promocion;
  texto: string;
  onTexto: (texto: string) => void;
};

const FORMATOS_ELEGIBLES: ClaveDeFormato[] = ['cuadrado', 'historia'];
const MENSAJE_SIN_IMAGEN = 'No pudimos armar la imagen. Igual podés copiar el texto.';
const MENSAJE_SIN_COPIAR = 'No pudimos copiar el texto. Seleccionalo y copialo a mano.';

// La promoción lista: el texto para editar y la imagen para compartir, descargar o copiar. Nada se publica solo.
export function VistaPreviaDePromocion({ negocio, canal, promocion, texto, onTexto }: Props) {
  const esLista = promocion.clave === 'lista';
  const formatoDelCanal = formatoPara(canal, promocion);
  const [formatoElegido, setFormatoElegido] = useState<ClaveDeFormato | null>(null);
  const formato = formatoElegido ? FORMATOS_DE_IMAGEN[formatoElegido] : formatoDelCanal;
  const [imagen, setImagen] = useState<{ blob: Blob; direccion: string } | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    let direccion: string | null = null;
    imagenDePromocion(formato, { negocio, promocion })
      .then((blob) => {
        if (!vigente) return;
        direccion = URL.createObjectURL(blob);
        setImagen({ blob, direccion });
        setAviso(null);
      })
      .catch(() => {
        if (vigente) setAviso(MENSAJE_SIN_IMAGEN);
      });
    return () => {
      vigente = false;
      if (direccion) URL.revokeObjectURL(direccion);
    };
  }, [formato, negocio, promocion]);

  const archivo = imagen && { blob: imagen.blob, nombre: `promocion-${promocion.clave}.png` };

  const compartirTodo = () =>
    archivo &&
    compartir({ archivo, texto, telefono: '', titulo: promocion.titulo }).catch(() =>
      setAviso('No pudimos abrir WhatsApp. Descargá la imagen y mandala vos.'),
    );

  const copiarTexto = () =>
    navigator.clipboard
      .writeText(texto)
      .then(() => setAviso('Texto copiado.'))
      .catch(() => setAviso(MENSAJE_SIN_COPIAR));

  return (
    <section className="promociones-vista" aria-label="Tu promoción lista">
      <div className="field">
        <label htmlFor="promocion-texto">Texto (podés cambiarlo)</label>
        <textarea
          className="input"
          id="promocion-texto"
          rows={9}
          value={texto}
          onChange={(evento) => onTexto(evento.target.value)}
        />
      </div>
      {!esLista ? (
        <div className="field">
          <label htmlFor="promocion-formato">Formato de la imagen</label>
          <select
            className="input"
            id="promocion-formato"
            value={formato.clave}
            onChange={(evento) => setFormatoElegido(evento.target.value as ClaveDeFormato)}
          >
            {FORMATOS_ELEGIBLES.map((clave) => (
              <option key={clave} value={clave}>
                {FORMATOS_DE_IMAGEN[clave].nombre}
              </option>
            ))}
          </select>
        </div>
      ) : null}
      {imagen ? (
        <img className="promociones-imagen" src={imagen.direccion} alt={`Imagen de «${promocion.titulo}»`} />
      ) : (
        <p role="status">Armando la imagen…</p>
      )}
      {aviso ? <p role="status">{aviso}</p> : null}
      <div className="promociones-acciones">
        <Btn cls="primary" disabled={!archivo} onClick={() => void compartirTodo()}>
          <Icon name="upload" /> Compartir
        </Btn>
        <Btn cls="secondary" onClick={() => void copiarTexto()}>
          <Icon name="copy" /> Copiar texto
        </Btn>
        <Btn cls="secondary" disabled={!archivo} onClick={() => archivo && guardarArchivo(archivo)}>
          <Icon name="download" /> Descargar imagen
        </Btn>
      </div>
    </section>
  );
}
