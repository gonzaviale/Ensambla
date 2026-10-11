import { describe, expect, it } from 'vitest';
import {
  cambiosParaEditar,
  validarFormulario,
  valoresDeRegistro,
  valoresIniciales,
  valoresParaGuardar,
} from '@/lib/formularioRegistro';
import { campoPorTipo, camposFormulario } from './formularioTestData';

describe('Validación de registros', () => {
  it.each(['TEXTO', 'NUMERO', 'FECHA', 'OPCION', 'SI_NO'] as const)(
    'rechaza un %s obligatorio vacío, incluyendo espacios',
    (tipo) => {
      const campo = { ...campoPorTipo(tipo), obligatorio: true };
      expect(validarFormulario([campo], { [campo.id]: '   ' })).toEqual({
        [campo.id]: 'Este dato es obligatorio',
      });
    },
  );

  it.each([
    ['TEXTO', 'a'.repeat(1001), 'Máximo 1000 caracteres'],
    ['NUMERO', 'Infinity', 'Tiene que ser un número'],
    ['NUMERO', '1e999', 'Tiene que ser un número'],
    ['NUMERO', '0xff', 'Tiene que ser un número'],
    ['NUMERO', 'abc', 'Tiene que ser un número'],
    ['FECHA', '2025-02-29', 'Tiene que ser una fecha válida'],
    ['FECHA', '2026-04-31', 'Tiene que ser una fecha válida'],
    ['FECHA', '10/10/2026', 'Tiene que ser una fecha válida'],
    ['OPCION', 'Otro', 'Elegí una de las opciones'],
    ['SI_NO', 'sí', 'Tiene que ser sí o no'],
  ] as const)('rechaza %s inválido', (tipo, valor, mensaje) => {
    const campo = campoPorTipo(tipo);
    expect(validarFormulario([campo], { [campo.id]: valor })).toEqual({ [campo.id]: mensaje });
  });

  it.each([
    ['TEXTO', 'a'.repeat(1000)],
    ['NUMERO', '0'],
    ['NUMERO', '-12.5'],
    ['NUMERO', '1e3'],
    ['FECHA', '2024-02-29'],
    ['SI_NO', 'false'],
    ['SI_NO', 'true'],
    ['OPCION', 'Insumos'],
  ] as const)('acepta %s válido', (tipo, valor) => {
    const campo = campoPorTipo(tipo);
    expect(validarFormulario([campo], { [campo.id]: valor })).toEqual({});
  });

  it('omite opcionales vacíos, recorta el texto y conserva cero y falso', () => {
    const valores = valoresIniciales(camposFormulario);
    valores[campoPorTipo('TEXTO').id] = '  Producto  ';
    valores[campoPorTipo('FECHA').id] = '';
    valores.ajeno = 'no se envía';
    expect(validarFormulario(camposFormulario, valores)).toEqual({});
    expect(valoresParaGuardar(camposFormulario, valores)).toEqual({
      [campoPorTipo('TEXTO').id]: 'Producto',
      [campoPorTipo('NUMERO').id]: 0,
      [campoPorTipo('SI_NO').id]: false,
      [campoPorTipo('OPCION').id]: 'Insumos',
    });
  });
});

describe('Edición de registros', () => {
  it('precarga el formulario con los datos guardados como texto', () => {
    const valores = valoresDeRegistro(camposFormulario, {
      [campoPorTipo('TEXTO').id]: 'Harina',
      [campoPorTipo('NUMERO').id]: 12.5,
      [campoPorTipo('FECHA').id]: null,
      [campoPorTipo('SI_NO').id]: true,
    });

    expect(valores).toEqual({
      [campoPorTipo('TEXTO').id]: 'Harina',
      [campoPorTipo('NUMERO').id]: '12.5',
      [campoPorTipo('FECHA').id]: '',
      [campoPorTipo('OPCION').id]: '',
      [campoPorTipo('SI_NO').id]: 'true',
    });
    expect(validarFormulario(camposFormulario, valores)).toEqual({});
  });

  it('manda null en lo que se vació para que el backend lo borre', () => {
    const cargados = valoresParaGuardar(camposFormulario, {
      ...valoresIniciales(camposFormulario),
      [campoPorTipo('FECHA').id]: '',
    });

    expect(cambiosParaEditar(camposFormulario, cargados)).toEqual({
      [campoPorTipo('TEXTO').id]: 'Producto',
      [campoPorTipo('NUMERO').id]: 0,
      [campoPorTipo('FECHA').id]: null,
      [campoPorTipo('OPCION').id]: 'Insumos',
      [campoPorTipo('SI_NO').id]: false,
    });
  });
});
