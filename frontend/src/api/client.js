import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  withCredentials: true
})

const unauthorizedHandlers = new Set()

export function onUnauthorized(handler) {
  unauthorizedHandlers.add(handler)
  return () => unauthorizedHandlers.delete(handler)
}

// CSRF: axios xsrfCookieName/xsrfHeaderName usan la cookie XSRF-TOKEN emitida por /api/auth/csrf
async function ensureCsrf() {
  if (document.cookie.includes('XSRF-TOKEN=')) return
  try {
    await api.get('/auth/csrf')
  } catch {
    /* la cookie se emitirá igualmente si el backend responde; se reintenta en el POST */
  }
}

api.interceptors.request.use(async (config) => {
  if (config.method !== 'get' && config.method !== 'head') {
    await ensureCsrf()
  }
  axios.defaults.withCredentials = true
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      unauthorizedHandlers.forEach((handler) => handler(error))
    }
    const data = error.response?.data
    const message =
      (data && (data.error || data.mensaje || data.message)) ||
      'Ocurrió un error inesperado. Intenta de nuevo.'
    error.handledMessage = message
    error.codigo = data?.codigo
    return Promise.reject(error)
  }
)

export async function login(email, password, recordar = false) {
  const { data } = await api.post('/auth/login', { email, password, recordar })
  return data
}

export async function fetchMe() {
  const { data } = await api.get('/auth/me')
  return data
}

export async function logout() {
  try {
    // El LogoutFilter de Spring Security atiende POST /api/auth/logout:
    // invalida la sesion y borra la cookie JSESSIONID (SecurityConfig).
    await api.post('/auth/logout')
  } catch {
    /* si el servidor no responde, el estado local se limpia igualmente */
  }
}

export async function registroVivienda(payload) {
  const { data } = await api.post('/onboarding/registro', payload)
  return data
}

export async function solicitarReset(email) {
  const { data } = await api.post('/password/solicitud', { email })
  return data
}

export async function restablecerPassword(token, nuevaPassword) {
  const { data } = await api.post('/password/restablecer', {
    token,
    nuevaPassword
  })
  return data
}

export async function fetchOnboardingEstado() {
  const { data } = await api.get('/onboarding/estado')
  return data
}

/** Documento de consentimiento vigente: { version, texto, hashTexto }. */
export async function fetchConsentimientoTexto() {
  const { data } = await api.get('/onboarding/consentimiento/texto')
  return data
}

/** Acepta el consentimiento: la versión y el hash los fija el servidor. */
export async function aceptarConsentimiento() {
  const { data } = await api.post('/onboarding/consentimiento')
  return data
}

/** Registra el rechazo (trazabilidad); el cierre de sesión lo hace la página. */
export async function rechazarConsentimiento() {
  await api.post('/onboarding/consentimiento/rechazo')
}

export async function fetchEncuestaInicial() {
  const { data } = await api.get('/onboarding/encuesta/INICIAL')
  return data
}

/** Estado de la ventana de posprueba para la vivienda. */
export async function fetchEncuestaFinalDisponible() {
  const { data } = await api.get('/encuesta-final/disponible')
  return data
}

/** Los 23 ítems de la encuesta FINAL (sin claves). */
export async function fetchEncuestaFinal() {
  const { data } = await api.get('/encuesta-final')
  return data
}

/** Guarda las 23 respuestas; retorna { mensaje, puntajeConocimiento }. */
export async function responderEncuestaFinal(respuestas) {
  const { data } = await api.post('/encuesta-final', { respuestas })
  return data
}

// ---------- Panel de administración ----------

export async function fetchAdminEstudio() {
  const { data } = await api.get('/admin/estudio')
  return data
}

export async function habilitarPosprueba() {
  const { data } = await api.post('/admin/estudio/posprueba/habilitar')
  return data
}

export async function deshabilitarPosprueba() {
  const { data } = await api.post('/admin/estudio/posprueba/deshabilitar')
  return data
}

export async function fetchAdminViviendas() {
  const { data } = await api.get('/admin/viviendas')
  return data
}

export async function fetchAdminSesiones() {
  const { data } = await api.get('/admin/sesiones')
  return data
}

export async function fetchAdminComentarios() {
  const { data } = await api.get('/admin/comentarios')
  return data
}

export async function responderEncuesta(encuestaId, respuestas) {
  const { data } = await api.post('/onboarding/encuesta', {
    encuestaId,
    respuestas
  })
  return data
}

export async function fetchLecciones() {
  const { data } = await api.get('/lecciones')
  return data
}

export async function fetchLeccion(id) {
  const { data } = await api.get(`/lecciones/${id}`)
  return data
}

export async function fetchProgreso() {
  const { data } = await api.get('/lecciones/progreso')
  return data
}

export async function fetchEjercicios(leccionId) {
  const { data } = await api.get(`/ejercicios/leccion/${leccionId}`)
  return data
}

export async function responderEjercicio(ejercicioId, respuesta) {
  const { data } = await api.post(`/ejercicios/${ejercicioId}/responder`, { respuesta })
  return data
}

export async function fetchPracticaItem(modo, excluir = []) {
  const { data } = await api.get('/practica/item', {
    params: { modo, excluir: excluir.join(',') }
  })
  return data
}

export async function responderPractica(itemId, respuesta) {
  const { data } = await api.post(`/practica/item/${itemId}/responder`, { respuesta })
  return data
}

export async function fetchPracticaResumen() {
  const { data } = await api.get('/practica/resumen')
  return data
}

export async function verificarCorreo(token) {
  const { data } = await api.post('/auth/verificar-correo', { token })
  return data
}

export async function reenviarVerificacion(email) {
  const { data } = await api.post('/auth/reenviar-verificacion', { email })
  return data
}

export async function enviarComentario({ tipo, temaDeclarado, contenido }) {
  const { data } = await api.post('/comentarios', { tipo, temaDeclarado, contenido })
  return data
}

export async function fetchMisComentarios() {
  const { data } = await api.get('/comentarios/mios')
  return data
}

export async function fetchConfigPublica() {
  const { data } = await api.get('/config')
  return data
}

export default api

// ---------- Exportaciones CSV (investigación) ----------

export async function exportarCsv(tipo) {
  const res = await api.get(`/admin/export/${tipo}`, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = `${tipo}-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(url)
}

/** Asigna (o limpia con null) la categoría temática de un comentario. */
export async function actualizarCategoriaComentario(id, categoria) {
  const { data } = await api.patch(`/admin/comentarios/${id}/categoria`, { categoriaTematica: categoria })
  return data
}
