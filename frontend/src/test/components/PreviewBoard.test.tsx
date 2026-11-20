import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { PreviewBoard } from '@/components/sistema/PreviewBoard';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import type { Bloque, Sistema } from '@/lib/sistema/tipos';

let sistema: Sistema;
let inventario: Bloque;
let contenedor: HTMLElement;

describe('las filas de ejemplo en la vista previa', () => {
  it('se ven distintas: etiqueta «Ejemplo» y estilo propio', () => {
    dadoUnComercioConFilasDeEjemplo();

    cuandoSeMuestraLaVistaPrevia();

    entoncesLasFilasDeEjemploTienenSuEstiloYSuEtiqueta();
  });

  it('no muestran avisos ni píldoras de columnas', () => {
    dadoUnComercioConFilasDeEjemplo();

    cuandoSeMuestraLaVistaPrevia();

    entoncesNoHayAvisosNiPildorasDeColumnas();
  });

  it('las filas reales no llevan marca', () => {
    dadoUnComercioConFilasReales();

    cuandoSeMuestraLaVistaPrevia();

    entoncesNingunaFilaLlevaMarca();
  });

  it('es sólo de lectura: no deja editar ni agregar registros', () => {
    dadoUnComercioConFilasDeEjemplo();

    cuandoSeMuestraLaVistaPrevia();

    entoncesNoSePuedeEditarNiAgregar();
  });

  it('con más filas de las que entran muestra sólo las primeras', () => {
    dadoUnInventarioConDieciseisFilas();

    cuandoSeMuestraLaVistaPrevia();

    entoncesLaTablaMuestraFilas(3);
  });

  it('con más filas de las que entran ofrece «Ver todas»', async () => {
    dadoUnInventarioConDieciseisFilas();
    cuandoSeMuestraLaVistaPrevia();

    await cuandoSePideVerTodas();

    entoncesLaTablaMuestraFilas(16);
  });

  it('después de «Ver todas» ofrece «Ver menos» y vuelve a las filas de antes', async () => {
    dadoUnInventarioConDieciseisFilas();
    cuandoSeMuestraLaVistaPrevia();
    await cuandoSePideVerTodas();

    await cuandoSePideVerMenos();

    entoncesLaTablaMuestraFilas(3);
  });
});

function dadoUnComercioConFilasDeEjemplo() {
  sistema = makeTemplate('shop', {}, true);
  inventario = elInventario();
}

function dadoUnComercioConFilasReales() {
  dadoUnComercioConFilasDeEjemplo();
  for (const bloque of allBlocks(sistema)) {
    for (const fila of bloque.rows) delete fila.__example;
  }
}

function dadoUnInventarioConDieciseisFilas() {
  dadoUnComercioConFilasDeEjemplo();
  const base = inventario.rows[0];
  inventario.rows = Array.from({ length: 16 }, (_, i) => ({
    ...base,
    id: `fila-${i}`,
    __example: undefined,
  }));
}

function cuandoSeMuestraLaVistaPrevia() {
  contenedor = render(<PreviewBoard sistema={sistema} />).container;
}

async function cuandoSePideVerTodas() {
  await userEvent.click(within(laTabla()).getByRole('button', { name: 'Ver todas (16)' }));
}

async function cuandoSePideVerMenos() {
  await userEvent.click(within(laTabla()).getByRole('button', { name: 'Ver menos' }));
}

function elInventario(): Bloque {
  const encontrado = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!encontrado) throw new Error('Falta el inventario');
  return encontrado;
}

function laTabla(): HTMLElement {
  const tabla = screen.getByText(inventario.name).closest('section');
  if (!tabla) throw new Error('Falta el bloque');
  return tabla;
}

function entoncesLasFilasDeEjemploTienenSuEstiloYSuEtiqueta() {
  expect(contenedor.querySelectorAll('tr.example-row').length).toBeGreaterThan(0);
  expect(within(laTabla()).getAllByText('Ejemplo').length).toBeGreaterThan(0);
}

function entoncesNoHayAvisosNiPildorasDeColumnas() {
  expect(screen.queryByText('Datos de ejemplo, no son reales')).not.toBeInTheDocument();
  expect(contenedor.querySelector('.preview-field-tags')).toBeNull();
}

function entoncesNingunaFilaLlevaMarca() {
  expect(contenedor.querySelector('tr.example-row')).toBeNull();
  expect(screen.queryByText('Ejemplo')).not.toBeInTheDocument();
}

function entoncesNoSePuedeEditarNiAgregar() {
  expect(screen.queryByRole('button', { name: 'Editar registro' })).toBeNull();
  expect(screen.queryByRole('button', { name: '+ Agregar registro' })).toBeNull();
}

function entoncesLaTablaMuestraFilas(cantidad: number) {
  expect(within(laTabla()).getAllByRole('row')).toHaveLength(1 + cantidad);
}
