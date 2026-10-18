import { CATALOG } from './catalogo';
import { hoy } from './formato';
import type { Campo, TipoBloque, Valor } from './tipos';

export const DESCRIPCION_DE_EJEMPLO =
  'Tengo un almacén de barrio. Compro mercadería a distribuidores, vendo al mostrador y anoto lo que me fían y el stock en un cuaderno.';

// La «lectura» de una foto es de ejemplo: propone tres registros con las muestras del bloque, así se
// puede probar el recorrido (revisar, corregir, incorporar) sin un servicio de reconocimiento.
export function filasDeEjemplo(
  tipo: TipoBloque,
  campos: Campo[],
  vinculos: Record<string, string[]>,
): Record<string, Valor>[] {
  const muestras = CATALOG[tipo].examples;
  return Array.from({ length: 3 }, (_, indice) => {
    const ejemplo = muestras[indice % Math.max(muestras.length, 1)] ?? {};
    const fila: Record<string, Valor> = {};
    for (const campo of campos) {
      let valor: Valor = ejemplo[campo.key];
      if (campo.type === 'relation') {
        const ids = vinculos[campo.key] ?? [];
        valor = ids.length ? ids[indice % ids.length] : '';
      } else if (valor == null) {
        valor =
          campo.type === 'select'
            ? (campo.options?.[0] ?? '')
            : campo.type === 'checkbox'
              ? false
              : campo.type === 'date'
                ? hoy()
                : campo.type === 'number' || campo.type === 'currency'
                  ? indice + 1
                  : campo.type === 'email'
                    ? 'contacto@example.com'
                    : campo.required
                      ? `Registro ${indice + 1}`
                      : '';
      }
      fila[campo.key] = valor;
    }
    return fila;
  });
}
