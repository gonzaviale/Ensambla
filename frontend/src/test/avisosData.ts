import type { AvisoDelServidor } from '@/types/avisos';

// Un aviso de stock agotado como lo devuelve el motor del servidor.
export const avisoDelServidor = (datos: Partial<AvisoDelServidor> = {}): AvisoDelServidor => ({
  id: 'e0000000-0000-4000-8000-000000000001',
  proyectoId: 'c0000000-0000-4000-8000-000000000001',
  tipo: 'STOCK',
  grupo: 'STOCK',
  prioridad: 3,
  estado: 'PENDIENTE',
  entidad: 'registro:1',
  titulo: 'Te quedaste sin Yerba Playadito 1 kg',
  detalle: 'No te quedan unidades y tu mínimo es 10. Para no quedarte corto, pedí 20.',
  motivos: ['Definiste un mínimo de 10 y quedan 0.'],
  acciones: [
    {
      tipo: 'REGISTRAR',
      texto: 'Registrar entrada',
      proyectoId: 'c0000000-0000-4000-8000-000000000001',
      bloqueId: 'b0000000-0000-4000-8000-000000000002',
    },
  ],
  creadoEn: '2026-10-19T12:00:00Z',
  resueltoEn: null,
  ...datos,
});
