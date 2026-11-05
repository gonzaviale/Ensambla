const CLAVE_DE_ALMACENAMIENTO = 'ensambla:telefonos-de-proveedores';

type Telefonos = Record<string, string>;

// El almacenamiento puede no estar (ventana privada, datos bloqueados): sin él la persona vuelve a escribir el número.
function leer(): Telefonos {
  try {
    const guardado = window.localStorage.getItem(CLAVE_DE_ALMACENAMIENTO);
    return guardado ? (JSON.parse(guardado) as Telefonos) : {};
  } catch {
    return {};
  }
}

/** El WhatsApp que la persona anotó la última vez para ese proveedor; vacío si no hay. */
export const telefonoDe = (proveedor: string): string => leer()[proveedor] ?? '';

/** Guarda el número en este navegador para no pedirlo de nuevo. */
export function recordarTelefono(proveedor: string, telefono: string) {
  try {
    window.localStorage.setItem(
      CLAVE_DE_ALMACENAMIENTO,
      JSON.stringify({ ...leer(), [proveedor]: telefono }),
    );
  } catch {
    // Sin almacenamiento no se recuerda: no es un error para la persona.
  }
}
