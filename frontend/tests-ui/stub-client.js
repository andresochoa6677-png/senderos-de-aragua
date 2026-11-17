// Stub del cliente API para la auditoría UI: mismas funciones que
// client.js, pero contra el backend REAL con sesión pre-autenticada.
const BASE = 'http://localhost:8080'
const COOKIE = process.env.AUDIT_COOKIE || ''
const XSRF = process.env.AUDIT_XSRF || ''

async function call(method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      Cookie: COOKIE,
      'X-XSRF-TOKEN': XSRF,
      'Content-Type': 'application/json'
    },
    body: body ? JSON.stringify(body) : undefined
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    const err = new Error(data.error || 'Error')
    err.handledMessage = data.error || 'Ocurrió un error inesperado.'
    err.status = res.status
    throw err
  }
  return data
}

export async function fetchLecciones() { return call('GET', '/api/lecciones') }
export async function fetchEjercicios(leccionId) { return call('GET', `/api/ejercicios/leccion/${leccionId}`) }
export async function responderEjercicio(ejercicioId, respuesta) {
  return call('POST', `/api/ejercicios/${ejercicioId}/responder`, { respuesta })
}
export default {}
