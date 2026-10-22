// Lleva la página al principio de todo. Con «reducir movimiento» el salto es directo.
export function subirArriba() {
  const reducido =
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  window.scrollTo({ top: 0, behavior: reducido ? 'auto' : 'smooth' });
}
