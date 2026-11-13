import { spawn } from 'node:child_process';
const proceso = spawn(
  process.execPath,
  [
    'node_modules/vite/bin/vite.js',
    '--mode',
    process.argv.includes('--demo') ? 'demo' : 'development',
    '--host',
    '0.0.0.0',
    '--port',
    '5174',
  ],
  {
    stdio: 'inherit',
    env: { ...process.env, ENSAMBLA_HTTPS: '1' },
  },
);
proceso.on('exit', (codigo) => {
  process.exitCode = codigo ?? 1;
});
