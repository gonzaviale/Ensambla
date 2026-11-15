import { describe, it } from 'vitest';
import {
  givenFormularioImporteLibre,
  whenSeAgregaServicio,
  thenSeAgregaImporteSinProducto,
  thenSeRechazaImporteCero,
  givenDosImportesLibres,
  thenNoSeAgrupanImportesDistintos,
} from '@/test/caja/importeLibreHelpers';

describe('importe libre de caja', () => {
  it('agrega una descripción y un importe a la compra sin crear un producto', async () => {
    const { user, agregado, cerrado } = givenFormularioImporteLibre();
    await whenSeAgregaServicio(user);
    thenSeAgregaImporteSinProducto(agregado, cerrado);
  });
  it('mantiene abierto el formulario cuando el importe no es positivo', async () => {
    const { user, cerrado } = givenFormularioImporteLibre();
    await whenSeAgregaServicio(user, '0');
    thenSeRechazaImporteCero(cerrado);
  });
  it('conserva separados los importes libres y envía sus descripciones al cobrar', () => {
    const lineas = givenDosImportesLibres();
    thenNoSeAgrupanImportesDistintos(lineas);
  });
});
