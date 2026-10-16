// Después de un envío con errores, el foco va al primer campo inválido (igual que el resto de los
// formularios de la app). Se espera un cuadro para que React ya haya pintado `aria-invalid`.
export function enfocarPrimerError(contenedor: HTMLElement | null) {
  window.requestAnimationFrame(() => {
    contenedor?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  });
}
