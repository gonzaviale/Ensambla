import { importarSepa } from './lib/catalogoSepa.mjs';

const [archivo, fecha] = process.argv.slice(2);
if (!archivo || !fecha)
  throw new Error('Uso: npm run catalogo:sepa -- archivo.zip|productos.jsonl AAAA-MM-DD');
const resultado = await importarSepa({ archivo, fecha });
console.log(
  `SEPA: ${resultado.cantidad} códigos importados; ${resultado.omitidos} omitidos. Reiniciá Vite para cargar el índice.`,
);
