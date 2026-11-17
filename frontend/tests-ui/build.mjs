// Empaqueta la prueba UI con esbuild, sustituyendo el cliente API por el
// stub autenticado contra el backend real.
import esbuild from '../node_modules/esbuild/lib/main.js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const aqui = path.dirname(fileURLToPath(import.meta.url))

await esbuild.build({
  entryPoints: [path.join(aqui, 'l1-ui.mjs')],
  bundle: true,
  platform: 'node',
  format: 'esm',
  outfile: path.join(aqui, 'l1-ui.bundle.mjs'),
  external: ['happy-dom'],
  jsx: 'automatic',
  loader: { '.js': 'jsx', '.jsx': 'jsx', '.mjs': 'jsx' },
  banner: { js: "import { createRequire } from 'module'; const require = createRequire(import.meta.url);" },
  plugins: [{
    name: 'stub-client',
    setup(build) {
      build.onResolve({ filter: /api\/client\.js$/ }, () => ({
        path: path.join(aqui, 'stub-client.js')
      }))
    }
  }]
})
console.log('bundle listo')
