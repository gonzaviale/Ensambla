import type { TipoCampo } from '@/types/campo';

// Cómo se le presenta cada tipo a la persona: nombre cotidiano y un ejemplo, sin jerga técnica.
export const TIPOS_DE_DATO: Record<TipoCampo, { nombre: string; ayuda: string }> = {
  TEXTO: { nombre: 'Texto', ayuda: 'Palabras o frases, como un nombre o una dirección.' },
  NUMERO: { nombre: 'Número', ayuda: 'Cantidades o precios, como 12 o 1500,50.' },
  FECHA: { nombre: 'Fecha', ayuda: 'Un día del calendario, como el día de entrega.' },
  OPCION: {
    nombre: 'Lista de opciones',
    ayuda: 'Se elige una opción de una lista que armás vos, como Chico, Mediano o Grande.',
  },
  SI_NO: { nombre: 'Sí o no', ayuda: 'Una respuesta de sí o no, como «¿Pagó?».' },
  RELACION: {
    nombre: 'Conexión con otra tabla',
    ayuda: 'Cada fila elige una fila de otra tabla, como el producto de una venta.',
  },
  FORMULA: {
    nombre: 'Cálculo',
    ayuda: 'Se calcula solo con otras columnas numéricas, como cantidad por precio.',
  },
};
