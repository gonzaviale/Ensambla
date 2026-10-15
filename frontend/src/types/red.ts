// Dominio de «la red proactiva» (docs/diferencial-red-proactiva.md): comercio de barrio, listas de
// proveedores, avisos, ofertas y suscripción. Todo se simula en el cliente (stores/red.ts); no hay API.

/* ---------- productos y proveedores ---------- */

export type PresentacionLista = 'UNIDAD' | 'BULTO';

export type CostoHistorial = {
  fecha: string;
  /** Costo por unidad vendible. */
  costo: number;
  /** Lista de la que salió ese costo (trazabilidad). */
  listaId: string | null;
  proveedorId: string;
};

export type AlternativaProveedor = {
  proveedorId: string;
  /** Costo por unidad que ofrece otro proveedor de la red por el mismo producto. */
  costo: number;
  fecha: string;
};

export type ProductoRed = {
  id: string;
  nombre: string;
  categoria: string;
  /** Proveedor al que se le compra habitualmente. */
  proveedorId: string;
  /** Costo por unidad vendible (lo que paga el comercio). */
  costo: number;
  /** Precio al público actual. */
  precio: number;
  stock: number;
  /** Unidades vendidas en los últimos 7 días. */
  ventasSemana: number;
  /** Cuántas unidades trae un bulto del proveedor, si lo vende por bulto. */
  unidadesPorBulto: number | null;
  /** Desde cuándo rige el costo actual y de qué lista salió. */
  costoDesde: string;
  costoListaId: string | null;
  historial: CostoHistorial[];
  alternativas: AlternativaProveedor[];
};

export type ProveedorRed = {
  id: string;
  nombre: string;
  rubro: string;
  zona: string;
  telefono: string;
  /** Cómo suele mandar la lista. */
  origenHabitual: OrigenLista;
  /** Cada cuántos días suele llegar una lista nueva. */
  frecuenciaDias: number;
};

/* ---------- reglas y umbrales (los decide el dueño) ---------- */

export type ReglaCategoria = {
  /** Porcentaje que se agrega sobre el costo. */
  margen: number;
  /** El precio se redondea hacia arriba al múltiplo de este valor (10, 50, 100). */
  redondeo: number;
};

export type ReglasPrecio = {
  general: ReglaCategoria;
  porCategoria: Record<string, ReglaCategoria>;
  /** Por debajo de este margen (sobre el costo) se avisa «precio en riesgo». */
  margenMinimo: number;
};

export type TipoAlerta =
  | 'precio-en-riesgo'
  | 'lista-nueva'
  | 'stock-por-agotarse'
  | 'oferta-que-sirve'
  | 'compras-caro'
  | 'mejor-proveedor'
  | 'cambio-raro';

export type Umbrales = {
  /** Avisar si el stock alcanza para menos de esta cantidad de días. */
  diasDeStock: number;
  /** Un aumento de costo mayor a este porcentaje se marca como «cambio raro». */
  aumentoRaro: number;
  /** Diferencia mínima (en %) para avisar «comprás caro» o «mejor proveedor». */
  diferenciaCompra: number;
  /** No se compara con la zona si hay menos comercios que esta cantidad (privacidad). */
  minimoComerciosZona: number;
  /** Qué avisos recibe el comercio. */
  activas: Record<TipoAlerta, boolean>;
};

/* ---------- listas de proveedor ---------- */

export type OrigenLista = 'whatsapp' | 'pdf' | 'excel' | 'foto';

export type MotivoDuda = 'unidad-bulto' | 'nombre-parecido' | 'precio-raro';

export type EstadoItem = 'seguro' | 'dudoso' | 'sin-coincidencia';

export type ItemLista = {
  id: string;
  /** La línea tal cual figura en la lista del proveedor. */
  textoOriginal: string;
  /** Precio de la línea tal cual figura (por bulto o por unidad). */
  precioLista: number;
  /** Cómo se interpretó la línea: precio por unidad o por bulto cerrado. */
  presentacion: PresentacionLista;
  unidadesPorBulto: number | null;
  /** Producto del comercio con el que se emparejó, o null si no hay coincidencia. */
  productoId: string | null;
  /** 0 a 1: qué tan segura está Ensambla del emparejamiento. */
  confianza: number;
  estado: EstadoItem;
  motivo: MotivoDuda | null;
  /** Productos candidatos (ids) cuando el emparejamiento es dudoso. */
  sugeridos: string[];
  /** El comercio ya confirmó (o corrigió) el emparejamiento. */
  confirmado: boolean;
  ignorado: boolean;
};

export type EstadoLista = 'por-revisar' | 'aplicada' | 'descartada';

export type ListaProveedor = {
  id: string;
  proveedorId: string;
  nombre: string;
  recibidaEl: string;
  origen: OrigenLista;
  estado: EstadoLista;
  aplicadaEl: string | null;
  items: ItemLista[];
  /** Resumen de las listas ya aplicadas (de las que no se conserva cada línea). */
  resumen: { productos: number; aumentoPromedio: number } | null;
};

/** Un cambio de precio calculado para un ítem emparejado de una lista (no se guarda: se deriva). */
export type CambioPropuesto = {
  itemId: string;
  productoId: string;
  nombre: string;
  categoria: string;
  costoAnterior: number;
  costoNuevo: number;
  variacion: number;
  precioActual: number;
  precioSugerido: number;
  margenActual: number;
  margenConSugerido: number;
  /** Con el precio actual y el costo nuevo, el margen queda por debajo del mínimo. */
  enRiesgo: boolean;
  /** El aumento supera el umbral de «cambio raro». */
  raro: boolean;
};

/** Un precio que se cambió en el sistema y que hay que trasladar al mostrador (etiqueta o lista). */
export type CambioAplicado = {
  id: string;
  productoId: string;
  nombre: string;
  precioAnterior: number;
  precioNuevo: number;
  costoAnterior: number;
  costoNuevo: number;
  fecha: string;
  listaId: string | null;
  /** El comercio ya cambió la etiqueta en el local. */
  trasladado: boolean;
};

/* ---------- avisos, ofertas y pedido ---------- */

export type AccionAlerta =
  | { tipo: 'aplicar-precio'; productoId: string; precioNuevo: number }
  | { tipo: 'revisar-lista'; listaId: string }
  | { tipo: 'agregar-al-pedido'; productoId: string; cantidad: number }
  | { tipo: 'ver-oferta'; ofertaId: string }
  | { tipo: 'ver-proveedores'; productoId: string }
  | { tipo: 'comparar'; productoId: string }
  | { tipo: 'confirmar-cambio'; listaId: string; itemId: string };

export type Alerta = {
  /** Estable entre renders: el mismo problema siempre produce el mismo id. */
  id: string;
  tipo: TipoAlerta;
  /** Una línea, en lenguaje del comercio. */
  titulo: string;
  /** El porqué, en una línea. */
  porque: string;
  /** Plata en juego por mes (para ordenar). */
  impacto: number;
  productoId: string | null;
  proveedorId: string | null;
  accion: AccionAlerta;
  /** Etiqueta del botón de un toque. */
  etiquetaAccion: string;
};

export type EstadoAlerta = 'resuelta' | 'ignorada';

export type OfertaProveedor = {
  id: string;
  proveedorId: string;
  /** Producto del comercio al que aplica, o null si el comercio no lo vende. */
  productoId: string | null;
  nombreProducto: string;
  /** Costo por unidad habitual y de la oferta. */
  costoHabitual: number;
  costoOferta: number;
  vence: string;
  minimoBultos: number | null;
};

export type ReferenciaZona = {
  productoId: string;
  /** Costo promedio por unidad que pagan los comercios de la zona (dato agregado y anónimo). */
  costoPromedio: number;
  comercios: number;
};

export type ItemPedido = { productoId: string; cantidad: number };

/* ---------- negocio y onboarding ---------- */

export type NegocioRed = {
  nombre: string;
  rubro: string;
  zona: string;
  telefono: string;
  /** Lo que el comercio contó en el onboarding, en sus palabras. */
  relato: string;
  onboardingHecho: boolean;
};

/* ---------- planes y suscripción ---------- */

export type PlanId = 'gratis' | 'comercio' | 'red';

export type PeriodoPlan = 'mensual' | 'anual';

export type EstadoSuscripcion = 'gratis' | 'prueba' | 'activa' | 'cancelada' | 'vencida';

export type MedioPago = {
  tipo: 'tarjeta' | 'mercado-pago';
  /** «Visa •••• 4242» o «Mercado Pago». */
  detalle: string;
};

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
  /** Si se canceló: hasta cuándo sigue activo lo ya pagado. */
  activoHasta: string | null;
  medioPago: MedioPago | null;
  pagos: PagoSuscripcion[];
};

export type FuncionPlan =
  | 'alertas-precio'
  | 'listas-ilimitadas'
  | 'stock-por-agotarse'
  | 'resumen-del-dia'
  | 'etiquetas'
  | 'historial-costos'
  | 'comparar-proveedores'
  | 'comparacion-zona'
  | 'ofertas'
  | 'usuarios';

/* ---------- la red (datos agregados del barrio) ---------- */

export type RedBarrio = {
  zona: string;
  comercios: number;
  proveedores: number;
  productosEmparejados: number;
  listasProcesadas: number;
};

export type RedState = {
  negocio: NegocioRed;
  proveedores: ProveedorRed[];
  productos: ProductoRed[];
  listas: ListaProveedor[];
  cambios: CambioAplicado[];
  reglas: ReglasPrecio;
  umbrales: Umbrales;
  ofertas: OfertaProveedor[];
  zona: ReferenciaZona[];
  barrio: RedBarrio;
  pedido: ItemPedido[];
  alertas: Record<string, EstadoAlerta>;
  suscripcion: Suscripcion;
};
