import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import './index.css'

/**
 * Red de seguridad: un error de render JAMÁS debe dejar la pantalla en
 * blanco. Muestra el mensaje técnico para que sea reportable y un botón
 * de recarga que conserva la ruta actual.
 */
class FronteraDeError extends React.Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', padding: 24, fontFamily: 'system-ui, sans-serif' }}>
          <div style={{ maxWidth: 560, textAlign: 'center' }}>
            <h1 style={{ fontSize: 22, fontWeight: 700, color: '#214021' }}>
              Algo salió mal en esta pantalla
            </h1>
            <p style={{ marginTop: 12, color: '#57534e', fontSize: 14 }}>
              No perdiste ningún dato. Recarga la página para continuar; si el
              problema persiste, avísanos y envíanos el mensaje técnico de abajo.
            </p>
            <button
              type="button"
              onClick={() => window.location.reload()}
              style={{ marginTop: 20, padding: '12px 24px', borderRadius: 16, border: 'none', background: '#3a7a37', color: '#fff', fontWeight: 600, cursor: 'pointer' }}
            >
              Recargar la página
            </button>
            <pre style={{ marginTop: 20, textAlign: 'left', fontSize: 12, color: '#78716c', background: '#f5f5f4', padding: 12, borderRadius: 12, overflow: 'auto', whiteSpace: 'pre-wrap' }}>
              {String(this.state.error && (this.state.error.stack || this.state.error.message || this.state.error))}
            </pre>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <FronteraDeError>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </FronteraDeError>
  </React.StrictMode>
)
