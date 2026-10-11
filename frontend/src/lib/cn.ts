import { clsx, type ClassValue } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';

// Los tamaños de texto por rol (@theme en styles/index.css) se declaran para que twMerge no los
// confunda con colores (text-body vs text-fg) y resuelva bien los conflictos.
const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      'font-size': [{ text: ['caption', 'hint', 'body', 'lead', 'h3', 'h2', 'h1', 'display'] }],
    },
  },
});

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
