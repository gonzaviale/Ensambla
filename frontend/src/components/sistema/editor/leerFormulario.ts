import type { Bloque, Valor } from '@/lib/sistema/tipos';

// Lo que la persona cargó en un formulario de registro, por clave de campo (recordValues() del mockup).
export function leerValores(formulario: HTMLFormElement, bloque: Bloque) {
  const valores: Record<string, Valor> = {};
  for (const campo of bloque.fields) {
    const control = formulario.elements.namedItem(campo.key);
    valores[campo.key] =
      campo.type === 'checkbox'
        ? Boolean((control as HTMLInputElement | null)?.checked)
        : ((control as HTMLInputElement | null)?.value ?? '');
  }
  return valores;
}

/** El primer campo con error recibe el foco, para que se lea el mensaje. */
export function enfocarPrimerError(formulario: HTMLFormElement | null) {
  formulario
    ?.querySelector<HTMLElement>('.invalid input, .invalid textarea, .invalid select')
    ?.focus();
}
