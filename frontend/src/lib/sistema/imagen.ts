// Las fotos de un celular pesan varios MB y el servicio de lectura admite hasta 10 MB por imagen: antes de
// enviarla se achica (sin perder lo legible de una anotación) y se guarda como JPG.
const LADO_MAXIMO = 2000;
const BYTES_SIN_ACHICAR = 1_500_000;
const CALIDAD_JPG = 0.85;

export function dimensionesReducidas(ancho: number, alto: number, maximo = LADO_MAXIMO) {
  const lado = Math.max(ancho, alto);
  if (lado <= maximo) return { ancho, alto };
  const escala = maximo / lado;
  return { ancho: Math.round(ancho * escala), alto: Math.round(alto * escala) };
}

const aJpg = (canvas: HTMLCanvasElement) =>
  new Promise<Blob | null>((resolver) => canvas.toBlob(resolver, 'image/jpeg', CALIDAD_JPG));

// Si el navegador no puede achicarla (o la foto ya es liviana), se envía tal cual.
export async function reducirImagen(imagen: File): Promise<Blob> {
  if (typeof createImageBitmap !== 'function' || imagen.type === 'image/gif') return imagen;
  try {
    const mapa = await createImageBitmap(imagen, { imageOrientation: 'from-image' });
    const { ancho, alto } = dimensionesReducidas(mapa.width, mapa.height);
    if (ancho === mapa.width && imagen.size <= BYTES_SIN_ACHICAR) {
      mapa.close();
      return imagen;
    }
    const canvas = document.createElement('canvas');
    canvas.width = ancho;
    canvas.height = alto;
    const contexto = canvas.getContext('2d');
    if (!contexto) {
      mapa.close();
      return imagen;
    }
    // Un PNG con transparencia pasaría a negro al guardarlo como JPG.
    contexto.fillStyle = '#ffffff';
    contexto.fillRect(0, 0, ancho, alto);
    contexto.drawImage(mapa, 0, 0, ancho, alto);
    mapa.close();
    const reducida = await aJpg(canvas);
    return reducida && reducida.size < imagen.size ? reducida : imagen;
  } catch {
    return imagen;
  }
}
