import { CATALOG } from './catalogo';
import { normalize } from './core';
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

// Una columna que una anotación en papel suele traer y que el bloque de ese tipo no tiene todavía.
type ColumnaDeEjemplo = { campo: Campo; valores: Valor[] };

const COLUMNAS_NUEVAS: Partial<Record<TipoBloque, ColumnaDeEjemplo>> = {
  inventory: {
    campo: { key: 'proveedor', label: 'Proveedor', type: 'text', required: false },
    valores: ['Distribuidora Sur', 'Mayorista Norte', 'Distribuidora Sur'],
  },
  clients: {
    campo: { key: 'direccion', label: 'Dirección', type: 'text', required: false },
    valores: ['Belgrano 123', 'San Martín 450', 'Mitre 88'],
  },
  orders: {
    campo: { key: 'telefono', label: 'Teléfono', type: 'tel', required: false },
    valores: ['11 4444-5555', '11 5555-6666', '11 6666-7777'],
  },
  expenses: {
    campo: { key: 'proveedor', label: 'Proveedor', type: 'text', required: false },
    valores: ['Distribuidora Sur', 'Mayorista Norte', 'Distribuidora Sur'],
  },
  sales: {
    campo: { key: 'nota', label: 'Nota', type: 'text', required: false },
    valores: ['Pagó en efectivo', 'Retira mañana', ''],
  },
};

// Las columnas de la foto que el bloque no tiene: se proponen como campos nuevos con el tipo que
// sugieren sus valores. Nunca una que el bloque ya tenga (mismo nombre, sin importar tildes ni mayúsculas).
export function columnaNuevaDeEjemplo(tipo: TipoBloque, campos: Campo[]): ColumnaDeEjemplo | null {
  const columna = COLUMNAS_NUEVAS[tipo];
  if (!columna) return null;
  const repetida = campos.some(
    (campo) =>
      campo.key === columna.campo.key || normalize(campo.label) === normalize(columna.campo.label),
  );
  return repetida ? null : columna;
}

// Datos que «no se leyeron con seguridad»: el segundo registro en su primer texto y el tercero en su
// primer número, para poder probar cómo se marcan y se resuelven.
export function dudasDeEjemplo(campos: Campo[]) {
  const dudas: { fila: number; campo: string; motivo: string }[] = [];
  const texto = campos.find((campo) => campo.type === 'text' || campo.type === 'textarea');
  const numero = campos.find((campo) => campo.type === 'number' || campo.type === 'currency');
  if (texto) dudas.push({ fila: 1, campo: texto.key, motivo: 'La letra no se lee con claridad.' });
  if (numero)
    dudas.push({ fila: 2, campo: numero.key, motivo: 'No estamos seguros de este número.' });
  return dudas;
}

type PedidoDeEjemplo = {
  tipo: TipoBloque;
  campos: Campo[];
  vinculos: Record<string, string[]>;
  destinoExistente: boolean;
};

// Todo lo que devuelve la lectura de ejemplo: los registros, las columnas nuevas (con sus valores en
// cada registro), los datos dudosos y la descripción del negocio.
export function lecturaDeEjemplo({ tipo, campos, vinculos, destinoExistente }: PedidoDeEjemplo) {
  const filas = filasDeEjemplo(tipo, campos, vinculos);
  const nueva = destinoExistente ? columnaNuevaDeEjemplo(tipo, campos) : null;
  if (nueva) {
    filas.forEach((fila, indice) => {
      fila[nueva.campo.key] = nueva.valores[indice % nueva.valores.length] ?? '';
    });
  }
  return {
    filas,
    camposNuevos: nueva ? [nueva.campo] : [],
    dudas: dudasDeEjemplo(campos),
    descripcion: DESCRIPCION_DE_EJEMPLO,
  };
}
