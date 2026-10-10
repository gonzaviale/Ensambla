import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import pluginQuery from '@tanstack/eslint-plugin-query';
import pluginRouter from '@tanstack/eslint-plugin-router';

// Capas (de arriba hacia abajo): routes > pages > layouts > components > hooks > api / stores > types, lib.
// Cada capa solo importa de las que están por debajo. Los datos llegan a la UI por hooks/, nunca por api/.
const noDependeDe = (...grupos) => ({
  patterns: grupos.map(([group, message]) => ({ group, message })),
});

const arriba = {
  routes: ['@/routes/*', '@/routeTree.gen'],
  pages: ['@/pages/*'],
  layouts: ['@/layouts/*'],
  config: ['@/config/router', '@/config/providers'],
};

export default tseslint.config(
  { ignores: ['dist', 'public', 'src/routeTree.gen.ts'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.strict,
      ...pluginQuery.configs['flat/recommended'],
      ...pluginRouter.configs['flat/recommended'],
    ],
    languageOptions: { globals: globals.browser },
    plugins: { 'react-hooks': reactHooks, 'react-refresh': reactRefresh },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowExportNames: ['Route'] }],
      '@typescript-eslint/consistent-type-imports': 'error',
    },
  },
  {
    // pages y components consumen datos por hooks/, no llaman a la API directamente.
    files: ['src/pages/**/*.{ts,tsx}', 'src/components/**/*.{ts,tsx}', 'src/layouts/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        noDependeDe(
          [['@/api/*', '@/mocks/*'], 'La UI no llama a api/ ni a mocks/: usá un hook de @/hooks.'],
          [arriba.routes, 'La UI no depende de routes.'],
        ),
      ],
    },
  },
  {
    files: ['src/components/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        noDependeDe(
          [['@/api/*', '@/mocks/*'], 'La UI no llama a api/ ni a mocks/: usá un hook de @/hooks.'],
          [
            [...arriba.routes, ...arriba.pages, ...arriba.layouts],
            'components no depende de pages, layouts ni routes.',
          ],
        ),
      ],
    },
  },
  {
    files: ['src/hooks/**/*.{ts,tsx}', 'src/stores/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        noDependeDe(
          [
            [...arriba.routes, ...arriba.pages, ...arriba.layouts, '@/components/*'],
            'hooks y stores no dependen de la UI.',
          ],
          [['@/mocks/*'], 'hooks y stores no conocen mocks.'],
        ),
      ],
    },
  },
  {
    files: ['src/api/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        noDependeDe(
          [
            [
              ...arriba.routes,
              ...arriba.pages,
              ...arriba.layouts,
              '@/components/*',
              '@/hooks/*',
              '@/stores/*',
              '@/mocks/*',
            ],
            'api solo depende de types, lib y config/env.',
          ],
          [
            ['react', 'react-dom', '@tanstack/react-query', '@tanstack/react-router'],
            'api no importa React ni TanStack: es código de red puro.',
          ],
        ),
      ],
    },
  },
  {
    files: ['src/types/**/*.{ts,tsx}', 'src/lib/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        noDependeDe([
          [
            ...arriba.routes,
            ...arriba.pages,
            ...arriba.layouts,
            '@/components/*',
            '@/hooks/*',
            '@/stores/*',
            '@/api/*',
            '@/mocks/*',
          ],
          'types y lib no dependen de otras capas.',
        ]),
      ],
    },
  },
  {
    files: ['**/*.test.{ts,tsx}', 'src/test/**'],
    rules: { 'no-restricted-imports': 'off' },
  },
);
