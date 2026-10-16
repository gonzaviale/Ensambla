import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearRegistro } from '@/api/registros';
import type { FilaRelato, PiezaId, RolCampo } from '@/types/negocio';
import type { ValoresRegistro } from '@/types/registro';
import { proyectoQuery } from '../proyectos/queries';
import { useSumarPieza } from './useArmarSistema';
import { ubicarPiezas, type PiezaPresente } from './useMiNegocio';

// «Contá qué pasó»: guarda cada cosa entendida en la tabla de su pieza, mapeando por rol de columna.
// Si falta una pieza y la persona pidió sumarla, la crea primero. Nunca guarda sin que se confirme.

export type FilaConfirmada = {
  /** Identifica la fila en la pantalla (para saber cuáles se guardaron). */
  id: string;
  fila: FilaRelato;
  /** Monto ya corregido por la persona. */
  monto: number;
};

export type ResultadoCarga = {
  guardadas: string[];
  /** Falló el guardado: se pueden reintentar. */
  fallidas: string[];
  /** Su pieza no está en el sistema (y no se sumó). */
  sinPieza: string[];
};

function hoy(): string {
  const ahora = new Date();
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${ahora.getFullYear()}-${dos(ahora.getMonth() + 1)}-${dos(ahora.getDate())}`;
}

function valoresDe(presente: PiezaPresente, { fila, monto }: FilaConfirmada): ValoresRegistro {
  const valores: ValoresRegistro = {};
  const poner = (rol: RolCampo, valor: string | number | boolean) => {
    const campo = presente.campo(rol);
    if (campo) valores[campo.id] = valor;
  };
  poner('fecha', hoy());
  poner('monto', monto);
  if (fila.pieza === 'ventas') {
    if (fila.valores.producto) poner('producto', fila.valores.producto);
    poner('cantidad', fila.valores.cantidad ?? 1);
  } else if (fila.pieza === 'gastos') {
    if (fila.valores.concepto) poner('concepto', fila.valores.concepto);
  } else {
    if (fila.valores.cliente) poner('cliente', fila.valores.cliente);
    poner('pagado', false);
  }
  return valores;
}

export function useCargarRelato(sistemaId: string | undefined) {
  const queryClient = useQueryClient();
  const sumarPieza = useSumarPieza(sistemaId ?? '');

  return useMutation({
    mutationFn: async ({
      filas,
      presentes,
      sumar = [],
    }: {
      filas: FilaConfirmada[];
      presentes: Map<PiezaId, PiezaPresente>;
      /** Piezas que faltan y la persona aceptó sumar antes de guardar. */
      sumar?: PiezaId[];
    }): Promise<ResultadoCarga> => {
      let vigentes = presentes;
      if (sistemaId && sumar.length > 0) {
        for (const id of sumar) await sumarPieza.mutateAsync({ id });
        const fresco = await queryClient.fetchQuery({ ...proyectoQuery(sistemaId), staleTime: 0 });
        vigentes = ubicarPiezas(fresco);
      }

      const resultado: ResultadoCarga = { guardadas: [], fallidas: [], sinPieza: [] };
      const tocadas = new Set<string>();
      for (const item of filas) {
        const presente = vigentes.get(item.fila.pieza);
        if (!presente) {
          resultado.sinPieza.push(item.id);
          continue;
        }
        try {
          await crearRegistro(presente.bloque.id, valoresDe(presente, item));
          resultado.guardadas.push(item.id);
          tocadas.add(presente.bloque.id);
        } catch {
          resultado.fallidas.push(item.id);
        }
      }
      await Promise.all([
        ...[...tocadas].map((bloqueId) =>
          queryClient.invalidateQueries({ queryKey: ['registros', bloqueId] }),
        ),
        queryClient.invalidateQueries({ queryKey: ['reportes'] }),
      ]);
      return resultado;
    },
  });
}
