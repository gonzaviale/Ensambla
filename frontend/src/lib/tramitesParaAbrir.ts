// Qué hay que tramitar para abrir un local en la Ciudad de Buenos Aires o en La Matanza. Sale de las páginas
// oficiales de cada gobierno (consultadas en noviembre de 2026). Lo que esas páginas no dicen se marca «a
// confirmar»: acá no se inventa nada. Cada gobierno puede cambiar los requisitos, así que Sami siempre aclara
// que hay que confirmarlos antes de empezar.
import { UN_RUBRO, type Jurisdiccion, type Rubro } from './dondeAbrir';

/** Cómo se hace un trámite: por internet, yendo a una oficina, de las dos formas, o no está confirmado. */
export type Modalidad = 'online' | 'oficina' | 'mixta' | 'confirmar';

export const ETIQUETA_DE_MODALIDAD: Record<Modalidad, string> = {
  online: 'Online',
  oficina: 'En oficina',
  mixta: 'Online y en oficina',
  confirmar: 'A confirmar',
};

export type Aprobacion = {
  id: string;
  nombre: string;
  organismo: string;
  paraQue: string;
  modalidad: Modalidad;
  /** Cuándo corresponde, si no es siempre. */
  cuando?: string;
  /** Página oficial que explica este requisito, cuando existe una referencia directa. */
  fuente?: Fuente;
};

export type Paso = { texto: string; modalidad?: Modalidad };
export type Oficina = { nombre: string; donde: string; horario?: string; contacto?: string };
export type Fuente = { texto: string; url: string };

export type GuiaDeTramites = {
  jurisdiccion: Jurisdiccion;
  titulo: string;
  /** Lo que Sami cuenta, en pocas palabras. */
  resumenSami: string;
  modalidad: Modalidad;
  costo: string;
  costoCorto: string;
  profesional: string;
  profesionalCorto: string;
  plazo: string;
  aprobaciones: Aprobacion[];
  documentos: string[];
  tituloDePasos: string;
  pasos: Paso[];
  oficinas: Oficina[];
  avisos: string[];
  fuentes: Fuente[];
};

/* Ciudad de Buenos Aires */

/** Qué trámite de habilitación corresponde según el tamaño del local. */
export type CaminoCaba = 'expres' | 'sin-planos' | 'con-planos';

export const MAXIMO_EXPRES_M2 = 200;
export const MAXIMO_SIN_PLANOS_M2 = 500;
export const MAXIMO_CROQUIS_LA_MATANZA_M2 = 70;

export function caminoCaba(superficieM2: number): CaminoCaba {
  if (superficieM2 <= MAXIMO_EXPRES_M2) return 'expres';
  if (superficieM2 <= MAXIMO_SIN_PLANOS_M2) return 'sin-planos';
  return 'con-planos';
}

const FUENTES_CABA: Fuente[] = [
  {
    texto: 'Habilitación exprés (Ciudad de Buenos Aires)',
    url: 'https://buenosaires.gob.ar/tramites/habilitacion-de-actividad-economica-expres',
  },
  {
    texto: 'Habilitación de actividad económica (Ciudad de Buenos Aires)',
    url: 'https://buenosaires.gob.ar/tramites/habilitacion-de-actividad-economica',
  },
];

function guiaCaba(rubro: Rubro, superficieM2: number): GuiaDeTramites {
  const camino = caminoCaba(superficieM2);
  const esExpres = camino === 'expres';
  const consultaDeUsos: Aprobacion = {
    id: 'consulta-de-usos',
    nombre: 'Consulta de usos',
    organismo: 'Dirección General de Interpretación Urbanística',
    paraQue: 'Confirma qué usos admite el lugar según el Código Urbanístico.',
    modalidad: 'confirmar',
    cuando: 'Solo si el local está en un área especial o el uso lo exige. Tiene costo.',
    fuente: FUENTES_CABA[1],
  };
  const aprobaciones: Aprobacion[] = esExpres
    ? [
        {
          id: 'habilitacion-expres',
          nombre: 'Habilitación de actividad económica, modalidad exprés',
          organismo: 'Agencia Gubernamental de Control',
          paraQue: 'Habilita el local si tiene hasta 200 m² y cumple las condiciones del trámite.',
          modalidad: 'online',
          fuente: FUENTES_CABA[0],
        },
        {
          id: 'caa',
          nombre: 'Certificado de Aptitud Ambiental',
          organismo: 'Agencia de Protección Ambiental',
          paraQue: 'Se genera solo al completar todos los formularios del trámite exprés.',
          modalidad: 'online',
          fuente: FUENTES_CABA[0],
        },
        consultaDeUsos,
      ]
    : [
        {
          id: 'habilitacion-general',
          nombre:
            camino === 'sin-planos'
              ? 'Habilitación de actividad económica, declaración responsable sin planos'
              : 'Habilitación de actividad económica, declaración responsable con planos',
          organismo: 'Agencia Gubernamental de Control',
          paraQue:
            camino === 'sin-planos'
              ? 'Hasta 500 m². Podés empezar a trabajar de inmediato; después hay una revisión técnica.'
              : 'Es obligatoria si el local supera los 500 m². Podés empezar de inmediato; después hay una revisión técnica.',
          modalidad: 'online',
          fuente: FUENTES_CABA[1],
        },
        {
          id: 'anexo-tecnico',
          nombre: 'Anexo Técnico firmado por un profesional',
          organismo: 'Arquitecto, ingeniero o maestro mayor de obras matriculado',
          paraQue: 'Lo carga el profesional con el número de trámite y el código de seguridad.',
          modalidad: 'confirmar',
          fuente: FUENTES_CABA[1],
        },
        {
          id: 'caa',
          nombre: 'Certificado de Aptitud Ambiental',
          organismo: 'Agencia de Protección Ambiental',
          paraQue: 'Verifica que el local cumpla las normas ambientales.',
          modalidad: 'confirmar',
          fuente: FUENTES_CABA[1],
        },
        consultaDeUsos,
      ];

  const resumenSami = {
    expres: `Para ${UN_RUBRO[rubro]} de ${superficieM2} m² en la Ciudad te sirve la habilitación exprés, siempre que el local cumpla las condiciones. Se hace todo online con tu usuario miBA nivel 3, no tiene costo y no hace falta un profesional.`,
    'sin-planos': `Con ${superficieM2} m² ya no entrás en el trámite exprés, que llega hasta 200 m². Te corresponde la declaración responsable sin planos: se hace online y podés empezar de inmediato, pero necesitás un profesional matriculado que firme el Anexo Técnico.`,
    'con-planos': `Con más de 500 m² la declaración responsable es con planos y es obligatoria. Se hace online, necesitás un profesional matriculado y, según el caso, el plano de uso de la actividad.`,
  }[camino];

  const documentosBase = [
    'Usuario y clave de miBA nivel 3',
    'CUIT de quien firma el trámite (persona física o jurídica)',
    'Datos del local: dimensiones, tipo de superficie, ventilación y destino de uso',
    'Rubro y actividades que vas a desarrollar',
  ];
  const documentos = esExpres
    ? documentosBase
    : [
        ...documentosBase,
        'Formulario de conformación de la unidad de uso (declaración jurada)',
        'Anexo Técnico firmado por un profesional matriculado',
        'Según el caso: certificado de sobrecarga, plano de uso de la actividad o autorización de emplazamiento',
        'Documentación específica de tu rubro, si la normativa la pide',
      ];

  const pasos: Paso[] = esExpres
    ? [
        {
          texto: 'Entrá con tu usuario miBA (nivel 3) al trámite de habilitación exprés.',
          modalidad: 'online',
        },
        {
          texto: 'Completá los formularios con los datos del local y de la actividad.',
          modalidad: 'online',
        },
        {
          texto: 'Al terminar, el sistema genera solo el Certificado de Aptitud Ambiental.',
          modalidad: 'online',
        },
        {
          texto:
            'Si el sistema detecta que no cumplís un requisito, te deriva al trámite que corresponde.',
          modalidad: 'online',
        },
      ]
    : [
        {
          texto:
            'Iniciá la solicitud en la plataforma SSIT con tu miBA y elegí «Nueva Autorización de Actividad Económica».',
          modalidad: 'online',
        },
        { texto: 'Cargá el CUIT de quien firma el trámite.', modalidad: 'online' },
        {
          texto:
            'Declará la ubicación del local: se genera el número de trámite y el código de seguridad que necesita el profesional.',
          modalidad: 'online',
        },
        {
          texto:
            'Tramitá el Certificado de Aptitud Ambiental ante la Agencia de Protección Ambiental.',
        },
        { texto: 'El profesional completa y firma el Anexo Técnico.' },
        {
          texto:
            'La Dirección General de Habilitaciones y Permisos evalúa: puede librar al uso, aprobar, observar o rechazar.',
        },
        {
          texto:
            'Si lo libra al uso, recibís la oblea con código QR y el Libro de Registro de Inspecciones Digital. Ojo: librar al uso no es lo mismo que estar habilitado.',
        },
      ];

  return {
    jurisdiccion: 'caba',
    titulo: 'Abrir en la Ciudad de Buenos Aires',
    resumenSami,
    modalidad: 'online',
    costo:
      'La habilitación no tiene costo, según la Ley Tarifaria vigente. La consulta de usos sí lo tiene.',
    costoCorto: 'Sin costo',
    profesional: esExpres
      ? 'No hace falta en el trámite exprés.'
      : 'Sí: un arquitecto, ingeniero o maestro mayor de obras matriculado firma el Anexo Técnico.',
    profesionalCorto: esExpres ? 'Sin profesional' : 'Con profesional',
    plazo: 'La Ciudad no publica un plazo de tramitación.',
    aprobaciones,
    documentos,
    tituloDePasos: 'Paso a paso',
    pasos,
    oficinas: [
      {
        nombre: 'Consultas sobre habilitaciones',
        donde:
          'Mesa de Atención Virtual o, con turno, de forma presencial (Sistema de Turnos de la Ciudad)',
      },
    ],
    avisos: [
      'El trámite exprés no sirve en galerías, shoppings, mercados o estaciones, en edificios catalogados o áreas especiales, si el local tiene entrepisos, ni si vendés explosivos, inflamables o medicamentos sin envasar.',
      'Tampoco entran las actividades con normativa específica, como farmacia o agencia de lotería.',
      'Según tu rubro, pueden pedirte documentación específica.',
      'Las licencias duran 15 años; después hay que presentar una declaración responsable de actualización de datos.',
    ],
    fuentes: FUENTES_CABA,
  };
}

/* La Matanza */

const FUENTES_LA_MATANZA: Fuente[] = [
  {
    texto: 'Habilitación de comercio (La Matanza)',
    url: 'https://www.lamatanza.gov.ar/tramites/17/details',
  },
  {
    texto: 'Rubros comerciales (La Matanza)',
    url: 'https://www.lamatanza.gov.ar/tramites/22/details',
  },
  {
    texto: 'Planeamiento urbano (La Matanza)',
    url: 'https://www.lamatanza.gov.ar/planeamientourbano',
  },
];

function guiaLaMatanza(rubro: Rubro, superficieM2: number): GuiaDeTramites {
  const vendeAlimentos = rubro !== 'papeleria';
  const alcanzaCroquis = superficieM2 <= MAXIMO_CROQUIS_LA_MATANZA_M2;

  const aprobaciones: Aprobacion[] = [
    {
      id: 'zonificacion',
      nombre: 'Consulta de zonificación',
      organismo: 'Dirección de Planeamiento',
      paraQue:
        'El municipio recomienda hacerla antes de empezar: confirma si tu rubro se puede abrir en esa dirección.',
      modalidad: 'oficina',
      fuente: FUENTES_LA_MATANZA[2],
    },
    {
      id: 'habilitacion',
      nombre: 'Habilitación de comercio',
      organismo: 'Departamento de Habilitaciones',
      paraQue:
        'El trámite principal. Los formularios se piden online y en la oficina te ayudan a armarlos.',
      modalidad: 'mixta',
      fuente: FUENTES_LA_MATANZA[0],
    },
    {
      id: 'libre-deuda',
      nombre: 'Libre deuda de ABL',
      organismo: 'Banco de Cuentas Municipal',
      paraQue: 'Se pide para acreditar que el inmueble no tiene deuda.',
      modalidad: 'oficina',
      fuente: FUENTES_LA_MATANZA[0],
    },
    {
      id: 'contrato',
      nombre: 'Certificación del contrato de alquiler o comodato',
      organismo: 'Dirección de Catastro',
      paraQue: 'Certifica el contrato con el que acreditás que podés usar el local.',
      modalidad: 'oficina',
      fuente: FUENTES_LA_MATANZA[0],
    },
    {
      id: 'inscripcion-fiscal',
      nombre: 'Inscripción en AFIP y en ARBA o Convenio Multilateral',
      organismo: 'AFIP y ARBA',
      paraQue: 'Piden la constancia con la actividad y el domicilio del local.',
      modalidad: 'confirmar',
    },
  ];
  if (vendeAlimentos) {
    aprobaciones.push({
      id: 'alimentos',
      nombre: 'Análisis de agua, curso de manipulación de alimentos y desinfección',
      organismo: 'Empresa habilitada en La Matanza',
      paraQue: 'Los rubros alimenticios los presentan junto con el resto de la documentación.',
      modalidad: 'confirmar',
      cuando: 'Si vendés alimentos.',
      fuente: FUENTES_LA_MATANZA[0],
    });
  }
  aprobaciones.push({
    id: 'reba',
    nombre: 'Habilitación para vender bebidas alcohólicas (REBA)',
    organismo: 'REBA',
    paraQue:
      'Se pide por mail después de la preinspección y de tener el número de expediente de la habilitación.',
    modalidad: 'online',
    cuando: 'Si vendés bebidas alcohólicas.',
  });
  if (rubro === 'kiosco') {
    aprobaciones.push({
      id: 'maxikiosco',
      nombre: 'Reglamentación especial del maxikiosco',
      organismo: 'Municipio (Ordenanza 14630)',
      paraQue: 'El maxikiosco es un rubro con reglamentación especial.',
      modalidad: 'confirmar',
      cuando: 'Si tu kiosco es un maxikiosco.',
      fuente: FUENTES_LA_MATANZA[1],
    });
  }

  const documentos = [
    'DNI, frente y dorso (si sos extranjero, con residencia vigente)',
    'Certificado del Registro de Deudores Alimentarios Morosos (RDAM), gratis por web',
    'Escritura, comodato (con cláusula de no restitución anticipada por 3 años) o alquiler de mínimo 36 meses (Ley 27.551)',
    'Libre deuda de ABL y contrato certificado en Catastro',
    alcanzaCroquis
      ? 'Croquis: en rubros básicos de hasta 70 m² alcanza el formulario F15, firmado por propietario y titular'
      : 'Plano de obra (el formulario F15 alcanza solo en rubros básicos de hasta 70 m²)',
    'Troquel de matafuegos vigente, o factura de compra o recarga',
    'Constancia de AFIP y de ARBA o Convenio Multilateral, con la actividad y el domicilio del local',
    ...(vendeAlimentos
      ? [
          'Análisis de agua, curso de manipulación de alimentos y factura de desinfección (si vendés alimentos)',
        ]
      : []),
    'Si es una sociedad: contrato social, acta de designación de autoridades y, según el caso, autorización de IGJ',
  ];

  const resumenSami = `Según la página del municipio, los trámites de habilitación se hacen online, pero varios pasos se hacen en oficinas de San Justo: la zonificación, el libre deuda de ABL y la certificación del contrato. ${
    alcanzaCroquis
      ? 'Con hasta 70 m² y un rubro básico alcanza un croquis (formulario F15).'
      : 'Con más de 70 m² piden plano de obra.'
  }${vendeAlimentos ? ' Si vendés alimentos, suman análisis de agua, curso de manipulación y desinfección.' : ''}`;

  return {
    jurisdiccion: 'la-matanza',
    titulo: 'Abrir en La Matanza',
    resumenSami,
    modalidad: 'mixta',
    costo:
      'El municipio menciona los derechos de habilitación y la tasa de Seguridad e Higiene, pero no publica los montos.',
    costoCorto: 'Costos no publicados',
    profesional: alcanzaCroquis
      ? 'No lo menciona: con hasta 70 m² y un rubro básico alcanza el formulario F15 firmado por propietario y titular.'
      : 'No lo aclara: para más de 70 m² piden plano de obra y el municipio no dice quién lo firma.',
    profesionalCorto: 'A confirmar',
    plazo: 'El municipio no publica un plazo de tramitación.',
    aprobaciones,
    documentos,
    tituloDePasos: 'Orden sugerido (el municipio no publica el paso a paso)',
    pasos: [
      { texto: 'Consultá la zonificación de la dirección en Planeamiento.', modalidad: 'oficina' },
      {
        texto: 'Juntá los documentos y sacá el libre deuda de ABL y la certificación del contrato.',
        modalidad: 'oficina',
      },
      {
        texto:
          'Completá los formularios de habilitación; en el Departamento de Habilitaciones te ayudan a armarlos.',
        modalidad: 'mixta',
      },
      {
        texto:
          'Con el número de expediente y la preinspección, pedí la habilitación de bebidas alcohólicas (REBA), si vendés.',
        modalidad: 'online',
      },
    ],
    oficinas: [
      {
        nombre: 'Departamento de Habilitaciones (asesoramiento para los formularios)',
        donde: 'Perú 2250, 4º piso, San Justo',
        horario: 'Lunes a viernes de 8:30 a 13:30',
      },
      {
        nombre: 'Subsecretaría General de Planificación Operativa y Control Comunal',
        donde: 'Perú 2250, 4º piso, San Justo',
        horario: 'Lunes a viernes de 8 a 14',
        contacto: 'secretariacontrolcomunal@lamatanza.gov.ar',
      },
      {
        nombre: 'Dirección de Planeamiento (zonificación)',
        donde: 'Yrigoyen 2746, San Justo',
        contacto: 'Tel. 4651-0014, interno 113',
      },
      {
        nombre: 'Banco de Cuentas Municipal (libre deuda de ABL)',
        donde: 'Monseñor Marcón 3133, San Justo',
      },
      {
        nombre: 'Dirección de Catastro (certificación de contratos)',
        donde: 'Perú 2250, 1º piso, San Justo',
      },
      {
        nombre: 'REBA (bebidas alcohólicas)',
        donde: 'Por mail',
        contacto: 'reba@lamatanza.gov.ar',
      },
    ],
    avisos: [
      'La página del municipio dice que, por la emergencia sanitaria, todos los trámites de habilitación se hacen online. Ese texto puede estar desactualizado: confirmalo antes de ir.',
      'El municipio no informa costos ni plazos.',
      'Hay rubros básicos, rubros con autorización de otro organismo (por ejemplo, el Banco Central o el RENAR) y rubros con reglamentación especial.',
      'Si el local es una subparcela, hace falta un reglamento de copropiedad que permita la actividad; en rubros básicos puede alcanzar con una nota.',
    ],
    fuentes: FUENTES_LA_MATANZA,
  };
}

export function guiaDeTramites(
  jurisdiccion: Jurisdiccion,
  rubro: Rubro,
  superficieM2: number,
): GuiaDeTramites {
  return jurisdiccion === 'caba'
    ? guiaCaba(rubro, superficieM2)
    : guiaLaMatanza(rubro, superficieM2);
}
