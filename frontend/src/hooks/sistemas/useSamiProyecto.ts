import { queryOptions, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  conversarConSami,
  crearProyectoConSami,
  listarPlantillasDelServidor,
} from '@/api/samiProyecto';
import { sistemasQuery } from '@/hooks/sistemas/queries';
import { useSistemaStore } from '@/stores/sistemaStore';
import type {
  OpcionDePregunta,
  PreguntaDeSami,
  ProyectoCreado,
  PropuestaDeSami,
  RespuestaDeConversacion,
  RespuestaElegida,
} from '@/types/samiProyecto';

export type MensajeDeSami = { quien: 'sami' | 'you'; texto: string };

const LARGO_MINIMO = 5;
const MENSAJE_CORTO = 'Contame un poquito más para encontrar un punto de partida.';
const SIN_RESPUESTA =
  'Con lo que me contaste todavía no encuentro un punto de partida. ¿Me contás un poco más de cómo trabajás?';

export const plantillasDelServidorQuery = () =>
  queryOptions({
    queryKey: ['plantillas'],
    queryFn: ({ signal }) => listarPlantillasDelServidor(signal),
    staleTime: 5 * 60 * 1000,
  });

const textoDeLaPropuesta = (propuesta: PropuestaDeSami) => {
  const extra = propuesta.columnas.map((columna) => columna.nombre);
  const columnas = extra.length
    ? ` y sumarle ${extra.length === 1 ? 'la columna' : 'las columnas'} ${extra.join(', ')}`
    : '';
  return `Te propongo empezar con «${propuesta.plantilla}»${columnas}. Mirá cómo queda a la derecha y sacá lo que no necesites.`;
};

// «Armalo con Sami» contra el backend real (T-118): la persona cuenta su negocio, Sami pregunta lo que le falta,
// propone una plantilla con columnas extra y, al confirmar, se crea el proyecto. No guarda estado en el servidor:
// cada pedido lleva el texto y las respuestas dadas hasta ahora.
export function useSamiProyecto(saludo: string) {
  const avisar = useSistemaStore((estado) => estado.avisar);
  const incorporarSistemas = useSistemaStore((estado) => estado.incorporarSistemas);
  const queryClient = useQueryClient();
  const conversar = useMutation({ mutationFn: conversarConSami });
  const crear = useMutation({ mutationFn: crearProyectoConSami });
  const plantillas = useQuery(plantillasDelServidorQuery());

  const [mensajes, setMensajes] = useState<MensajeDeSami[]>([{ quien: 'sami', texto: saludo }]);
  const [descripcion, setDescripcion] = useState('');
  const [respuestas, setRespuestas] = useState<RespuestaElegida[]>([]);
  const [pregunta, setPregunta] = useState<PreguntaDeSami | null>(null);
  const [propuesta, setPropuesta] = useState<PropuestaDeSami | null>(null);
  const [quitadas, setQuitadas] = useState<string[]>([]);
  const [nombre, setNombre] = useState('');
  const [creado, setCreado] = useState<ProyectoCreado | null>(null);

  const decir = (quien: MensajeDeSami['quien'], texto: string) =>
    setMensajes((actuales) => [...actuales, { quien, texto }]);

  const recibir = (respuesta: RespuestaDeConversacion) => {
    if (respuesta.tipo === 'PREGUNTA' && respuesta.pregunta) {
      setPregunta(respuesta.pregunta);
      decir('sami', respuesta.pregunta.texto);
    } else if (respuesta.tipo === 'PROPUESTA' && respuesta.propuesta) {
      setPregunta(null);
      setPropuesta(respuesta.propuesta);
      setQuitadas([]);
      setNombre(respuesta.propuesta.plantilla);
      decir('sami', textoDeLaPropuesta(respuesta.propuesta));
    } else {
      // El algoritmo no alcanzó y no hubo propuesta: se vuelve a empezar con una descripción más completa.
      setPregunta(null);
      setDescripcion('');
      setRespuestas([]);
      decir('sami', SIN_RESPUESTA);
    }
  };

  /** `true` si Sami contestó; si falla avisa, deshace lo que se había adelantado y devuelve `false`. */
  const consultar = async (texto: string, elegidas: RespuestaElegida[], alFallar: () => void) => {
    try {
      recibir(await conversar.mutateAsync({ texto, respuestas: elegidas }));
      return true;
    } catch (error) {
      alFallar();
      avisar(error instanceof Error ? error.message : 'Sami no pudo pensar la propuesta.');
      return false;
    }
  };

  /**
   * La persona cuenta su negocio (escribiendo o dictando): empieza una conversación nueva.
   * Devuelve `true` si el texto se mandó y Sami contestó, para que la pantalla vacíe el campo.
   */
  const describir = async (texto: string) => {
    const limpio = texto.trim();
    if (limpio.length < LARGO_MINIMO) {
      avisar(MENSAJE_CORTO);
      return false;
    }
    if (conversar.isPending) return false;
    decir('you', limpio);
    setDescripcion(limpio);
    setRespuestas([]);
    setPropuesta(null);
    setPregunta(null);
    return consultar(limpio, [], () => setDescripcion(''));
  };

  /** Responde la pregunta de Sami con una de las opciones rápidas. */
  const elegir = async (opcion: OpcionDePregunta) => {
    if (!pregunta || conversar.isPending) return;
    const anterior = pregunta;
    const elegidas = [...respuestas, { preguntaId: pregunta.id, opcionId: opcion.id }];
    decir('you', opcion.texto);
    setRespuestas(elegidas);
    setPregunta(null);
    await consultar(descripcion, elegidas, () => {
      setRespuestas(respuestas);
      setPregunta(anterior);
    });
  };

  const alternarColumna = (nombreDeColumna: string) =>
    setQuitadas((actuales) =>
      actuales.includes(nombreDeColumna)
        ? actuales.filter((quitada) => quitada !== nombreDeColumna)
        : [...actuales, nombreDeColumna],
    );

  const columnasElegidas = (propuesta?.columnas ?? []).filter(
    (columna) => !quitadas.includes(columna.nombre),
  );

  /** El proyecto se creó en el servidor: se trae y se suma a la copia local para que aparezca en «Mis cuadernos». */
  const sumarAMisSistemas = async () => {
    try {
      incorporarSistemas(await queryClient.fetchQuery({ ...sistemasQuery(), staleTime: 0 }));
    } catch {
      // Si no se pudo traer, aparece la próxima vez que la persona entre.
    }
  };

  /** Crea el proyecto con la plantilla y sólo las columnas que la persona dejó. */
  const crearProyecto = async () => {
    if (!propuesta || crear.isPending) return;
    try {
      setCreado(
        await crear.mutateAsync({
          plantillaId: propuesta.plantillaId,
          nombre: nombre.trim() || undefined,
          columnas: columnasElegidas.map((columna) => ({
            nombre: columna.nombre,
            tipo: columna.tipo,
            opciones: columna.tipo === 'OPCION' ? columna.opciones : undefined,
          })),
        }),
      );
      await sumarAMisSistemas();
    } catch (error) {
      avisar(
        error instanceof Error ? error.message : 'No pudimos crear el proyecto. Probá de nuevo.',
      );
    }
  };

  const reiniciar = () => {
    setMensajes([{ quien: 'sami', texto: saludo }]);
    setDescripcion('');
    setRespuestas([]);
    setPregunta(null);
    setPropuesta(null);
    setQuitadas([]);
    setNombre('');
    setCreado(null);
  };

  const plantilla = propuesta
    ? plantillas.data?.find((candidata) => candidata.id === propuesta.plantillaId)
    : undefined;

  return {
    mensajes,
    pensando: conversar.isPending,
    pregunta,
    propuesta,
    plantilla,
    quitadas,
    columnasElegidas,
    nombre,
    setNombre,
    creando: crear.isPending,
    creado,
    describir,
    elegir,
    alternarColumna,
    crearProyecto,
    reiniciar,
  };
}
