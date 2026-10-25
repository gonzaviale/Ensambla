import { Generator, getConfig } from '@tanstack/router-generator';
const root = process.cwd();
const config = getConfig({ target: 'react', autoCodeSplitting: true }, root);
const gen = new Generator({ config, root });
await gen.run();
console.log('generado');
