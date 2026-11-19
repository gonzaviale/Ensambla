import { expect } from 'vitest';
import type { Campo, Fila } from '@/lib/sistema/tipos';
import {
  agregarPieza,
  dadoQueSamiYaPidio,
  dadoUnAlmacenDeEjemplo,
  editarCampo,
  piezaDelTipo,
} from './accionesDeSami';

let filas: Fila[] = [];

const campoLlamado = (tipo: string, etiqueta: string) =>
  piezaDelTipo(tipo).fields.find((campo) => campo.label === etiqueta);

const campoDeClave = (tipo: string, clave: string) =>
  piezaDelTipo(tipo).fields.find((campo) => campo.key === clave);

export const dadoUnInventarioQueYaUsaLaClaveRubro = () => {
  dadoUnAlmacenDeEjemplo();
  piezaDelTipo('inventory').fields.push({
    key: 'rubro',
    label: 'Otro',
    type: 'text',
    required: false,
  });
};

export const dadoUnInventarioConMinimosEscritosDeDistintasFormas = () => {
  dadoUnAlmacenDeEjemplo();
  const inventario = piezaDelTipo('inventory');
  const [primera, segunda] = inventario.rows as [Fila, Fila];
  primera.minimum = 5;
  segunda.minimum = 'mucho';
  const real: Fila = { id: 'real', name: 'Arroz', minimum: '7' };
  inventario.rows.push(real);
  filas = [primera, segunda, real];
};

export const dadosClientesConNotasQueEstanYQueNoEstanEnLaLista = () => {
  dadoUnAlmacenDeEjemplo();
  const [primera, segunda] = piezaDelTipo('clients').rows as [Fila, Fila];
  primera.notes = 'mayorista';
  segunda.notes = 'Otra cosa';
  filas = [primera, segunda];
};

export const dadoUnAlmacenConGastos = () => {
  dadoUnAlmacenDeEjemplo();
  dadoQueSamiYaPidio(agregarPieza('expenses'));
};

export const dadosGastosConLaCategoriaComoListaPropia = () => {
  dadoUnAlmacenConGastos();
  dadoQueSamiYaPidio(
    editarCampo('expenses', 'Categoría', { opciones: ['Mercadería', 'Impuestos'] }),
  );
};

export const entoncesElUltimoDatoEs = (tipo: string, esperado: Partial<Campo>) => {
  expect(piezaDelTipo(tipo).fields.at(-1)).toMatchObject(esperado);
};

export const entoncesLaPiezaYaNoTieneElDato = (tipo: string, clave: string) => {
  expect(campoDeClave(tipo, clave)).toBeUndefined();
  expect(piezaDelTipo(tipo).rows.every((fila) => !(clave in fila))).toBe(true);
};

export const entoncesLosDatosSon = (tipo: string, esperados: Record<string, Partial<Campo>>) => {
  const reales = Object.fromEntries(
    Object.keys(esperados).map((etiqueta) => [etiqueta, campoLlamado(tipo, etiqueta)]),
  );
  expect(reales).toMatchObject(esperados);
};

export const entoncesLaEtiquetaDeLaClave = (tipo: string, clave: string, etiqueta: string) => {
  expect(campoDeClave(tipo, clave)?.label).toBe(etiqueta);
};

export const entoncesLaEtiquetaDeLaClaveMide = (tipo: string, clave: string, largo: number) => {
  expect(campoDeClave(tipo, clave)?.label).toHaveLength(largo);
};

export const entoncesLaPrimeraFilaConserva = (tipo: string, clave: string, valor: unknown) => {
  expect(piezaDelTipo(tipo).rows[0]?.[clave]).toBe(valor);
};

export const entoncesLasEtiquetasSon = (tipo: string, etiquetas: string[]) => {
  expect(piezaDelTipo(tipo).fields.map((campo) => campo.label)).toEqual(etiquetas);
};

export const entoncesLaPrimeraEtiquetaEs = (tipo: string, etiqueta: string) => {
  expect(piezaDelTipo(tipo).fields[0]?.label).toBe(etiqueta);
};

export const entoncesLosMinimosQuedanComoNumeroYLoQueNoSirveSeBorra = () => {
  const [primera, segunda, real] = filas;
  expect(primera?.minimum).toBe(5);
  expect(segunda && 'minimum' in segunda).toBe(false);
  expect(real?.minimum).toBe(7);
};

export const entoncesLasNotasQueNoEstanEnLaListaSeBorran = () => {
  const [primera, segunda] = filas;
  expect(primera?.notes).toBe('Mayorista');
  expect(segunda && 'notes' in segunda).toBe(false);
};

export const entoncesLasCategoriasDeLosGastosSon = (categorias: unknown[]) => {
  expect(piezaDelTipo('expenses').rows.map((fila) => fila.category)).toEqual(categorias);
};

export const entoncesLaCategoriaEsTextoSinOpciones = () => {
  const categoria = campoLlamado('expenses', 'Categoría');
  expect(categoria?.type).toBe('text');
  expect(categoria?.options).toBeUndefined();
};
