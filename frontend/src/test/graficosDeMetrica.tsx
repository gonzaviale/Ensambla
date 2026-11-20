import { render, screen, within } from '@testing-library/react';
import { expect } from 'vitest';
import { GraficoDeLaMetrica } from '@/components/sistema/metricas/GraficoDeLaMetrica';
import { NombreDeLaMetrica } from '@/components/sistema/metricas/NombreDeLaMetrica';
import { SelectorDeGrafico } from '@/components/sistema/metricas/SelectorDeGrafico';
import { partesParaMostrar, type Parte } from '@/lib/metricas/graficos';
import type { DefinicionDeMetrica, GraficoDeMetrica, ResultadoDeMetrica } from '@/types/metricas';

// Métricas ya calculadas, como las devuelve el servidor, para mirar sus gráficos sin red.

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];
const PRODUCTOS = ['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina', 'Arroz', 'Fideos'];
const DOCE_PRODUCTOS = Array.from({ length: 12 }, (_, lugar) => `Producto ${lugar + 1}`);

const receta = (cambios: Partial<DefinicionDeMetrica>): DefinicionDeMetrica => ({
  nombre: 'Lo más vendido',
  fuente: 'VENTAS',
  calculo: 'UNIDADES',
  agrupar: 'PRODUCTO',
  periodo: 'MES',
  soloFiado: false,
  ...cambios,
});

const resultadoCon = (etiquetas: string[], valor: number): ResultadoDeMetrica => ({
  formato: 'NUMERO',
  valor,
  filas: etiquetas.map((etiqueta, lugar) => ({ etiqueta, valor: 70 - lugar * 10 })),
  motivo: null,
  deDondeSale: { fuente: 'VENTAS', filasUsadas: 10, filasSinDato: 0 },
});

export const SIETE_PRODUCTOS = { definicion: receta({}), resultado: resultadoCon(PRODUCTOS, 300) };
export const DOCE_EN_EL_REPARTO = {
  definicion: receta({}),
  resultado: resultadoCon(DOCE_PRODUCTOS, 600),
};
export const LA_SEMANA = {
  definicion: receta({ nombre: 'Ventas por día', calculo: 'SUMA', agrupar: 'DIA_DE_LA_SEMANA' }),
  resultado: resultadoCon(DIAS, 280),
};
export const SIN_NUMERO = {
  definicion: receta({}),
  resultado: { ...resultadoCon([], 0), valor: null, motivo: 'Todavía no cargaste ventas.' },
};

type Metrica = { definicion: DefinicionDeMetrica; resultado: ResultadoDeMetrica };

let partes: Parte[];

export const cuandoSeMuestranLasPartesDe = (metrica: Metrica) => {
  partes = partesParaMostrar(metrica.definicion, metrica.resultado);
};

export const entoncesSeVenLasPartes = (etiquetas: string[]) =>
  expect(partes.map((parte) => parte.etiqueta)).toEqual(etiquetas);

export const dadoElGrafico = (metrica: Metrica, grafico: GraficoDeMetrica) =>
  render(<GraficoDeLaMetrica {...metrica} grafico={grafico} />);

export const entoncesSeVeElNumero = (texto: string) =>
  expect(screen.getByText(texto)).toBeInTheDocument();

export const entoncesSeLee = (texto: string | RegExp) =>
  expect(screen.getByText(texto)).toBeInTheDocument();

export const entoncesNoSeLee = (texto: string | RegExp) =>
  expect(screen.queryByText(texto)).not.toBeInTheDocument();

export const entoncesSeVeElDibujo = (nombre: string) =>
  expect(screen.getByLabelText(nombre)).toBeInTheDocument();

export const entoncesNoHayDibujo = (nombre: string) =>
  expect(screen.queryByLabelText(nombre)).not.toBeInTheDocument();

export const entoncesDiceElMotivo = (motivo: string) =>
  expect(screen.getByRole('note')).toHaveTextContent(motivo);

export const entoncesNoMuestraNingunNumero = () =>
  expect(screen.queryByText(/\$|\d/)).not.toBeInTheDocument();

export const dadoElNombreDe = (resultado: ResultadoDeMetrica) =>
  render(<NombreDeLaMetrica nombre="Ventas del mes" resultado={resultado} />);

export const entoncesNoHayAviso = () => expect(screen.queryByRole('note')).not.toBeInTheDocument();

export const dadoElSelector = (grafico: GraficoDeMetrica, posibles: GraficoDeMetrica[]) =>
  render(<SelectorDeGrafico grafico={grafico} posibles={posibles} onElegir={() => undefined} />);

const opciones = () =>
  within(screen.getByRole('radiogroup', { name: 'Cómo verla' })).getAllByRole('radio');

export const entoncesOfreceLosGraficos = (nombres: string[]) =>
  expect(opciones().map((opcion) => opcion.textContent)).toEqual(nombres);

export const entoncesEstaElegido = (nombre: string) =>
  expect(screen.getByRole('radio', { name: nombre })).toBeChecked();

export const entoncesNoHaySelector = () =>
  expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
