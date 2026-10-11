// Preparación de las fotos del cuaderno en el navegador, antes de subirlas (T-091 y T-092).

export type Rotacion = 0 | 90 | 180 | 270;

export const LADO_MAYOR_MAXIMO = 2000;
export const PESO_MAXIMO_BYTES = 1.5 * 1024 * 1024;
// Se prueba de mayor a menor calidad hasta que la foto entra en el peso máximo.
const CALIDADES_JPEG = [0.85, 0.75, 0.65, 0.55];

export const girar = (rotacion: Rotacion): Rotacion => ((rotacion + 90) % 360) as Rotacion;

// Al girar 90° o 270° se intercambian el ancho y el alto del lienzo.
export function medidasRotadas(ancho: number, alto: number, rotacion: Rotacion) {
  return rotacion % 180 === 0 ? { ancho, alto } : { ancho: alto, alto: ancho };
}

// Achica sin deformar hasta que el lado mayor mida como mucho `ladoMayor`; nunca agranda.
export function medidasReducidas(ancho: number, alto: number, ladoMayor = LADO_MAYOR_MAXIMO) {
  const escala = Math.min(1, ladoMayor / Math.max(ancho, alto));
  return { ancho: Math.round(ancho * escala), alto: Math.round(alto * escala) };
}

function aBlob(lienzo: HTMLCanvasElement, calidad: number) {
  return new Promise<Blob | null>((resolve) => lienzo.toBlob(resolve, 'image/jpeg', calidad));
}

async function comprimir(lienzo: HTMLCanvasElement) {
  let blob: Blob | null = null;
  for (const calidad of CALIDADES_JPEG) {
    blob = await aBlob(lienzo, calidad);
    if (!blob || blob.size <= PESO_MAXIMO_BYTES) return blob;
  }
  return blob;
}

/**
 * Devuelve la foto lista para subir: derecha según la orientación EXIF de la cámara, con el giro que eligió el
 * usuario, con el lado mayor en 2000px como máximo y comprimida en JPEG. Si el navegador no puede decodificarla
 * (por ejemplo HEIC fuera de Safari), se manda el archivo original.
 */
export async function prepararFoto(archivo: File, rotacion: Rotacion): Promise<Blob> {
  if (typeof createImageBitmap !== 'function') return archivo;
  try {
    // 'from-image' aplica la orientación EXIF: la foto queda como se ve en la cámara.
    const imagen = await createImageBitmap(archivo, { imageOrientation: 'from-image' });
    const reducida = medidasReducidas(imagen.width, imagen.height);
    const { ancho, alto } = medidasRotadas(reducida.ancho, reducida.alto, rotacion);
    const lienzo = document.createElement('canvas');
    lienzo.width = ancho;
    lienzo.height = alto;
    const contexto = lienzo.getContext('2d');
    if (!contexto) return archivo;
    contexto.translate(ancho / 2, alto / 2);
    contexto.rotate((rotacion * Math.PI) / 180);
    contexto.drawImage(
      imagen,
      -reducida.ancho / 2,
      -reducida.alto / 2,
      reducida.ancho,
      reducida.alto,
    );
    imagen.close();
    return (await comprimir(lienzo)) ?? archivo;
  } catch {
    return archivo;
  }
}
