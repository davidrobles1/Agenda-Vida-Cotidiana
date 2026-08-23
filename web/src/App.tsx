import { AppRouter } from './routes/AppRouter'
import { ModeProvider } from './core/user/ModeContext'
import { VisualThemeProvider } from './core/theme/VisualThemeContext'

function App() {
  return (
    <ModeProvider>
      <VisualThemeProvider>
        <AppRouter />
      </VisualThemeProvider>
    </ModeProvider>
  )
}

export default App
