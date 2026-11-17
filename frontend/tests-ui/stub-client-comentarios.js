// Stub del cliente API para la prueba UI de comentarios: envío
// controlado por la prueba mediante globalThis.__stubFallo.
export async function enviarComentario(payload) {
  if (globalThis.__stubFallo) {
    const err = new Error('fallo simulado')
    err.handledMessage = 'No pudimos registrar tu comentario. Intenta de nuevo.'
    throw err
  }
  return {
    id: 1,
    tipo: payload.tipo,
    temaDeclarado: payload.temaDeclarado,
    contenido: payload.contenido.trim(),
    creadoEn: new Date().toISOString()
  }
}

export async function fetchMisComentarios() { return [] }

export default {}
