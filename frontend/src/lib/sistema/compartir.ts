import { guardarArchivo } from '@/lib/descargas';

/** Un archivo que armó el servidor o el navegador, con el nombre que sugiere. */
export type ArchivoParaCompartir = { blob: Blob; nombre: string };

/**
 * Lo que se le hace llegar a alguien: el archivo, el mensaje que lo acompaña y el WhatsApp de esa persona (puede estar
 * vacío: entonces se elige a quién mandárselo). `titulo` es el asunto cuando el sistema ofrece compartir.
 */
export type ParaCompartir = {
  archivo: ArchivoParaCompartir;
  texto: string;
  telefono: string;
  titulo: string;
};

/** Una forma de hacerle llegar el archivo a alguien; cada una sabe si el navegador la permite. */
export interface FormaDeCompartir {
  disponible(archivo: File): boolean;
  compartir(para: ParaCompartir, archivo: File): Promise<void>;
}

const esCancelacion = (error: unknown) => error instanceof DOMException && error.name === 'AbortError';

/** El enlace que abre WhatsApp con el mensaje; sin número se elige a quién mandárselo. */
export function enlaceDeWhatsapp(telefono: string, texto: string): string {
  const numero = telefono.replace(/\D/g, '');
  return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
}

/** En el celular: la hoja de compartir del sistema lleva el archivo directo a WhatsApp. */
class HojaDeCompartirDelCelular implements FormaDeCompartir {
  disponible(archivo: File) {
    return typeof navigator.canShare === 'function' && navigator.canShare({ files: [archivo] });
  }

  async compartir({ texto, titulo }: ParaCompartir, archivo: File) {
    try {
      await navigator.share({ files: [archivo], text: texto, title: titulo });
    } catch (error) {
      if (!esCancelacion(error)) throw error;
    }
  }
}

/** En la computadora: se baja el archivo y se abre WhatsApp Web con el mensaje, para adjuntarlo. */
class DescargaYWhatsappWeb implements FormaDeCompartir {
  disponible() {
    return true;
  }

  async compartir({ archivo, texto, telefono }: ParaCompartir) {
    guardarArchivo(archivo);
    window.open(enlaceDeWhatsapp(telefono, texto), '_blank', 'noopener');
  }
}

const PREFERIDAS: FormaDeCompartir[] = [new HojaDeCompartirDelCelular()];
const RESPALDO: FormaDeCompartir = new DescargaYWhatsappWeb();

/** La forma que le toca a este navegador y archivo: la del celular si se puede, si no el respaldo. */
export function formaDeCompartirPara(archivo: File): FormaDeCompartir {
  return PREFERIDAS.find((forma) => forma.disponible(archivo)) ?? RESPALDO;
}

export function compartir(para: ParaCompartir): Promise<void> {
  const archivo = new File([para.archivo.blob], para.archivo.nombre, { type: para.archivo.blob.type });
  return formaDeCompartirPara(archivo).compartir(para, archivo);
}
