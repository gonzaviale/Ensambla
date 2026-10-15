// Datos de demostración de la vista del mayorista. Son ilustrativos: no salen de ningún comercio real.

/** Distribuidora Sur es el mayorista de la demostración. */
export const PROVEEDOR_DEMO_ID = 'prov-sur';

/** De los comercios de la red, cuántos le compran a la Distribuidora Sur (cifra de demostración). */
export const COMERCIOS_QUE_LE_COMPRAN = 18;

/** Productos más pedidos esta semana en la zona: cifras agregadas y anónimas, de ejemplo. */
export const DEMANDA_ILUSTRATIVA = [
  { producto: 'Coca-Cola 2,25 L', pedidos: 148 },
  { producto: 'Leche La Serenísima 1 L', pedidos: 131 },
  { producto: 'Harina 000 Pureza 1 kg', pedidos: 96 },
  { producto: 'Azúcar Ledesma 1 kg', pedidos: 84 },
  { producto: 'Yerba Playadito 1 kg', pedidos: 71 },
  { producto: 'Aceite Natura 900 ml', pedidos: 58 },
] as const;
