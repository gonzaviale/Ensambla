import { describe, it } from 'vitest';
import {
  cuandoSeConsultaDesde,
  dadoUnAvisoCreadoEl,
  entoncesDice,
} from '@/test/haceCuantoHelpers';

describe('haceCuanto · cuenta días de calendario argentino', () => {
  it('dice hoy si el aviso se creó hoy a la noche aunque en UTC ya sea otro día', () => {
    const creadoEn = dadoUnAvisoCreadoEl('2026-11-18T02:19:35Z');

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-18T02:36:00Z');

    entoncesDice(texto, 'hoy');
  });

  it('dice ayer si el aviso se creó ayer a la noche', () => {
    const creadoEn = dadoUnAvisoCreadoEl('2026-11-17T00:21:46Z');

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-18T02:36:00Z');

    entoncesDice(texto, 'ayer');
  });

  it('dice ayer pasada la medianoche aunque no hayan pasado 24 horas', () => {
    const creadoEn = dadoUnAvisoCreadoEl('2026-11-16T23:50:00-03:00');

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-17T00:10:00-03:00');

    entoncesDice(texto, 'ayer');
  });

  it('dice hace dos días si se creó anteayer aunque no hayan pasado 48 horas', () => {
    const creadoEn = dadoUnAvisoCreadoEl('2026-11-15T23:00:00-03:00');

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-17T11:00:00-03:00');

    entoncesDice(texto, 'hace 2 días');
  });

  it('no dice nada si el aviso no trae fecha', () => {
    const creadoEn = dadoUnAvisoCreadoEl(undefined);

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-17T11:00:00-03:00');

    entoncesDice(texto, undefined);
  });

  it('no dice nada si la fecha del aviso es inválida', () => {
    const creadoEn = dadoUnAvisoCreadoEl('no es una fecha');

    const texto = cuandoSeConsultaDesde(creadoEn, '2026-11-17T11:00:00-03:00');

    entoncesDice(texto, undefined);
  });
});
