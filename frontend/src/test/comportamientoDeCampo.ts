import { expect } from 'vitest';
import { comportamientoDe, type ComportamientoDeCampo } from '@/lib/sistema/comportamiento';
import { makeSystem } from '@/lib/sistema/core';
import type { Campo, TipoCampo, Valor } from '@/lib/sistema/tipos';

export type CasoDeTipo = {
  tipo: TipoCampo;
  valorValido: Valor;
  valorInvalido: Valor;
  mensajeDeError: string;
  coercion: [Valor, Valor];
  textoPlano: [Valor, string];
  interpretacion: [unknown, Valor];
  noInterpretable: unknown;
};

export const TIPOS_CON_VALIDACION: CasoDeTipo[] = [
  {
    tipo: 'number',
    valorValido: 12,
    valorInvalido: 'abc',
    mensajeDeError: 'Escribí un número válido.',
    coercion: ['12', 12],
    textoPlano: [12, '12'],
    interpretacion: ['3,5', 3.5],
    noInterpretable: 'abc',
  },
  {
    tipo: 'currency',
    valorValido: '1500',
    valorInvalido: 'mucho',
    mensajeDeError: 'Escribí un número válido.',
    coercion: ['1500', 1500],
    textoPlano: [1500, '1500'],
    interpretacion: [900, 900],
    noInterpretable: 'mucho',
  },
  {
    tipo: 'date',
    valorValido: '2026-10-16',
    valorInvalido: '16/10/2026',
    mensajeDeError: 'Elegí una fecha válida.',
    coercion: ['2026-10-16', '2026-10-16'],
    textoPlano: ['2026-10-16', '2026-10-16'],
    interpretacion: ['16/10/2026', '2026-10-16'],
    noInterpretable: '31/02/2026',
  },
  {
    tipo: 'time',
    valorValido: '09:30',
    valorInvalido: '25:00',
    mensajeDeError: 'Elegí un horario válido.',
    coercion: ['09:30', '09:30'],
    textoPlano: ['09:30', '09:30'],
    interpretacion: ['9:30', '09:30'],
    noInterpretable: '25:00',
  },
  {
    tipo: 'email',
    valorValido: 'ana@correo.com',
    valorInvalido: 'ana@correo',
    mensajeDeError: 'Revisá el correo.',
    coercion: ['ana@correo.com', 'ana@correo.com'],
    textoPlano: ['ana@correo.com', 'ana@correo.com'],
    interpretacion: ['ana@correo.com', 'ana@correo.com'],
    noInterpretable: '',
  },
  {
    tipo: 'select',
    valorValido: 'Chica',
    valorInvalido: 'Mediana',
    mensajeDeError: 'Elegí una de las opciones.',
    coercion: ['Chica', 'Chica'],
    textoPlano: ['Chica', 'Chica'],
    interpretacion: ['grande', 'Grande'],
    noInterpretable: 'Mediana',
  },
];

const sistema = makeSystem('Sistema de prueba', [], false);

let campo: Campo;
let comportamiento: ComportamientoDeCampo;
let caso: CasoDeTipo;
let resultado: unknown;

const campoDe = (tipo: TipoCampo, requerido: boolean, minimo?: number): Campo => ({
  key: 'campo',
  label: 'Campo',
  type: tipo,
  required: requerido,
  options: ['Chica', 'Grande'],
  min: minimo,
});

export const dadoUnCampoDe = (tipo: TipoCampo, requerido = false, minimo?: number) => {
  campo = campoDe(tipo, requerido, minimo);
  comportamiento = comportamientoDe(tipo);
};

export const dadoUnCampoConLosCasosDe = (casoDelTipo: CasoDeTipo) => {
  caso = casoDelTipo;
  dadoUnCampoDe(casoDelTipo.tipo);
};

export const cuandoSeValidaUnValorValido = () => {
  resultado = comportamiento.validar(campo, caso.valorValido, sistema);
};

export const cuandoSeValidaUnValorInvalido = () => {
  resultado = comportamiento.validar(campo, caso.valorInvalido, sistema);
};

export const cuandoSeValida = (valor: Valor) => {
  resultado = comportamiento.validar(campo, valor, sistema);
};

export const cuandoSeGuardaUnValorCargadoAMano = () => {
  resultado = comportamiento.coercionar(caso.coercion[0]);
};

export const cuandoSeGuardaUnValorAusente = () => {
  resultado = comportamiento.coercionar(undefined);
};

export const cuandoSeMuestraComoTexto = () => {
  resultado = comportamiento.textoPlano(campo, caso.textoPlano[0], sistema);
};

export const cuandoSeMuestra = (valor: Valor) => {
  resultado = comportamiento.textoPlano(campo, valor, sistema);
};

export const cuandoSeInterpretaLoAnotadoAMano = () => {
  resultado = comportamiento.interpretar(campo, caso.interpretacion[0]);
};

export const cuandoSeInterpretaAlgoQueNoSirve = () => {
  resultado = comportamiento.interpretar(campo, caso.noInterpretable);
};

export const cuandoSeInterpretaTextoMuyLargo = () => {
  resultado = String(comportamiento.interpretar(campo, 'a'.repeat(300)));
};

export const cuandoSeInterpreta = (crudo: unknown) => {
  resultado = comportamiento.interpretar(campo, crudo);
};

export const cuandoSeProponeUnEjemploParaElRegistro = (indice: number, vinculos: string[] = []) => {
  resultado = comportamiento.valorDeEjemplo(campo, indice, undefined, vinculos);
};

export const cuandoSePreguntaSiFalta = (valor: Valor) => {
  resultado = comportamiento.falta(valor);
};

export const entoncesNoHayError = () => expect(resultado).toBeUndefined();

export const entoncesElErrorEsElDelTipo = () => expect(resultado).toBe(caso.mensajeDeError);

export const entoncesElErrorEs = (mensaje: string) => expect(resultado).toBe(mensaje);

export const entoncesSeGuardaConSuTipo = () => expect(resultado).toBe(caso.coercion[1]);

export const entoncesElTextoEsElEsperado = () => expect(resultado).toBe(caso.textoPlano[1]);

export const entoncesElTextoEs = (texto: string) => expect(resultado).toBe(texto);

export const entoncesSeObtieneElValorInterpretado = () =>
  expect(resultado).toBe(caso.interpretacion[1]);

export const entoncesSeObtiene = (valor: unknown) => expect(resultado).toBe(valor);

export const entoncesNoSeObtieneNada = () => expect(resultado).toBeUndefined();

export const entoncesElTextoSeRecortaA = (largo: number) =>
  expect(String(resultado)).toHaveLength(largo);
