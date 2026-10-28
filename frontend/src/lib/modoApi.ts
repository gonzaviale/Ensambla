// El modo de datos que se elige con el interruptor del header: «mock» (toda la API simulada en el navegador) o
// «real» (el backend). Se guarda en el navegador y se lee al arrancar la app, así que cambiarlo recarga la página.
// Sirve para presentar con el backend real y tener a mano la versión simulada si algo falla.

export type ModoApi = 'mock' | 'real';
type ModoMocks = 'completo' | 'parcial' | 'ninguno';

export const CLAVE_MODO_API = 'ensambla.modo-api';
export const CLAVE_ENTRAR_COMO_DEMO = 'ensambla.mock.entrar-como-demo';

/** El modo elegido con el interruptor, o null si nunca se tocó (o no hay almacenamiento). */
export function modoApiElegido(): ModoApi | null {
  try {
    const valor = localStorage.getItem(CLAVE_MODO_API);
    return valor === 'mock' || valor === 'real' ? valor : null;
  } catch {
    return null;
  }
}

/** Guarda el modo; al pasar a «mock» deja pedido entrar directo con la cuenta de demostración. */
export function guardarModoApi(modo: ModoApi) {
  try {
    localStorage.setItem(CLAVE_MODO_API, modo);
    if (modo === 'mock') localStorage.setItem(CLAVE_ENTRAR_COMO_DEMO, '1');
    else localStorage.removeItem(CLAVE_ENTRAR_COMO_DEMO);
  } catch {
    // Sin almacenamiento (modo privado): el interruptor no puede recordar la elección.
  }
}

/** Lee y borra el pedido de entrar con la cuenta de demostración: vale una sola vez. */
export function tomarEntrarComoDemo(): boolean {
  try {
    const pedido = localStorage.getItem(CLAVE_ENTRAR_COMO_DEMO) === '1';
    localStorage.removeItem(CLAVE_ENTRAR_COMO_DEMO);
    return pedido;
  } catch {
    return false;
  }
}

/**
 * El modo de mocks con el que arranca la app: lo elegido con el interruptor manda sobre lo compilado
 * (VITE_API_MOCKS). «mock» simula todo; «real» usa el backend (o el modo parcial, si así se compiló).
 */
export function modoMocksEfectivo(compilado: ModoMocks, elegido: ModoApi | null): ModoMocks {
  if (elegido === 'mock') return 'completo';
  if (elegido === 'real') return compilado === 'completo' ? 'ninguno' : compilado;
  return compilado;
}
