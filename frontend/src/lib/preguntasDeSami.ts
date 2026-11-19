import type { RespuestasDeSami } from './respuestasDeSami';

type Opcion = {
  valor: string;
  titulo: string;
  ayuda: string;
  /** Cuando la ayuda depende de dónde está el local de la persona. */
  ayudaEn?: (localidad: string) => string;
};
export type Pregunta = {
  clave: keyof RespuestasDeSami;
  etiqueta: string;
  texto: string;
  opciones: Opcion[];
};

export const PREGUNTAS: Pregunta[] = [
  {
    clave: 'alcance',
    etiqueta: 'Zona',
    texto: '¿En qué zona querés llegar a nuevos clientes?',
    opciones: [
      {
        valor: 'cerca',
        titulo: 'Cerca de mi local actual',
        ayuda: 'Hasta 4 km de tu local actual',
        ayudaEn: (localidad) => `Hasta 4 km de ${localidad}`,
      },
      {
        valor: 'toda',
        titulo: 'En cualquier zona',
        ayuda: 'Incluye zonas de la Ciudad y de La Matanza',
      },
    ],
  },
  {
    clave: 'prioridad',
    etiqueta: 'Lugares',
    texto: '¿Qué combinación de lugares puede atraer mejor a tu público?',
    opciones: [
      { valor: 'equilibrado', titulo: 'Un equilibrio', ayuda: 'Un poco de cada cosa' },
      {
        valor: 'escuelas',
        titulo: 'Escuelas y plazas',
        ayuda: 'Priorizo escuelas, parques y espacios de encuentro cercanos',
      },
    ],
  },
  {
    clave: 'publico',
    etiqueta: 'Entorno',
    texto: '¿En qué entorno suele moverse el público al que querés llegar?',
    opciones: [
      {
        valor: 'vecinos',
        titulo: 'Sin priorizar un entorno',
        ayuda: 'Miro de forma equilibrada todos los datos disponibles',
      },
      {
        valor: 'estudiantes',
        titulo: 'Escuelas y plazas',
        ayuda: 'Priorizo escuelas, parques y plazas cercanas',
      },
      {
        valor: 'oficinas',
        titulo: 'Oficinas y transporte',
        ayuda: 'Priorizo oficinas, bancos, paradas y estaciones cercanas',
      },
    ],
  },
  {
    clave: 'tamano',
    etiqueta: 'Tamaño',
    texto: '¿Qué tamaño tendría el local?',
    opciones: [
      { valor: 'chico', titulo: 'Chico', ayuda: 'Hasta 70 m²' },
      { valor: 'mediano', titulo: 'Mediano', ayuda: 'De 70 a 200 m²' },
      { valor: 'grande', titulo: 'Grande', ayuda: 'De 200 a 500 m²' },
      { valor: 'muy-grande', titulo: 'Muy grande', ayuda: 'Más de 500 m²' },
    ],
  },
];

export const tituloDeLaRespuesta = (pregunta: Pregunta, valor: string) =>
  pregunta.opciones.find((opcion) => opcion.valor === valor)?.titulo ?? valor;

export const ayudaDeLaOpcion = (opcion: Opcion, localidad: string) =>
  opcion.ayudaEn?.(localidad) ?? opcion.ayuda;
