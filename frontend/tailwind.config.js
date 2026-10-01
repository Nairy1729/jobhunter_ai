/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        dark: {
          950: '#07080a',
          900: '#0c0e12',
          850: '#11141a',
          800: '#181b22',
          750: '#20242e',
          700: '#2b313d',
        },
        ink: {
          primary: '#edeef0',
          secondary: '#9ba1ad',
          muted: '#5e6573',
          faint: '#393e49',
        },
        signal: {
          emerald: '#00f59b',
          cyan: '#00e1ff',
          amber: '#f59e0b',
          rose: '#f43f5e',
        },
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
      },
      letterSpacing: {
        widest: '.2em',
        tightest: '-.04em',
      },
      animation: {
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'drift': 'drift 10s ease-in-out infinite alternate',
      },
      keyframes: {
        drift: {
          '0%': { transform: 'translateY(0px)' },
          '100%': { transform: 'translateY(-10px)' },
        }
      }
    },
  },
  plugins: [],
}
