import { describe, it } from 'vitest';
import { aAvisoDeSami, tiposCubiertosPorElServidor } from '@/lib/sistema/avisosServidor';
import { avisoDelServidor } from '@/test/avisosData';
import {
  cuandoSeCalcula,
  entoncesElResultadoCoincideCon,
  entoncesElResultadoDe,
  entoncesElResultadoEs,
} from '@/test/calculo';

const PROYECTO_ID = 'c0000000-0000-4000-8000-000000000001';
const AVISO_ID = 'e0000000-0000-4000-8000-000000000001';

type AvisoDeSami = ReturnType<typeof aAvisoDeSami>;
type Acciones = NonNullable<Parameters<typeof avisoDelServidor>[0]>['acciones'];

const traducir = (
  cambios: Parameters<typeof avisoDelServidor>[0] = {},
  nombres = new Map<string, string>(),
) => cuandoSeCalcula(() => aAvisoDeSami(avisoDelServidor(cambios), nombres));

const lasAcciones = (esperadas: unknown[]) =>
  entoncesElResultadoDe<AvisoDeSami>((aviso) => aviso.acciones, esperadas);

const conAcciones = (acciones: Acciones): { acciones: Acciones } => ({ acciones });

const delServidor = (...tipos: Array<'STOCK' | 'ESTANCADO'>) =>
  tipos.map((tipo) => aAvisoDeSami(avisoDelServidor({ tipo }), new Map()));

describe('aAvisoDeSami', () => {
  it('arma el aviso con el id del servidor, el tipo y el grupo en minúscula', () => {
    traducir({ tipo: 'PRECIO_ZONA', grupo: 'VENTAS', prioridad: 2 });

    entoncesElResultadoCoincideCon({
      id: `servidor:${AVISO_ID}`,
      servidorId: AVISO_ID,
      tipo: 'precio_zona',
      grupo: 'ventas',
      prioridad: 2,
      porque: ['Definiste un mínimo de 10 y quedan 0.'],
    });
  });

  it('usa el nombre del sistema que conoce', () => {
    traducir({}, new Map([[PROYECTO_ID, 'Mi almacén']]));

    entoncesElResultadoCoincideCon({ sistema: 'Mi almacén', sistemaId: PROYECTO_ID });
  });

  it('si no conoce el sistema usa «Tu negocio»', () => {
    traducir({ proyectoId: null });

    entoncesElResultadoCoincideCon({ sistema: 'Tu negocio', sistemaId: '' });
  });

  it('registrar y abrir llevan a la tabla del proyecto', () => {
    traducir(
      conAcciones([
        { tipo: 'REGISTRAR', proyectoId: PROYECTO_ID, bloqueId: 'entradas' },
        { tipo: 'ABRIR', proyectoId: PROYECTO_ID, bloqueId: 'inventario', registroId: 'fila-1' },
        { tipo: 'ABRIR' },
      ]),
    );

    lasAcciones([
      {
        tipo: 'registrar',
        texto: 'Cargar entrada',
        sistemaId: PROYECTO_ID,
        bloqueId: 'entradas',
        avisoId: AVISO_ID,
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
    traducir(
      conAcciones([
        { tipo: 'EDITAR_REGISTRO', texto: 'Subir a $4.200', registroId: 'fila-1' },
        { tipo: 'EDITAR_REGISTRO' },
      ]),
    );

    lasAcciones([
      {
        tipo: 'servidor',
        texto: 'Subir a $4.200',
        avisoId: AVISO_ID,
        confirmacion: 'Listo, aplicamos el cambio.',
      },
      {
        tipo: 'servidor',
        texto: 'Aplicar el cambio',
        avisoId: AVISO_ID,
        confirmacion: 'Listo, aplicamos el cambio.',
      },
    ]);
  });

  it('ver en Mi zona lleva a la zona con el producto, sin pedirle nada al servidor', () => {
    traducir({
      tipo: 'CONSUMO_ZONA',
      grupo: 'ZONA',
      ...conAcciones([
        { tipo: 'VER_ZONA', texto: 'Ver en Mi zona', producto: 'Café La Virginia 250 g' },
        { tipo: 'VER_ZONA' },
      ]),
    });

    lasAcciones([
      { tipo: 'zona', texto: 'Ver en Mi zona', producto: 'Café La Virginia 250 g' },
      { tipo: 'zona', texto: 'Ver en Mi zona', producto: undefined },
    ]);
  });

  it('un aviso de consumo en la zona conserva su tipo', () => {
    traducir({ tipo: 'CONSUMO_ZONA', grupo: 'ZONA' });

    entoncesElResultadoCoincideCon({ tipo: 'consumo_zona' });
  });

  it('crear una fila (un combo) también se le pide al servidor, que la crea y cierra el aviso', () => {
    traducir({
      tipo: 'COMBO',
      ...conAcciones([
        {
          tipo: 'CREAR_REGISTRO',
          texto: 'Crear el combo',
          proyectoId: PROYECTO_ID,
          bloqueId: 'inventario',
        },
        { tipo: 'CREAR_REGISTRO' },
      ]),
    });

    lasAcciones([
      {
        tipo: 'servidor',
        texto: 'Crear el combo',
        avisoId: AVISO_ID,
        confirmacion: 'Listo, lo creamos.',
      },
      {
        tipo: 'servidor',
        texto: 'Crear la fila',
        avisoId: AVISO_ID,
        confirmacion: 'Listo, lo creamos.',
      },
    ]);
  });

  it('un aviso de combo conserva su tipo', () => {
    traducir({ tipo: 'COMBO' });

    entoncesElResultadoCoincideCon({ tipo: 'combo' });
  });
});

describe('tiposCubiertosPorElServidor', () => {
  it('sin motor activo sólo cubre los tipos que el servidor ya trajo', () => {
    cuandoSeCalcula(() => [...tiposCubiertosPorElServidor(delServidor('ESTANCADO'), false)]);

    entoncesElResultadoEs(['estancado']);
  });

  it('sin motor activo ni avisos no cubre ningún tipo', () => {
    cuandoSeCalcula(() => tiposCubiertosPorElServidor([], false).size);

    entoncesElResultadoEs(0);
  });

  it('con el motor activo cubre siempre los tipos del motor, aunque no haya ninguno pendiente', () => {
    // Un aviso aplicado o descartado ya no está en la lista: la copia del navegador no puede volver a mostrarlo.
    cuandoSeCalcula(() => [...tiposCubiertosPorElServidor([], true)]);

    entoncesElResultadoEs([
      'stock',
      'estancado',
      'precio_zona',
      'muy_vendido',
      'revisar_precio',
      'combo',
      'consumo_zona',
    ]);
  });

  it('con el motor activo cubre todos los tipos de aviso, también los que trajo el servidor', () => {
    cuandoSeCalcula(() =>
      [...tiposCubiertosPorElServidor(delServidor('STOCK', 'ESTANCADO'), true)].sort(),
    );

    entoncesElResultadoEs([
      'combo',
      'consumo_zona',
      'estancado',
      'muy_vendido',
      'precio_zona',
      'revisar_precio',
      'stock',
    ]);
  });

  it('sin motor activo suma los tipos que trajo el servidor', () => {
    cuandoSeCalcula(() =>
      [...tiposCubiertosPorElServidor(delServidor('STOCK', 'ESTANCADO'), false)].sort(),
    );

    entoncesElResultadoEs(['estancado', 'stock']);
  });
});
