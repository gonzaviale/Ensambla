/// <reference types="node" />
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { CATALOG } from '@/lib/sistema/catalogo';
import { catalogoParaSami, catalogoParaSamiComoTexto } from '@/lib/sistema/catalogoSami';

// El backend lee este archivo para saber qué componentes puede ensamblar Sami. Con
// `npm run catalogo:sami` (modo actualizar-catalogo-sami) el test lo reescribe desde el catálogo del editor.
const ARCHIVO = resolve(
  process.cwd(),
  '../backend/src/main/resources/ia/catalogo-componentes.json',
);

type Catalogo = ReturnType<typeof catalogoParaSami>;
type Componente = Catalogo['componentes'][number];

let catalogo: Catalogo;
let textoDelBackend = '';

describe('catálogo de componentes para Sami', () => {
  it('el backend tiene el mismo catálogo que el editor', () => {
    cuandoSeLeeElCatalogoDelBackend();

    entoncesCoincideConElDelEditor();
  });

  it('exporta todos los bloques del editor', () => {
    cuandoSeExportaElCatalogo();

    entoncesTraeUnComponentePorCadaBloqueDelEditor();
  });

  it('el inventario empieza por el campo obligatorio del producto', () => {
    cuandoSeExportaElCatalogo();

    entoncesElInventarioEmpiezaPorElProducto();
  });

  it('los gastos ofrecen el alquiler entre sus categorías', () => {
    cuandoSeExportaElCatalogo();

    entoncesLosGastosOfrecenAlquiler();
  });
});

function cuandoSeLeeElCatalogoDelBackend() {
  if (import.meta.env.MODE === 'actualizar-catalogo-sami')
    writeFileSync(ARCHIVO, catalogoParaSamiComoTexto(), 'utf8');
  textoDelBackend = readFileSync(ARCHIVO, 'utf8').replace(/\r\n/g, '\n');
}

function cuandoSeExportaElCatalogo() {
  catalogo = catalogoParaSami();
}

function entoncesCoincideConElDelEditor() {
  expect(textoDelBackend, 'Corré `npm run catalogo:sami` y commiteá el archivo.').toBe(
    catalogoParaSamiComoTexto(),
  );
}

function entoncesTraeUnComponentePorCadaBloqueDelEditor() {
  expect(catalogo.componentes.map((componente) => componente.tipo)).toEqual(Object.keys(CATALOG));
}

const componenteDe = (tipo: string): Componente | undefined =>
  catalogo.componentes.find((componente) => componente.tipo === tipo);

function entoncesElInventarioEmpiezaPorElProducto() {
  expect(componenteDe('inventory')?.campos[0]).toEqual({
    clave: 'name',
    etiqueta: 'Producto',
    tipo: 'text',
    obligatorio: true,
  });
}

function entoncesLosGastosOfrecenAlquiler() {
  const categoria = componenteDe('expenses')?.campos.find((campo) => campo.clave === 'category');
  expect(categoria?.opciones).toContain('Alquiler');
}
