/** Le entrega a la persona el archivo que mandó el servidor, como una descarga del navegador. */
export function guardarArchivo({ blob, nombre }: { blob: Blob; nombre: string }) {
  const direccion = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = direccion;
  enlace.download = nombre;
  document.body.append(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(direccion);
}
