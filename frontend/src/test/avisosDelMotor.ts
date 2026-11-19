import { expect } from 'vitest';
import {
  analizar,
  parecido,
  responder,
  type AccionDeAviso,
  type Aviso,
  type GrupoDeAviso,
  type Respuesta,
  type TipoDeAviso,
} from '@/lib/sistema/alertas';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import type { Fila, Sistema, TipoBloque } from '@/lib/sistema/tipos';
import { MINIMO_DE_COMERCIOS, sinDatosInsuficientes, type Zona } from '@/lib/sistema/zona';
import { zonaDeLaPersona, zonaParaTexto } from '@/mocks/db/zona';

export const HOY = '2026-10-16';
export const ZONA = zonaDeLaPersona();

type Venta = [dias: number, cantidad: number];

let sistema: Sistema;
let zona: Zona = ZONA;
let avisos: Aviso[] = [];
let otraVez: Aviso[] = [];
let respuesta: Respuesta;
let zonas: Zona[] = [];
let similitud: number;

const deTipo = (tipo: TipoDeAviso) => avisos.filter((aviso) => aviso.tipo === tipo);
const primeroDeTipo = (tipo: TipoDeAviso) => deTipo(tipo)[0];
const analizarCon = (grupos: Record<string, boolean> = {}) =>
  analizar({ sistemas: [sistema], zona, hoy: HOY, grupos });

const fechaDeHaceDias = (dias: number) => {
  const fecha = new Date(`${HOY}T12:00:00`);
  fecha.setDate(fecha.getDate() - dias);
  return fecha.toISOString().slice(0, 10);
};

const ventaDeLaYerba = ([dias, cantidad]: Venta, indice: number): Fila => ({
  id: `v${indice}`,
  product: 'p1',
  quantity: cantidad,
  price: 3900,
  date: fechaDeHaceDias(dias),
  customer: '',
  credit: false,
});

const bloquesDe = (tipos: TipoBloque[]) => {
  sistema = makeSystem('Prueba', tipos, false);
  zona = ZONA;
  return allBlocks(sistema);
};

export const dadoElAlmacenDeEjemplo = () => {
  sistema = crearAlmacenDeEjemplo(HOY);
  zona = ZONA;
};

/** Un sistema de un solo producto, para llevar cada regla a su umbral. */
export const dadaUnaYerbaCon = (valores: Record<string, string | number>, ventas: Venta[] = []) => {
  const [inventario, bloqueDeVentas] = bloquesDe(['inventory', 'sales']);
  if (!inventario || !bloqueDeVentas) throw new Error('Faltan bloques');
  inventario.rows = [
    {
      id: 'p1',
      name: 'Yerba Playadito 1 kg',
      quantity: 50,
      price: 3900,
      cost: 2700,
      minimum: 10,
      ...valores,
    },
  ];
  bloqueDeVentas.rows = ventas.map(ventaDeLaYerba);
};

export const dadaUnaYerbaEnUnaZonaChica = (valores: Record<string, string | number>) => {
  dadaUnaYerbaCon(valores);
  zona = { nombre: 'Chica', comercios: 2, productos: [] };
};

export const dadosFideosAgotadosConBloqueDeEntradas = () => {
  const [inventario] = bloquesDe(['inventory', 'entries']);
  if (inventario) inventario.rows = [{ id: 'p1', name: 'Fideos', quantity: 0, minimum: 5 }];
};

export const dadasCanastasDeAlfajorYGalletitas = (veces: number) => {
  const [inventario, ventas, clientes] = bloquesDe(['inventory', 'sales', 'clients']);
  if (!inventario || !ventas || !clientes) throw new Error('Faltan bloques');
  inventario.rows = [
    { id: 'a', name: 'Alfajor Jorgito', quantity: 30, price: 800, cost: 500 },
    { id: 'b', name: 'Galletitas Criollitas 3 U', quantity: 30, price: 1250, cost: 880 },
  ];
  clientes.rows = [{ id: 'c', name: 'Rosa' }];
  ventas.rows = Array.from({ length: veces }, (_, indice) => indice).flatMap(canastaDelDia);
};

const canastaDelDia = (indice: number): Fila[] =>
  ['a', 'b'].map((producto) => ({
    id: `v${indice}${producto}`,
    product: producto,
    quantity: 1,
    price: 800,
    date: `2026-10-${String(10 - indice).padStart(2, '0')}`,
    customer: 'c',
    credit: false,
  }));

export const dadoUnSistemaSinInventario = () => {
  bloquesDe(['note']);
};

export const cuandoSeAnalizan = () => {
  avisos = analizarCon();
};

export const cuandoSeAnalizanDosVeces = () => {
  avisos = analizarCon();
  otraVez = analizarCon();
};

export const cuandoSeAnalizanConElGrupoApagado = (grupo: GrupoDeAviso) => {
  avisos = analizarCon({ [grupo]: false });
};

export const cuandoSePregunta = (pregunta: string) => {
  dadoElAlmacenDeEjemplo();
  respuesta = responder(pregunta, { zona, avisos: analizarCon() });
};

export const cuandoSeAnalizanCienBarrios = () => {
  zonas = Array.from({ length: 100 }, (_, indice) => zonaParaTexto(`Barrio ${indice}`));
};

export const cuandoSeAnalizaDosVecesElBarrio = (barrio: string) => {
  zonas = [zonaParaTexto(barrio), zonaParaTexto(barrio)];
};

export const cuandoSeFiltraUnaZonaDeDosComercios = () => {
  zonas = [sinDatosInsuficientes({ nombre: 'Chica', comercios: 2, productos: ZONA.productos })];
};

export const cuandoSeComparan = (tuyo: string, otro: string) => {
  similitud = parecido(tuyo, otro);
};

export const entoncesSeDisparanLasReglas = (tipos: TipoDeAviso[]) => {
  expect(new Set(avisos.map((aviso) => aviso.tipo))).toEqual(new Set(tipos));
};

export const entoncesCadaAvisoExplicaPorQueYTraeAcciones = () => {
  for (const aviso of avisos) {
    expect(aviso.porque.length, aviso.titulo).toBeGreaterThan(0);
    expect(
      aviso.porque.every((motivo) => motivo.trim().length > 0),
      aviso.titulo,
    ).toBe(true);
    expect(aviso.acciones.length, aviso.titulo).toBeGreaterThan(0);
    expect(aviso.sistemaId).toBe(sistema.id);
  }
};

export const entoncesElPrimeroEsUrgente = (titulo: string) => {
  expect(avisos[0]?.titulo).toBe(titulo);
  expect(avisos[0]?.prioridad).toBe(3);
};

export const entoncesEstanOrdenadosPorUrgencia = () => {
  const prioridades = avisos.map((aviso) => aviso.prioridad);
  expect(prioridades).toEqual([...prioridades].sort((a, b) => b - a));
};

export const entoncesLosIdsNoSeRepiten = () => {
  expect(new Set(avisos.map((aviso) => aviso.id)).size).toBe(avisos.length);
};

export const entoncesLosIdsSonLosMismos = () => {
  expect(otraVez.map((aviso) => aviso.id)).toEqual(avisos.map((aviso) => aviso.id));
};

export const entoncesLosGruposSon = (grupos: GrupoDeAviso[]) => {
  expect(new Set(avisos.map((aviso) => aviso.grupo))).toEqual(new Set(grupos));
};

export const entoncesNoHayAvisosDelGrupo = (grupo: GrupoDeAviso) => {
  expect(avisos.some((aviso) => aviso.grupo === grupo)).toBe(false);
};

export const entoncesHayAvisosDeTipo = (tipo: TipoDeAviso, cantidad: number) => {
  expect(deTipo(tipo)).toHaveLength(cantidad);
};

export const entoncesNoHayAvisos = () => {
  expect(avisos).toEqual([]);
};

export const entoncesLosTitulosDeTipoSon = (tipo: TipoDeAviso, titulos: string[]) => {
  expect(deTipo(tipo).map((aviso) => aviso.titulo)).toEqual(titulos);
};

export const entoncesNingunTituloDeTipoEs = (tipo: TipoDeAviso, titulo: string) => {
  expect(deTipo(tipo).map((aviso) => aviso.titulo)).not.toContain(titulo);
};

export const entoncesNingunAvisoNombra = (texto: string) => {
  expect(JSON.stringify(avisos)).not.toContain(texto);
};

export const entoncesElAvisoTienePrioridad = (tipo: TipoDeAviso, prioridad: number) => {
  expect(primeroDeTipo(tipo)?.prioridad).toBe(prioridad);
};

export const entoncesElMotivoEs = (tipo: TipoDeAviso, indice: number, motivo: string) => {
  expect(primeroDeTipo(tipo)?.porque[indice]).toBe(motivo);
};

export const entoncesElMotivoDice = (tipo: TipoDeAviso, indice: number, parte: string) => {
  expect(primeroDeTipo(tipo)?.porque[indice]).toContain(parte);
};

export const entoncesLosMotivosDicen = (tipo: TipoDeAviso, parte: string) => {
  expect(primeroDeTipo(tipo)?.porque.join(' ')).toContain(parte);
};

export const entoncesLaPrimeraAccionEs = (tipo: TipoDeAviso, accion: AccionDeAviso['tipo']) => {
  expect(primeroDeTipo(tipo)?.acciones[0]?.tipo).toBe(accion);
};

export const entoncesElTituloEs = (tipo: TipoDeAviso, titulo: string) => {
  expect(primeroDeTipo(tipo)?.titulo).toBe(titulo);
};

export const entoncesElTituloDice = (tipo: TipoDeAviso, parte: string) => {
  expect(primeroDeTipo(tipo)?.titulo).toContain(parte);
};

export const entoncesElDetalleDice = (tipo: TipoDeAviso, parte: string) => {
  expect(primeroDeTipo(tipo)?.detalle).toContain(parte);
};

export const entoncesProponeElPrecio = (tipo: TipoDeAviso, precio: number) => {
  const accion = primeroDeTipo(tipo)?.acciones[0];
  expect(accion?.tipo === 'aplicar' && accion.valores.price).toBe(precio);
};

export const entoncesProponeCrear = (tipo: TipoDeAviso, valores: Record<string, unknown>) => {
  const accion = primeroDeTipo(tipo)?.acciones[0];
  expect(accion?.tipo === 'crear' && accion.valores).toMatchObject(valores);
};

export const entoncesLaRespuestaDice = (parte: string) => {
  expect(respuesta.texto).toContain(parte);
};

export const entoncesLaRespuestaSoloTraeAvisosDeTipo = (tipo: TipoDeAviso) => {
  expect(respuesta.avisos.every((aviso) => aviso.tipo === tipo)).toBe(true);
};

export const entoncesElPrimerPrecioEsDe = (nombre: string) => {
  expect(respuesta.precios?.[0]?.nombre).toBe(nombre);
};

export const entoncesNoTraePrecios = () => {
  expect(respuesta.precios).toEqual([]);
};

export const entoncesElParecidoEs = (valor: number) => {
  expect(similitud).toBe(valor);
};

export const entoncesElMinimoDeComerciosEs = (minimo: number) => {
  expect(MINIMO_DE_COMERCIOS).toBe(minimo);
};

export const entoncesCadaProductoDeLaZonaTieneElMinimoDeComercios = () => {
  for (const producto of ZONA.productos) {
    expect(producto.comercios).toBeGreaterThanOrEqual(MINIMO_DE_COMERCIOS);
    expect(producto.ventas.comercios).toBeGreaterThanOrEqual(MINIMO_DE_COMERCIOS);
  }
};

export const entoncesNingunProductoDeLaZonaEs = (prefijo: string) => {
  expect(ZONA.productos.some((producto) => producto.nombre.startsWith(prefijo))).toBe(false);
};

export const entoncesLosBarriosConPocosComerciosNoTienenDatos = () => {
  const chico = zonas.find((otra) => otra.comercios < MINIMO_DE_COMERCIOS);
  expect(chico?.productos).toEqual([]);
};

export const entoncesLosBarriosConSuficientesComerciosTienenDatos = () => {
  const grande = zonas.find((otra) => otra.comercios >= MINIMO_DE_COMERCIOS);
  expect(grande?.productos.length).toBeGreaterThan(0);
};

export const entoncesNoQuedaNingunProducto = () => {
  expect(zonas[0]?.productos).toEqual([]);
};

export const entoncesDanLoMismo = () => {
  expect(zonas[0]).toEqual(zonas[1]);
};
