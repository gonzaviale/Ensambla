import { descargarSepa } from './lib/catalogoSepa.mjs';

try {
  const resultado = await descargarSepa();
  console.log(
    `SEPA: ${resultado.cantidad} códigos del ${resultado.fecha}. SHA-256: ${resultado.sha256}. Reiniciá Vite para cargar el índice.`,
  );
} catch (error) {
  console.error(error instanceof Error ? error.message : 'No pudimos descargar SEPA.');
  process.exitCode = 1;
}
