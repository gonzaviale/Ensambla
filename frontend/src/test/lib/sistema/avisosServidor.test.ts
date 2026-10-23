import { describe, expect, it } from 'vitest';
import { aAvisoDeSami } from '@/lib/sistema/avisosServidor';
import { avisoDelServidor } from '@/test/avisosData';

const PROYECTO_ID = 'c0000000-0000-4000-8000-000000000001';

describe('aAvisoDeSami', () => {
  it('arma el aviso con el id del servidor, el tipo y el grupo en minúscula', () => {
    const aviso = aAvisoDeSami(
      avisoDelServidor({ tipo: 'PRECIO_ZONA', grupo: 'VENTAS', prioridad: 2 }),
      new Map(),
    );

    expect(aviso).toMatchObject({
      id: 'servidor:e0000000-0000-4000-8000-000000000001',
      servidorId: 'e0000000-0000-4000-8000-000000000001',
      tipo: 'precio_zona',
      grupo: 'ventas',
      prioridad: 2,
      porque: ['Definiste un mínimo de 10 y quedan 0.'],
    });
  });

  it('usa el nombre del sistema que conoce y, si no, «Tu negocio»', () => {
    const conNombre = aAvisoDeSami(avisoDelServidor(), new Map([[PROYECTO_ID, 'Mi almacén']]));
    const sinNombre = aAvisoDeSami(avisoDelServidor({ proyectoId: null }), new Map());

    expect(conNombre.sistema).toBe('Mi almacén');
    expect(conNombre.sistemaId).toBe(PROYECTO_ID);
    expect(sinNombre.sistema).toBe('Tu negocio');
    expect(sinNombre.sistemaId).toBe('');
  });

  it('registrar y abrir llevan a la tabla del proyecto', () => {
    const aviso = aAvisoDeSami(
      avisoDelServidor({
        acciones: [
          { tipo: 'REGISTRAR', proyectoId: PROYECTO_ID, bloqueId: 'entradas' },
          { tipo: 'ABRIR', proyectoId: PROYECTO_ID, bloqueId: 'inventario', registroId: 'fila-1' },
          { tipo: 'ABRIR' },
        ],
      }),
      new Map(),
    );

    expect(aviso.acciones).toEqual([
      {
        tipo: 'registrar',
        texto: 'Registrar entrada',
        sistemaId: PROYECTO_ID,
        bloqueId: 'entradas',
      },
      {
        tipo: 'abrir',
        texto: 'Ver producto',
        sistemaId: PROYECTO_ID,
        bloqueId: 'inventario',
        filaId: 'fila-1',
      },
      { tipo: 'abrir', texto: 'Ver producto', sistemaId: '', bloqueId: '', filaId: undefined },
    ]);
  });

  it('editar un registro se le pide al servidor, que lo aplica y cierra el aviso', () => {
    const aviso = aAvisoDeSami(
      avisoDelServidor({
        acciones: [
          { tipo: 'EDITAR_REGISTRO', texto: 'Subir a $4.200', registroId: 'fila-1' },
          { tipo: 'EDITAR_REGISTRO' },
        ],
      }),
      new Map(),
    );

    expect(aviso.acciones).toEqual([
      {
        tipo: 'servidor',
        texto: 'Subir a $4.200',
        avisoId: 'e0000000-0000-4000-8000-000000000001',
        confirmacion: 'Listo, aplicamos el cambio.',
      },
      {
        tipo: 'servidor',
        texto: 'Aplicar el cambio',
        avisoId: 'e0000000-0000-4000-8000-000000000001',
        confirmacion: 'Listo, aplicamos el cambio.',
      },
    ]);
  });

  it('crear un registro (el combo) se le pide al servidor, que lo crea y cierra el aviso', () => {
    const aviso = aAvisoDeSami(
      avisoDelServidor({
        tipo: 'COMBO',
        grupo: 'VENTAS',
        acciones: [
          {
            tipo: 'CREAR_REGISTRO',
            texto: 'Crear el combo',
            proyectoId: PROYECTO_ID,
            bloqueId: 'inventario',
            valores: { name: 'Combo A + B', quantity: 0, price: 1850 },
          },
          { tipo: 'CREAR_REGISTRO' },
        ],
      }),
      new Map(),
    );

    expect(aviso.tipo).toBe('combo');
    expect(aviso.acciones).toEqual([
      {
        tipo: 'servidor',
        texto: 'Crear el combo',
        avisoId: 'e0000000-0000-4000-8000-000000000001',
        confirmacion: 'Listo, creamos el combo.',
      },
      {
        tipo: 'servidor',
        texto: 'Crear el combo',
        avisoId: 'e0000000-0000-4000-8000-000000000001',
        confirmacion: 'Listo, creamos el combo.',
      },
    ]);
  });
});
