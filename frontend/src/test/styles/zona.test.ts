import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

// Los tests corren sin CSS (css: false), así que el alto mínimo del botón se verifica sobre la hoja de estilos.
const hoja = readFileSync(resolve(__dirname, '../../styles/sistema/zona.css'), 'utf-8');

let regla = '';

describe('estilos de la pantalla Zona', () => {
  it('el botón «Analizar la zona» mide al menos 48 px de alto', () => {
    cuandoSeLeeLaReglaDe('.zona-otra-acciones .btn');

    entoncesLaReglaPideUnAltoMinimoDe48Px();
  });
});

function cuandoSeLeeLaReglaDe(selector: string) {
  const inicio = hoja.indexOf(`${selector} {`);
  regla = inicio < 0 ? '' : hoja.slice(inicio, hoja.indexOf('}', inicio));
}

function entoncesLaReglaPideUnAltoMinimoDe48Px() {
  expect(regla).toMatch(/min-height:\s*48px/);
}
