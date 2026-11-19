import { describe, expect, it } from 'vitest';
import {
  ETIQUETA_DE_MODALIDAD,
  MAXIMO_CROQUIS_LA_MATANZA_M2,
  caminoCaba,
  guiaDeTramites,
  type Modalidad,
} from '@/lib/tramitesParaAbrir';
import type { Jurisdiccion, Rubro } from '@/lib/dondeAbrir';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

type Guia = ReturnType<typeof guiaDeTramites>;

const JURISDICCIONES: Jurisdiccion[] = ['caba', 'la-matanza'];
const RUBROS: Rubro[] = ['almacen', 'papeleria', 'kiosco'];
const METROS_A_PROBAR = [40, 150, 400, 700];
const MODALIDADES = Object.keys(ETIQUETA_DE_MODALIDAD) as Modalidad[];

let guia: Guia;

describe('trámites para abrir', () => {
  it.each([
    [30, 'expres'],
    [200, 'expres'],
    [201, 'sin-planos'],
    [500, 'sin-planos'],
    [501, 'con-planos'],
  ])('en la Ciudad, un local de %d m² sigue el camino %s', (metros, camino) => {
    cuandoSeCalcula(() => caminoCaba(metros));

    entoncesElResultadoEs(camino);
  });

  it('hasta 200 m² alcanza la habilitación exprés', () => {
    cuandoSePideLaGuia('caba', 'almacen', 120);

    entoncesLasAprobacionesSon('habilitacion-expres', 'caa', 'consulta-de-usos');
  });

  it('la habilitación exprés es online, sin costo y sin profesional', () => {
    cuandoSePideLaGuia('caba', 'almacen', 120);

    entoncesLaGuiaEsOnlineSinCostoNiProfesional();
  });

  it('la habilitación exprés se explica a Sami y pide miBA nivel 3', () => {
    cuandoSePideLaGuia('caba', 'almacen', 120);

    entoncesLaGuiaMencionaExpresYMiBA();
  });

  it('con más de 200 m² hace falta un profesional que firme el Anexo Técnico', () => {
    cuandoSePideLaGuia('caba', 'kiosco', 300);

    entoncesLaGuiaPideProfesionalYAnexoTecnico();
  });

  it('con más de 500 m² la habilitación es con planos y obligatoria', () => {
    cuandoSePideLaGuia('caba', 'kiosco', 600);

    entoncesLaGuiaEsConPlanosYObligatoria();
  });

  it('en La Matanza es mixto: formularios online y varios pasos en oficinas', () => {
    cuandoSePideLaGuia('la-matanza', 'papeleria', 60);

    entoncesLaMatanzaEsMixtaConPasosEnOficinas();
  });

  it('en La Matanza no se publica el paso a paso y las oficinas están en San Justo', () => {
    cuandoSePideLaGuia('la-matanza', 'papeleria', 60);

    entoncesLasOficinasEstanEnSanJustoYNoSePublicaElPasoAPaso();
  });

  it.each([
    ['almacen', 'alimentos', true],
    ['kiosco', 'alimentos', true],
    ['papeleria', 'alimentos', false],
    ['kiosco', 'maxikiosco', true],
    ['almacen', 'maxikiosco', false],
  ] as [Rubro, string, boolean][])(
    'en La Matanza, el rubro %s con el requisito %s: %s',
    (rubro, requisito, corresponde) => {
      cuandoSePideLaGuia('la-matanza', rubro, 60);

      entoncesElRequisitoCorresponde(requisito, corresponde);
    },
  );

  it('en La Matanza, hasta 70 m² alcanza el croquis', () => {
    cuandoSePideLaGuia('la-matanza', 'almacen', MAXIMO_CROQUIS_LA_MATANZA_M2);

    entoncesLaGuiaPideCroquis();
  });

  it('en La Matanza, más de 70 m² pide plano de obra', () => {
    cuandoSePideLaGuia('la-matanza', 'almacen', MAXIMO_CROQUIS_LA_MATANZA_M2 + 1);

    entoncesLaGuiaPidePlanoDeObra();
  });

  it.each(JURISDICCIONES.flatMap((j) => RUBROS.map((r) => [j, r] as [Jurisdiccion, Rubro])))(
    'toda guía de %s para %s dice cómo se hace cada cosa, de dónde sale y qué no se sabe',
    (jurisdiccion, rubro) => {
      cuandoSePidenGuiasConDistintosTamanios(jurisdiccion, rubro);

      entoncesTodasLasGuiasEstanCompletas(jurisdiccion);
    },
  );

  it('lo que las páginas oficiales no dicen queda marcado como a confirmar', () => {
    cuandoSePideLaGuia('la-matanza', 'almacen', 60);

    entoncesLaInscripcionFiscalQuedaAConfirmar();
  });
});

function cuandoSePideLaGuia(jurisdiccion: Jurisdiccion, rubro: Rubro, metros: number) {
  guia = guiaDeTramites(jurisdiccion, rubro, metros);
}

let guias: Guia[] = [];

function cuandoSePidenGuiasConDistintosTamanios(jurisdiccion: Jurisdiccion, rubro: Rubro) {
  guias = METROS_A_PROBAR.map((metros) => guiaDeTramites(jurisdiccion, rubro, metros));
}

const textoDeLosDocumentos = () => guia.documentos.join(' ');
const aprobacionDe = (id: string) => guia.aprobaciones.find((aprobacion) => aprobacion.id === id);

function entoncesLasAprobacionesSon(...ids: string[]) {
  expect(guia.aprobaciones.map((aprobacion) => aprobacion.id)).toEqual(ids);
}

function entoncesLaGuiaEsOnlineSinCostoNiProfesional() {
  expect(guia.modalidad).toBe('online');
  expect(guia.costoCorto).toBe('Sin costo');
  expect(guia.profesionalCorto).toBe('Sin profesional');
}

function entoncesLaGuiaMencionaExpresYMiBA() {
  expect(guia.resumenSami).toMatch(/exprés/);
  expect(textoDeLosDocumentos()).toMatch(/miBA nivel 3/);
}

function entoncesLaGuiaPideProfesionalYAnexoTecnico() {
  expect(guia.aprobaciones.map((aprobacion) => aprobacion.id)).toContain('anexo-tecnico');
  expect(guia.profesionalCorto).toBe('Con profesional');
  expect(guia.resumenSami).toMatch(/sin planos/);
  expect(guia.pasos.length).toBeGreaterThan(4);
}

function entoncesLaGuiaEsConPlanosYObligatoria() {
  expect(guia.resumenSami).toMatch(/con planos y es obligatoria/);
  expect(guia.aprobaciones[0]?.nombre).toMatch(/con planos/);
}

function entoncesLaMatanzaEsMixtaConPasosEnOficinas() {
  expect(guia.modalidad).toBe('mixta');
  expect(aprobacionDe('zonificacion')?.modalidad).toBe('oficina');
  expect(aprobacionDe('libre-deuda')?.modalidad).toBe('oficina');
  expect(aprobacionDe('contrato')?.modalidad).toBe('oficina');
  expect(aprobacionDe('habilitacion')?.modalidad).toBe('mixta');
}

function entoncesLasOficinasEstanEnSanJustoYNoSePublicaElPasoAPaso() {
  expect(guia.oficinas.map((oficina) => oficina.donde).join(' ')).toMatch(/Perú 2250/);
  expect(guia.tituloDePasos).toMatch(/no publica el paso a paso/);
}

function entoncesElRequisitoCorresponde(requisito: string, corresponde: boolean) {
  const ids = guia.aprobaciones.map((aprobacion) => aprobacion.id);
  expect(ids.includes(requisito)).toBe(corresponde);
}

function entoncesLaGuiaPideCroquis() {
  expect(textoDeLosDocumentos()).toMatch(/formulario F15/);
  expect(guia.resumenSami).toMatch(/croquis/);
}

function entoncesLaGuiaPidePlanoDeObra() {
  expect(textoDeLosDocumentos()).toMatch(/Plano de obra/);
  expect(guia.resumenSami).toMatch(/plano de obra/);
}

function entoncesTodasLasGuiasEstanCompletas(jurisdiccion: Jurisdiccion) {
  for (const completa of guias) {
    expect(completa.jurisdiccion).toBe(jurisdiccion);
    expect(completa.aprobaciones.length).toBeGreaterThan(0);
    for (const aprobacion of completa.aprobaciones)
      expect(MODALIDADES).toContain(aprobacion.modalidad);
    expect(completa.documentos.length).toBeGreaterThan(0);
    expect(completa.pasos.length).toBeGreaterThan(0);
    expect(completa.oficinas.length).toBeGreaterThan(0);
    expect(completa.avisos.length).toBeGreaterThan(0);
    expect(completa.plazo).toMatch(/no publica/);
    for (const fuente of completa.fuentes)
      expect(fuente.url).toMatch(/^https:\/\/(www\.)?(buenosaires\.gob|lamatanza\.gov)\.ar/);
  }
}

function entoncesLaInscripcionFiscalQuedaAConfirmar() {
  expect(aprobacionDe('inscripcion-fiscal')?.modalidad).toBe('confirmar');
  expect(guia.avisos.join(' ')).toMatch(/puede estar desactualizado/);
  expect(guia.costoCorto).toBe('Costos no publicados');
}
