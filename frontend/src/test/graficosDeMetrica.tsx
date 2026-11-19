import { render, screen, within } from '@testing-library/react';
import { expect } from 'vitest';
import { GraficoDeLaMetrica } from '@/components/sistema/metricas/GraficoDeLaMetrica';
import { SelectorDeGrafico } from '@/components/sistema/metricas/SelectorDeGrafico';
import { partesParaMostrar, type Parte } from '@/lib/metricas/graficos';
import type {
  CalculoDeMetrica,
  DefinicionDeMetrica,
  GraficoDeMetrica,
  ResultadoDeMetrica,
} from '@/types/metricas';

// Métricas ya calculadas, como las devuelve el servidor, para mirar sus gráficos sin red.

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];
const PRODUCTOS = ['Yerba', 'Coca', 'Pan', 'Sprite', 'Harina', 'Arroz', 'Fideos'];

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

export const cuandoSeMuestranLasPartesDe = (metrica: Metrica, todas = false) => {
  partes = partesParaMostrar(metrica.definicion, metrica.resultado, todas);
};

export const cuandoSeMuestranLasPartesConElCalculo = (calculo: CalculoDeMetrica) =>
  cuandoSeMuestranLasPartesDe({
    ...SIETE_PRODUCTOS,
    definicion: receta({ calculo }),
  });

export const entoncesSeVenLasPartes = (etiquetas: string[]) =>
  expect(partes.map((parte) => parte.etiqueta)).toEqual(etiquetas);

export const entoncesOtrosSuma = (valor: number) =>
  expect(partes.find((parte) => parte.otros)?.valor).toBe(valor);

export const dadoElGrafico = (metrica: Metrica, grafico: GraficoDeMetrica) =>
  render(<GraficoDeLaMetrica {...metrica} grafico={grafico} />);

export const entoncesSeVeElNumero = (texto: string) =>
  expect(screen.getByText(texto)).toBeInTheDocument();

export const entoncesSeVeElDibujo = (nombre: string) =>
  expect(screen.getByLabelText(nombre)).toBeInTheDocument();

export const entoncesNoHayDibujo = (nombre: string) =>
  expect(screen.queryByLabelText(nombre)).not.toBeInTheDocument();

export const entoncesDiceElMotivo = (motivo: string) =>
  expect(screen.getByRole('status')).toHaveTextContent(motivo);

export const entoncesOfreceVerTodas = (texto: string) =>
  expect(screen.getByRole('button', { name: texto })).toBeInTheDocument();

export const entoncesNoOfreceVerTodas = () =>
  expect(screen.queryByRole('button', { name: /Ver las/ })).not.toBeInTheDocument();

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
