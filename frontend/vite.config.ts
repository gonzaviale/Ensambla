/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { tanstackRouter } from '@tanstack/router-plugin/vite';
import type { Plugin as PluginPostcss } from 'postcss';
import prefixSelector from 'postcss-prefix-selector';

// Las hojas del mockup (src/styles/sistema) se usan sin modificar, pero sus reglas globales
// (body, p, h1, a, button…) pisarían la landing y el acceso: se acotan bajo `.mk-root`.
const ES_DEL_SISTEMA = /[\\/]styles[\\/]sistema[\\/]/;
const ELEMENTOS_RAIZ = /^(:root|html|body)(?![\w-])/;
// Las animaciones del mockup se llaman como las del tema (`fade-in`): se renombran para no mezclarse.
const ANIMACIONES = ['reveal', 'fade-in', 'slide-in', 'thinking', 'campana', 'tour-appear'];

const acotarSistema = prefixSelector({
  prefix: '.mk-root',
  transform(prefix, selector, prefijado, archivo) {
    if (!archivo || !ES_DEL_SISTEMA.test(archivo)) return selector;
    // `body.x` pasa a `.mk-root.x`: la aplicación marca esas clases sobre su propio contenedor.
    return ELEMENTOS_RAIZ.test(selector) ? selector.replace(ELEMENTOS_RAIZ, prefix) : prefijado;
  },
});

const renombrarAnimaciones: PluginPostcss = {
  postcssPlugin: 'renombrar-animaciones-del-sistema',
  Once(raiz) {
    if (!ES_DEL_SISTEMA.test(raiz.source?.input.file ?? '')) return;
    raiz.walkAtRules('keyframes', (regla) => {
      if (ANIMACIONES.includes(regla.params)) regla.params = `mk-${regla.params}`;
    });
    raiz.walkDecls(/^animation(-name)?$/, (declaracion) => {
      for (const nombre of ANIMACIONES) {
        declaracion.value = declaracion.value.replace(
          new RegExp(`(^|[\\s,])${nombre}(?=[\\s,;]|$)`, 'g'),
          `$1mk-${nombre}`,
        );
      }
    });
  },
};

export default defineConfig({
  plugins: [tanstackRouter({ target: 'react', autoCodeSplitting: true }), react(), tailwindcss()],
  css: { postcss: { plugins: [acotarSistema, renombrarAnimaciones] } },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: fileURLToPath(new URL('./src/test/environment.ts', import.meta.url)),
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    testTimeout: 20_000,
  },
});
