import type { FormatoDeImagen, TarjetaDePromocion } from './formatosDeImagen';

const MENSAJE_SIN_LIENZO = 'Este navegador no puede armar la imagen.';
const MENSAJE_SIN_IMAGEN = 'No pudimos armar la imagen.';

/** La promoción dibujada en el formato elegido, como un PNG listo para compartir o descargar. */
export function imagenDePromocion(formato: FormatoDeImagen, tarjeta: TarjetaDePromocion): Promise<Blob> {
  const lienzo = document.createElement('canvas');
  lienzo.width = formato.ancho;
  lienzo.height = formato.alto;
  const contexto = lienzo.getContext('2d');
  if (!contexto) return Promise.reject(new Error(MENSAJE_SIN_LIENZO));
  formato.dibujar(contexto, tarjeta);
  return new Promise((resolver, rechazar) =>
    lienzo.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error(MENSAJE_SIN_IMAGEN))), 'image/png'),
  );
}
