// Preparación de las fotos del cuaderno en el navegador, antes de subirlas.

export type Rotacion = 0 | 90 | 180 | 270;

export const girar = (rotacion: Rotacion): Rotacion => ((rotacion + 90) % 360) as Rotacion;

// Al girar 90° o 270° se intercambian el ancho y el alto del lienzo.
export function medidasRotadas(ancho: number, alto: number, rotacion: Rotacion) {
  return rotacion % 180 === 0 ? { ancho, alto } : { ancho: alto, alto: ancho };
}

function aBlob(lienzo: HTMLCanvasElement, calidad: number) {
  return new Promise<Blob | null>((resolve) => lienzo.toBlob(resolve, 'image/jpeg', calidad));
}

/**
 * Devuelve la foto girada como el usuario la dejó en la miniatura. Si el navegador no puede
 * decodificarla (por ejemplo HEIC fuera de Safari), se manda el archivo original.
 */
export async function prepararFoto(archivo: File, rotacion: Rotacion): Promise<Blob> {
  if (rotacion === 0 || typeof createImageBitmap !== 'function') return archivo;
  try {
    const imagen = await createImageBitmap(archivo, { imageOrientation: 'from-image' });
    const { ancho, alto } = medidasRotadas(imagen.width, imagen.height, rotacion);
    const lienzo = document.createElement('canvas');
    lienzo.width = ancho;
    lienzo.height = alto;
    const contexto = lienzo.getContext('2d');
    if (!contexto) return archivo;
    contexto.translate(ancho / 2, alto / 2);
    contexto.rotate((rotacion * Math.PI) / 180);
    contexto.drawImage(imagen, -imagen.width / 2, -imagen.height / 2);
    imagen.close();
    return (await aBlob(lienzo, 0.92)) ?? archivo;
  } catch {
    return archivo;
  }
}
