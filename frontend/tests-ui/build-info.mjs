// Empaqueta la prueba UI del módulo de Información con esbuild.
import esbuild from '../node_modules/esbuild/lib/main.js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const aqui = path.dirname(fileURLToPath(import.meta.url))

await esbuild.build({
  entryPoints: [path.join(aqui, 'info-ui.mjs')],
  bundle: true,
  platform: 'node',
  format: 'esm',
  outfile: path.join(aqui, 'info-ui.bundle.mjs'),
  external: ['happy-dom'],
  jsx: 'automatic',
  loader: { '.js': 'jsx', '.jsx': 'jsx', '.mjs': 'jsx' },
  banner: { js: "import { createRequire } from 'module'; const require = createRequire(import.meta.url);" }
})
console.log('bundle ok')
