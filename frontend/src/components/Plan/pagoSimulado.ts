// Pago de DEMOSTRACIÓN: nada de esto toca una pasarela real. Sólo valida el formato de lo que se
// escribe y decide si la simulación aprueba o rechaza. El número completo nunca se guarda.

export type DatosTarjeta = { numero: string; nombre: string; vencimiento: string };
export type ErroresTarjeta = Partial<Record<keyof DatosTarjeta, string>>;

/** Tarjeta de prueba: la simulación la rechaza (termina en 0002). */
export const FINAL_RECHAZADO = '0002';
/** Tarjeta de prueba para ver un pago aprobado. */
export const TARJETA_DE_PRUEBA_APROBADA = '4242 4242 4242 4242';
export const TARJETA_DE_PRUEBA_RECHAZADA = '4000 0000 0000 0002';

export const soloDigitos = (texto: string) => texto.replace(/\D/g, '');

/** «4242424242424242» → «4242 4242 4242 4242» (hasta 19 dígitos). */
export function formatearNumero(texto: string): string {
  return (
    soloDigitos(texto)
      .slice(0, 19)
      .match(/.{1,4}/g)
      ?.join(' ') ?? ''
  );
}

/** «1230» → «12/30». */
export function formatearVencimiento(texto: string): string {
  const digitos = soloDigitos(texto).slice(0, 4);
  return digitos.length >= 3 ? `${digitos.slice(0, 2)}/${digitos.slice(2)}` : digitos;
}

export function validarTarjeta(datos: DatosTarjeta, ahora: Date = new Date()): ErroresTarjeta {
  const errores: ErroresTarjeta = {};
  const digitos = soloDigitos(datos.numero);
  if (digitos.length < 13 || digitos.length > 19) {
    errores.numero = 'Revisá el número: tiene que tener entre 13 y 19 dígitos.';
  }
  if (datos.nombre.trim().length < 2) {
    errores.nombre = 'Escribí el nombre como figura en la tarjeta.';
  }
  const coincide = /^(\d{2})\/(\d{2})$/.exec(datos.vencimiento);
  if (!coincide) {
    errores.vencimiento = 'Escribilo así: mes y año, por ejemplo 12/30.';
  } else {
    const mes = Number(coincide[1]);
    const anio = 2000 + Number(coincide[2]);
    if (mes < 1 || mes > 12) {
      errores.vencimiento = 'El mes tiene que estar entre 01 y 12.';
    } else if (anio * 12 + mes < ahora.getFullYear() * 12 + ahora.getMonth() + 1) {
      errores.vencimiento = 'Esa tarjeta está vencida.';
    }
  }
  return errores;
}

export function marcaDe(numero: string): string {
  const digitos = soloDigitos(numero);
  if (digitos.startsWith('4')) return 'Visa';
  if (/^(5[1-5]|2[2-7])/.test(digitos)) return 'Mastercard';
  return 'Tarjeta';
}

/** «Visa •••• 4242»: lo único que se conserva de la tarjeta. */
export function detalleDeTarjeta(numero: string): string {
  return `${marcaDe(numero)} •••• ${soloDigitos(numero).slice(-4)}`;
}

export const tarjetaSeRechaza = (numero: string) => soloDigitos(numero).endsWith(FINAL_RECHAZADO);
