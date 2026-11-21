import { expect } from 'vitest';
import { claseDeLaCelda } from '@/lib/sistema/celdas';
import { comportamientoDe } from '@/lib/sistema/comportamiento';
import type { Bloque, Campo, Fila, Sistema, TipoBloque, TipoCampo, Valor } from '@/lib/sistema/tipos';

let campo: Campo;
let bloque: Bloque;
let sistema: Sistema | undefined;
let resultado: unknown;

const unCampo = (type: TipoCampo, extra: Partial<Campo> = {}): Campo =>
  ({ key: 'valor', label: 'Valor', type, ...extra }) as Campo;

const unBloque = (type: TipoBloque, fields: Campo[] = [], rows: Fila[] = []): Bloque =>
  ({ id: 'b1', type, name: 'Bloque', fields, rows }) as unknown as Bloque;

export const dadoUnCampoDeTipo = (tipo: TipoCampo) => {
  campo = unCampo(tipo);
  sistema = undefined;
};

export const dadoUnCampoVinculadoAUnaTablaConUnRegistro = (nombre: string, registroId: string) => {
  const clientes = unBloque('clients', [unCampo('text', { key: 'name' })], [
    { id: registroId, name: nombre } as Fila,
  ]);
  clientes.id = 'clientes';
  campo = unCampo('relation', { target: 'clientes' });
  sistema = { sections: [{ id: 's', name: 'S', blocks: [clientes] }] } as unknown as Sistema;
};

export const dadoUnInventarioConElStockEn = (cantidad: number, minimo: number) => {
  campo = unCampo('number', { key: 'quantity' });
  bloque = unBloque('inventory', [campo], [{ id: 'f1', quantity: cantidad, minimum: minimo } as Fila]);
};

export const dadoUnaTablaDeClientesConLaCantidad = (cantidad: number) => {
  campo = unCampo('number', { key: 'quantity' });
  bloque = unBloque('clients', [campo], [{ id: 'f1', quantity: cantidad, minimum: 5 } as Fila]);
};

export const cuandoSeMuestraEnUnaTabla = (valor: Valor) => {
  resultado = comportamientoDe(campo.type).enTabla(campo, valor, sistema);
};

export const cuandoSeMuestraEnLaVistaPrevia = (valor: Valor) => {
  resultado = comportamientoDe(campo.type).enVistaPrevia(campo, valor, sistema);
};

export const cuandoSePideLaMarcaDeLaColumna = () => {
  resultado = comportamientoDe(campo.type).marcaDeColumna();
};

export const cuandoSePreguntaSiOcupaTodoElAncho = () => {
  resultado = comportamientoDe(campo.type).ocupaTodoElAncho();
};

export const cuandoSePideLaClaseDeLaCelda = () => {
  const [fila] = bloque.rows;
  resultado = fila ? claseDeLaCelda(bloque, campo, fila) : undefined;
};

export const entoncesSeVe = (texto: string) => expect(resultado).toBe(texto);

export const entoncesNoTieneUnaVistaPropia = () => expect(resultado).toBeUndefined();

export const entoncesLaCeldaEsDeStockBajo = () => expect(resultado).toBe('low-stock');

export const entoncesLaCeldaNoTieneClase = () => expect(resultado).toBe('');

export const entoncesOcupaTodoElAncho = () => expect(resultado).toBe(true);

export const entoncesNoOcupaTodoElAncho = () => expect(resultado).toBe(false);
