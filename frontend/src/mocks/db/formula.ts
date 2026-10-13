import type { Campo } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

// Evaluador de fórmulas para la API simulada: mismo lenguaje que el backend (números, {id-de-columna},
// + - * /, el menos unario y paréntesis). Devuelve el error para mostrar o el valor calculado.

const REFERENCIA = /^\{([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\}/i;
const NUMERO = /^\d+(\.\d+)?/;

type Nodo = (valorDe: (id: string) => number | undefined) => number | undefined;

export type FormulaInterpretada = { referencias: string[]; evaluar: Nodo };

class Lector {
  posicion = 0;
  readonly referencias: string[] = [];

  constructor(private readonly texto: string) {}

  private saltarEspacios() {
    while (/\s/.test(this.texto[this.posicion] ?? '')) this.posicion++;
  }

  private consumir(caracter: string) {
    this.saltarEspacios();
    if (this.texto[this.posicion] !== caracter) return false;
    this.posicion++;
    return true;
  }

  terminado() {
    this.saltarEspacios();
    return this.posicion >= this.texto.length;
  }

  expresion(): Nodo {
    let izquierda = this.termino();
    for (;;) {
      const suma = this.consumir('+');
      if (!suma && !this.consumir('-')) return izquierda;
      izquierda = operar(izquierda, this.termino(), (a, b) => (suma ? a + b : a - b));
    }
  }

  private termino(): Nodo {
    let izquierda = this.factor();
    for (;;) {
      const producto = this.consumir('*');
      if (!producto && !this.consumir('/')) return izquierda;
      izquierda = operar(izquierda, this.factor(), (a, b) =>
        producto ? a * b : b === 0 ? Number.NaN : a / b,
      );
    }
  }

  private factor(): Nodo {
    if (this.terminado()) throw new Error('La fórmula está incompleta');
    if (this.consumir('-')) {
      const interno = this.factor();
      return (valorDe) => {
        const valor = interno(valorDe);
        return valor === undefined ? undefined : -valor;
      };
    }
    if (this.consumir('(')) {
      const interno = this.expresion();
      if (!this.consumir(')')) throw new Error('Falta cerrar un paréntesis');
      return interno;
    }
    const resto = this.texto.slice(this.posicion);
    const referencia = REFERENCIA.exec(resto);
    if (referencia) {
      this.posicion += referencia[0].length;
      const id = (referencia[1] ?? '').toLowerCase();
      this.referencias.push(id);
      return (valorDe) => valorDe(id);
    }
    const numero = NUMERO.exec(resto);
    if (!numero) throw new Error('No se entiende la fórmula');
    this.posicion += numero[0].length;
    const valor = Number(numero[0]);
    return () => valor;
  }
}

function operar(izquierda: Nodo, derecha: Nodo, operacion: (a: number, b: number) => number): Nodo {
  return (valorDe) => {
    const a = izquierda(valorDe);
    const b = derecha(valorDe);
    if (a === undefined || b === undefined) return undefined;
    const resultado = operacion(a, b);
    return Number.isFinite(resultado) ? resultado : undefined;
  };
}

/** Lanza un Error con el mensaje para mostrar si la expresión no es válida. */
export function interpretarFormula(texto: string): FormulaInterpretada {
  if (!texto.trim()) throw new Error('Armá la fórmula');
  const lector = new Lector(texto);
  const evaluar = lector.expresion();
  if (!lector.terminado()) throw new Error('No se entiende la fórmula');
  return { referencias: [...new Set(lector.referencias)], evaluar };
}

/** Copia de los valores de una fila con el resultado de cada columna FORMULA (sin dato si no se puede calcular). */
export function conFormulas(campos: readonly Campo[], valores: Registro['valores']): Registro['valores'] {
  const resultado: Registro['valores'] = { ...valores };
  for (const campo of campos) {
    if (campo.tipo !== 'FORMULA' || !campo.formula) continue;
    const calculado = interpretarFormula(campo.formula).evaluar((id) => {
      const valor = valores[id];
      return typeof valor === 'number' ? valor : undefined;
    });
    if (calculado !== undefined) resultado[campo.id] = Number(calculado.toFixed(10));
  }
  return resultado;
}
