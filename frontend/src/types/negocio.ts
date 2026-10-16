import type { TipoCampo } from './campo';

// «Se ensambla solo»: catálogo de piezas (bloques), respuestas que cada pieza desbloquea y planes.
// Ver docs/ensambla-gestion-simple.md.

/* ---------- catálogo ---------- */

/** Para qué sirve una columna: las respuestas buscan columnas por rol, no por nombre. */
export type RolCampo =
  | 'nombre'
  | 'fecha'
  | 'monto'
  | 'cantidad'
  | 'costo'
  | 'precio'
  | 'stock'
  | 'cliente'
  | 'producto'
  | 'concepto'
  | 'pagado'
  | 'entregado'
  | 'sena'
  | 'unidad';

export type PiezaId = 'productos' | 'ventas' | 'gastos' | 'fiados' | 'pedidos' | 'insumos';

export type RubroId = 'comercio' | 'gastronomia' | 'otro';

export type CampoPieza = {
  nombre: string;
  tipo: TipoCampo;
  rol: RolCampo;
  obligatorio?: boolean;
  opciones?: string[];
  valorInicial?: string | null;
  /** Sólo en FORMULA: expresión con columnas por rol, por ejemplo `{precio} - {costo}`. */
  formula?: string;
};

export type Pieza = {
  id: PiezaId;
  /** Nombre por defecto del bloque (la tabla del sistema). */
  nombre: string;
  /** Otros nombres con los que una tabla ya creada cuenta como esta pieza. */
  alias: string[];
  /** Nombre del bloque según el rubro (por ejemplo «Platos» en gastronomía). */
  nombrePorRubro: Partial<Record<RubroId, string>>;
  /** Una línea, en lenguaje cotidiano. */
  descripcion: string;
  campos: CampoPieza[];
  /** Palabras del relato que la piden («fiado», «seña»…), sin tildes y en minúscula. */
  senales: string[];
  /** Rubros en los que se arma sola aunque no la nombren. */
  base: RubroId[];
};

/** Qué pieza sumó el ensamblador y por qué (se le muestra a la persona). */
export type PiezaEnsamblada = { id: PiezaId; nombre: string; motivo: string };

export type Ensamblado = {
  rubro: RubroId;
  piezas: PiezaEnsamblada[];
};

/* ---------- respuestas ---------- */

export type RespuestaId =
  | 'te-quedo'
  | 'vendiste'
  | 'gastaste'
  | 'margen-producto'
  | 'producto-perdida'
  | 'que-se-termina'
  | 'quien-te-debe'
  | 'pedidos-pendientes'
  | 'costo-plato';

export type DefinicionRespuesta = {
  id: RespuestaId;
  /** La pregunta, tal como la haría la persona. */
  pregunta: string;
  /** Piezas que tienen que estar para poder responderla. */
  requiere: PiezaId[];
  /** Una línea: qué muestra y por qué sirve. */
  ayuda: string;
  /** Las «básicas» entran en el plan Gratis. */
  basica: boolean;
};

export type ItemRespuesta = { nombre: string; valor: string; nota?: string };

export type ResultadoRespuesta =
  /** Falta alguna pieza: «Sumá *Gastos* para ver esto». */
  | { estado: 'bloqueada'; faltan: PiezaId[] }
  /** Están las piezas pero todavía no hay datos. */
  | { estado: 'sin-datos' }
  /** La respuesta es de un plan pago (candado con invitación a mejorar). */
  | { estado: 'plan' }
  | {
      estado: 'lista';
      /** El número o frase grande: «$ 182.000». */
      titular: string;
      /** Una línea que lo explica. */
      detalle: string;
      /** Variación contra el mes anterior, en %; `mejor` dice si es buena noticia. */
      tendencia?: { porcentaje: number; mejor: boolean };
      items?: ItemRespuesta[];
    };

export type RespuestaVista = {
  definicion: DefinicionRespuesta;
  resultado: ResultadoRespuesta;
};

/* ---------- relato («Contá qué pasó») ---------- */

export type FilaRelato = {
  /** En qué pieza va. */
  pieza: Extract<PiezaId, 'ventas' | 'gastos' | 'fiados'>;
  texto: string;
  valores: {
    concepto?: string;
    producto?: string;
    cantidad?: number;
    monto?: number;
    cliente?: string;
  };
};

/* ---------- planes y suscripción (simulados) ---------- */

export type PlanId = 'gratis' | 'negocio' | 'negocio-plus';
export type PeriodoPlan = 'mensual' | 'anual';
export type EstadoSuscripcion = 'gratis' | 'prueba' | 'activa' | 'cancelada' | 'vencida';

export type MedioPago = { tipo: 'tarjeta' | 'mercado-pago'; detalle: string };

export type PagoSuscripcion = {
  id: string;
  fecha: string;
  concepto: string;
  importe: number;
  estado: 'aprobado' | 'rechazado';
};

export type Suscripcion = {
  plan: PlanId;
  estado: EstadoSuscripcion;
  periodo: PeriodoPlan;
  pruebaUsada: boolean;
  pruebaDesde: string | null;
  pruebaHasta: string | null;
  proximoCobro: string | null;
  activoHasta: string | null;
  medioPago: MedioPago | null;
  pagos: PagoSuscripcion[];
};

export type FuncionPlan =
  | 'bloques-ilimitados'
  | 'respuestas-completas'
  | 'comparativas'
  | 'historial'
  | 'descarga-xlsx'
  | 'varias-personas'
  | 'varios-negocios'
  | 'resumen-semanal';
