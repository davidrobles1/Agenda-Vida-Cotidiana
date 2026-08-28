import { AppRouter } from './routes/AppRouter'
import { ModeProvider } from './core/user/ModeContext'
import { VisualThemeProvider } from './core/theme/VisualThemeContext'
import { GlobalIconTooltip } from './core/ui/tooltip/GlobalIconTooltip'

function App() {
  return (
    <ModeProvider>
      <VisualThemeProvider>
        <AppRouter />
        {/* Pedido explícito del usuario (2026-08-23): tooltips para todo
            control que se muestre solo con icono, en TODOS los módulos.
            Se monta una sola vez aquí y escucha en `document` — ver su
            propio doc comment sobre por qué es global y no botón por
            botón. */}
        <GlobalIconTooltip />
      </VisualThemeProvider>
    </ModeProvider>
  )
}

export default App
