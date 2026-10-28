import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { estado } from './estado';
import { DIRECCIONES_DE_PRUEBA, LOCALIDAD_DE_PRUEBA, ZONA_DE_PRUEBA } from './ubicacion';

const DIAS_DE_PRUEBA = 7;

/**
 * Deja la cuenta de demostración lista para mostrar todo sin cargar nada: el almacén de ejemplo (con ventas que
 * disparan avisos de stock, de producto estancado y de combo), el comercio ubicado en la zona de prueba y la prueba
 * de Premium activa. Sólo completa lo que falta: lo que la persona cambió en la demo se respeta.
 */
export function prepararCuentaDemo(email: string, ahora = new Date()) {
  if (!estado.sistemas.has(email)) estado.sistemas.set(email, [crearAlmacenDeEjemplo(hoy())]);
  if (!estado.negocios.has(email)) {
    const direccion = DIRECCIONES_DE_PRUEBA[0];
    estado.negocios.set(email, {
      nombre: 'Almacén de ejemplo',
      rubro: 'ALMACEN',
      ubicacion: {
        latitud: direccion.latitud,
        longitud: direccion.longitud,
        direccion: direccion.direccion,
        localidad: LOCALIDAD_DE_PRUEBA,
      },
      comparteDatosZona: true,
      zona: ZONA_DE_PRUEBA.nombre,
    });
  }
  if (!estado.pruebasPremium.has(email)) {
    estado.pruebasPremium.set(email, {
      inicioEn: ahora.toISOString(),
      finEn: new Date(ahora.getTime() + DIAS_DE_PRUEBA * 24 * 60 * 60 * 1000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });
  }
  estado.alCambiar?.();
}
