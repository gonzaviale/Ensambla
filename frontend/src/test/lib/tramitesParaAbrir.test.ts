import { describe, expect, it } from 'vitest';
import {
  ETIQUETA_DE_MODALIDAD,
  MAXIMO_CROQUIS_LA_MATANZA_M2,
  caminoCaba,
  guiaDeTramites,
  type Modalidad,
} from '@/lib/tramitesParaAbrir';
import type { Jurisdiccion, Rubro } from '@/lib/dondeAbrir';

const JURISDICCIONES: Jurisdiccion[] = ['caba', 'la-matanza'];
const RUBROS: Rubro[] = ['almacen', 'papeleria', 'kiosco'];
const MODALIDADES = Object.keys(ETIQUETA_DE_MODALIDAD) as Modalidad[];
const ids = (jurisdiccion: Jurisdiccion, rubro: Rubro, metros: number) =>
  guiaDeTramites(jurisdiccion, rubro, metros).aprobaciones.map((aprobacion) => aprobacion.id);

describe('trámites para abrir', () => {
  it('en la Ciudad, el tamaño del local decide qué habilitación corresponde', () => {
    expect(caminoCaba(30)).toBe('expres');
    expect(caminoCaba(200)).toBe('expres');
    expect(caminoCaba(201)).toBe('sin-planos');
    expect(caminoCaba(500)).toBe('sin-planos');
    expect(caminoCaba(501)).toBe('con-planos');
  });

  it('hasta 200 m² alcanza la habilitación exprés: online, sin costo y sin profesional', () => {
    const guia = guiaDeTramites('caba', 'almacen', 120);
    expect(ids('caba', 'almacen', 120)).toEqual(['habilitacion-expres', 'caa', 'consulta-de-usos']);
    expect(guia.modalidad).toBe('online');
    expect(guia.costoCorto).toBe('Sin costo');
    expect(guia.profesionalCorto).toBe('Sin profesional');
    expect(guia.resumenSami).toMatch(/exprés/);
    expect(guia.documentos.join(' ')).toMatch(/miBA nivel 3/);
  });

  it('con más de 200 m² hace falta un profesional que firme el Anexo Técnico', () => {
    const sinPlanos = guiaDeTramites('caba', 'kiosco', 300);
    expect(ids('caba', 'kiosco', 300)).toContain('anexo-tecnico');
    expect(sinPlanos.profesionalCorto).toBe('Con profesional');
    expect(sinPlanos.resumenSami).toMatch(/sin planos/);
    expect(sinPlanos.pasos.length).toBeGreaterThan(4);

    const conPlanos = guiaDeTramites('caba', 'kiosco', 600);
    expect(conPlanos.resumenSami).toMatch(/con planos y es obligatoria/);
    expect(conPlanos.aprobaciones[0]?.nombre).toMatch(/con planos/);
  });

  it('en La Matanza es mixto: formularios online y varios pasos en oficinas de San Justo', () => {
    const guia = guiaDeTramites('la-matanza', 'papeleria', 60);
    expect(guia.modalidad).toBe('mixta');
    const porId = (id: string) => guia.aprobaciones.find((aprobacion) => aprobacion.id === id);
    expect(porId('zonificacion')?.modalidad).toBe('oficina');
    expect(porId('libre-deuda')?.modalidad).toBe('oficina');
    expect(porId('contrato')?.modalidad).toBe('oficina');
    expect(porId('habilitacion')?.modalidad).toBe('mixta');
    expect(guia.oficinas.map((oficina) => oficina.donde).join(' ')).toMatch(/Perú 2250/);
    expect(guia.tituloDePasos).toMatch(/no publica el paso a paso/);
  });

  it('en La Matanza los rubros alimenticios y el maxikiosco suman requisitos', () => {
    expect(ids('la-matanza', 'almacen', 60)).toContain('alimentos');
    expect(ids('la-matanza', 'kiosco', 60)).toContain('alimentos');
    expect(ids('la-matanza', 'papeleria', 60)).not.toContain('alimentos');
    expect(ids('la-matanza', 'kiosco', 60)).toContain('maxikiosco');
    expect(ids('la-matanza', 'almacen', 60)).not.toContain('maxikiosco');
  });

  it('en La Matanza, hasta 70 m² alcanza el croquis y más pide plano de obra', () => {
    const chico = guiaDeTramites('la-matanza', 'almacen', MAXIMO_CROQUIS_LA_MATANZA_M2);
    const grande = guiaDeTramites('la-matanza', 'almacen', MAXIMO_CROQUIS_LA_MATANZA_M2 + 1);
    expect(chico.documentos.join(' ')).toMatch(/formulario F15/);
    expect(chico.resumenSami).toMatch(/croquis/);
    expect(grande.documentos.join(' ')).toMatch(/Plano de obra/);
    expect(grande.resumenSami).toMatch(/plano de obra/);
  });

  it('toda guía dice cómo se hace cada cosa, de dónde sale y qué no se sabe', () => {
    for (const jurisdiccion of JURISDICCIONES) {
      for (const rubro of RUBROS) {
        for (const metros of [40, 150, 400, 700]) {
          const guia = guiaDeTramites(jurisdiccion, rubro, metros);
          expect(guia.jurisdiccion).toBe(jurisdiccion);
          expect(guia.aprobaciones.length).toBeGreaterThan(0);
          for (const aprobacion of guia.aprobaciones)
            expect(MODALIDADES).toContain(aprobacion.modalidad);
          expect(guia.documentos.length).toBeGreaterThan(0);
          expect(guia.pasos.length).toBeGreaterThan(0);
          expect(guia.oficinas.length).toBeGreaterThan(0);
          expect(guia.avisos.length).toBeGreaterThan(0);
          expect(guia.plazo).toMatch(/no publica/);
          for (const fuente of guia.fuentes)
            expect(fuente.url).toMatch(/^https:\/\/(www\.)?(buenosaires\.gob|lamatanza\.gov)\.ar/);
        }
      }
    }
  });

  it('lo que las páginas oficiales no dicen queda marcado como a confirmar', () => {
    const laMatanza = guiaDeTramites('la-matanza', 'almacen', 60);
    const fiscal = laMatanza.aprobaciones.find(
      (aprobacion) => aprobacion.id === 'inscripcion-fiscal',
    );
    expect(fiscal?.modalidad).toBe('confirmar');
    expect(laMatanza.avisos.join(' ')).toMatch(/puede estar desactualizado/);
    expect(laMatanza.costoCorto).toBe('Costos no publicados');
  });
});
