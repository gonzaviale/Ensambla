/// <reference types="node" />
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { CATALOG } from '@/lib/sistema/catalogo';
import { catalogoParaSami, catalogoParaSamiComoTexto } from '@/lib/sistema/catalogoSami';

// El backend lee este archivo para saber qué componentes puede ensamblar Sami. Con
// `npm run catalogo:sami` (modo actualizar-catalogo-sami) el test lo reescribe desde el catálogo del editor.
const ARCHIVO = resolve(process.cwd(), '../backend/src/main/resources/ia/catalogo-componentes.json');

describe('catálogo de componentes para Sami', () => {
  it('el backend tiene el mismo catálogo que el editor', () => {
    const esperado = catalogoParaSamiComoTexto();
    if (import.meta.env.MODE === 'actualizar-catalogo-sami') writeFileSync(ARCHIVO, esperado, 'utf8');
    const actual = readFileSync(ARCHIVO, 'utf8').replace(/\r\n/g, '\n');
    expect(actual, 'Corré `npm run catalogo:sami` y commiteá el archivo.').toBe(esperado);
  });

  it('exporta todos los bloques del editor con sus campos', () => {
    const catalogo = catalogoParaSami();
    expect(catalogo.componentes.map((componente) => componente.tipo)).toEqual(Object.keys(CATALOG));
    const inventario = catalogo.componentes.find((componente) => componente.tipo === 'inventory');
    expect(inventario?.campos[0]).toEqual({
      clave: 'name',
      etiqueta: 'Producto',
      tipo: 'text',
      obligatorio: true,
    });
    const gastos = catalogo.componentes.find((componente) => componente.tipo === 'expenses');
    expect(gastos?.campos.find((campo) => campo.clave === 'category')?.opciones).toContain(
      'Alquiler',
    );
  });
});
