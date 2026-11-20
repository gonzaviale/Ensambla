import type { Aviso, TipoDeAviso } from './alertas';

/**
 * Lo que pide la home, uno de cada: algo que falta, un producto que no se mueve y un precio frente a la zona.
 * Si fueran solo «los más urgentes», tres faltantes de stock taparían a los demás.
 */
const TEMAS: TipoDeAviso[][] = [
  ['stock'],
  ['estancado'],
  ['precio_zona', 'muy_vendido', 'revisar_precio'],
];

/**
 * Las sugerencias de la home: la más urgente de cada tema y, si algún tema no tiene, las siguientes más urgentes.
 * `avisos` ya viene de más a menos urgente y el resultado respeta ese orden.
 */
export function sugerenciasDeLaHome(avisos: Aviso[], maximo = 3): Aviso[] {
  const porTema = TEMAS.map((tipos) => avisos.find((aviso) => tipos.includes(aviso.tipo))).filter(
    (aviso): aviso is Aviso => aviso !== undefined,
  );
  const elegidos = new Set(
    [...porTema, ...avisos.filter((aviso) => !porTema.includes(aviso))].slice(0, maximo),
  );
  return avisos.filter((aviso) => elegidos.has(aviso));
}
