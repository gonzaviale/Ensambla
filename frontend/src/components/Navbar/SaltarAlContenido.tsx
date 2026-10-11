// Primer elemento enfocable de la página: permite a quien navega con teclado saltar la barra.
export function SaltarAlContenido() {
  return (
    <a
      href="#contenido"
      className="sr-only rounded-md bg-fg px-4 py-3 font-semibold text-white focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-(--z-toast)"
    >
      Ir al contenido
    </a>
  );
}
