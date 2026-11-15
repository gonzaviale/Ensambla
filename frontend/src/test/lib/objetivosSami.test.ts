import { describe, expect, it } from 'vitest';
import {
  datosUsados,
  editablesDe,
  paraGuardar,
  problemaDelManual,
  textoDeCuota,
  tipoQueQueda,
} from '@/lib/objetivosSami';
import { propuestaObjetivoSchema } from '@/types/objetivos';

// El ejemplo del contrato de docs/objetivos-con-sami.md (sección 7).
const PROPUESTA = propuestaObjetivoSchema.parse({
  respuesta: 'Entendí que querés vender más y darte a conocer.',
  entendido: 'Vender más y que más vecinos conozcan tu local',
  tipo: 'HIBRIDO',
  esMonetario: false,
  minis: [
    {
      modo: 'MEDIDO',
      tipo: 'VENTAS_SEMANALES',
      registroId: null,
      referencia: null,
      titulo: 'Vender un 10 % más por semana',
      justificacion: 'En los últimos 7 días vendiste $182.000.',
      valorInicial: 182000,
      meta: 200200,
      aporteEstimado: 31200,
      textoConIa: false,
    },
    {
      modo: 'MANUAL',
      tipo: null,
      registroId: null,
      referencia: null,
      titulo: 'Pedile a 3 clientes que te recomienden con un vecino',
      justificacion: 'Es la forma más barata de que te conozcan.',
      valorInicial: null,
      meta: null,
      aporteEstimado: null,
      textoConIa: true,
    },
  ],
  datos: [
    { fuente: 'VENTAS', hay: true },
    { fuente: 'MARGEN', hay: true },
    { fuente: 'GASTOS', hay: false },
    { fuente: 'ZONA', hay: false },
  ],
  origen: 'IA',
  mensajesRestantes: 5,
  puedeGuardar: true,
});

/** El mini medido y el manual de la propuesta de ejemplo. */
function lasDos() {
  const [medido, manual] = editablesDe(PROPUESTA);
  if (!medido || !manual) throw new Error('La propuesta de ejemplo trae un medido y un manual');
  return { medido, manual };
}

describe('objetivos con Sami', () => {
  it('la propuesta del contrato se lee, y la cuota todavía puede no venir', () => {
    expect(PROPUESTA.minis).toHaveLength(2);
    expect(PROPUESTA.cuota).toBeUndefined();
    expect(
      propuestaObjetivoSchema.parse({
        ...PROPUESTA,
        cuota: { usadas: 2, limite: 5, restantes: 3 },
      }).cuota,
    ).toEqual({ usadas: 2, limite: 5, restantes: 3 });
  });

  it('dice la cuota que queda, y cuando se agotó', () => {
    expect(textoDeCuota({ usadas: 2, limite: 5, restantes: 3 })).toBe(
      'Te quedan 3 de 5 objetivos con IA este mes',
    );
    expect(textoDeCuota({ usadas: 20, limite: 20, restantes: 0 })).toBe(
      'Usaste los 20 objetivos con IA de este mes',
    );
  });

  it('cuenta en qué datos se apoyó Sami y de cuáles no hay', () => {
    expect(datosUsados(PROPUESTA.datos)).toBe(
      'Se apoyó en: ventas, margen. Sin datos de: gastos, zona.',
    );
    expect(datosUsados([])).toBe('');
  });

  it('el tipo que queda sale de los mini-objetivos que se conservan', () => {
    const { medido, manual } = lasDos();
    expect(tipoQueQueda([])).toBeNull();
    expect(tipoQueQueda([medido])).toBe('CON_DATOS');
    expect(tipoQueQueda([manual])).toBe('SIN_DATOS');
    expect(tipoQueQueda([medido, manual])).toBe('HIBRIDO');
  });

  it('de un medido sólo se manda cuál es; del manual, su texto ya retocado', () => {
    const { medido, manual } = lasDos();

    expect(paraGuardar(medido)).toEqual({
      modo: 'MEDIDO',
      tipo: 'VENTAS_SEMANALES',
      registroId: null,
      referencia: null,
    });
    expect(paraGuardar({ ...manual, titulo: '  Hablá con 5 vecinos ' })).toEqual({
      modo: 'MANUAL',
      titulo: 'Hablá con 5 vecinos',
      justificacion: 'Es la forma más barata de que te conozcan.',
    });
  });

  it('una acción manual no puede estar vacía ni traer importes o porcentajes', () => {
    const { medido, manual } = lasDos();

    expect(problemaDelManual(manual)).toBeNull();
    expect(problemaDelManual({ ...manual, titulo: ' ' })).toMatch(/Escribí qué vas a hacer/);
    expect(problemaDelManual({ ...manual, justificacion: 'Subir $5.000' })).toMatch(/Sin importes/);
    expect(problemaDelManual({ ...manual, titulo: 'Vender 20 %' })).toMatch(/Sin importes/);
    // Los números de un medido los calcula el servidor: no se retocan ni se validan acá.
    expect(problemaDelManual({ ...medido, titulo: 'Vender un 10 %' })).toBeNull();
  });
});
