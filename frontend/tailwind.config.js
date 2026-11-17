/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Paleta "Senderos de Aragua" — verde selva + tierra + acento dorado
        leaf: {
          50: '#f3f9f1', 100: '#e3f1de', 200: '#c8e3c0', 300: '#a1cd95',
          400: '#72b066', 500: '#4f9448', 600: '#3a7a37', 700: '#2f612d',
          800: '#284d27', 900: '#214021', 950: '#10240f'
        },
        earth: {
          50: '#faf6ef', 100: '#f2e9d9', 200: '#e4d3b3', 300: '#d3b783',
          400: '#c69c5b', 500: '#bd8843', 600: '#a4713a', 700: '#855a33',
          800: '#6e4a2e', 900: '#5c3e29', 950: '#322015'
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['"Bricolage Grotesque"', 'Inter', 'system-ui', 'sans-serif']
      },
      boxShadow: {
        card: '0 1px 2px rgba(16,36,15,.05), 0 4px 16px rgba(16,36,15,.08)'
      },
      keyframes: {
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(.8)' },
          '70%': { transform: 'scale(1.05)' },
          '100%': { opacity: '1', transform: 'scale(1)' }
        },
        'float-slow': {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-12px)' }
        }
      },
      animation: {
        'fade-up': 'fade-up .45s ease-out both',
        'scale-in': 'scale-in .4s cubic-bezier(.2,.9,.3,1.3) both',
        'float-slow': 'float-slow 8s ease-in-out infinite'
      }
    }
  },
  plugins: []
}
