import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

// Los tests corren sin CSS (css: false), así que el alto mínimo del botón se verifica sobre la hoja de estilos.
const hoja = readFileSync(resolve(__dirname, '../../styles/sistema/zona.css'), 'utf-8');

function reglaDe(selector: string): string {
  const inicio = hoja.indexOf(`${selector} {`);
  if (inicio < 0) return '';
  return hoja.slice(inicio, hoja.indexOf('}', inicio));
}

describe('estilos de la pantalla Zona', () => {
  it('el botón «Analizar la zona» mide al menos 48 px de alto', () => {
    expect(reglaDe('.zona-otra-acciones .btn')).toMatch(/min-height:\s*48px/);
  });
});
