package com.neawstreamtv.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.media3.common.util.UnstableApi
import androidx.compose.ui.Modifier // Para escalar la superficie
import androidx.compose.foundation.layout.fillMaxSize // Para ocupar toda la pantalla
import androidx.compose.ui.graphics.RectangleShape // Para la forma rectangular de la TV
import androidx.tv.material3.Surface // Superficie nativa de televisión
import androidx.tv.material3.ExperimentalTvMaterial3Api // <--- IMPORTADO: Para habilitar el uso de APIs experimentales de TV
import com.neawstreamtv.tv.ui.theme.NeawStreamTVTheme // Tu tema nativo de Android Studio

@UnstableApi
class MainActivity : ComponentActivity() {
    
    @OptIn(ExperimentalTvMaterial3Api::class) // <--- DECLARADO: Permite usar Surface y NeawStreamTVTheme sin errores de advertencia
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Envolvemos todo en tu tema nativo para forzar el Modo Oscuro
            NeawStreamTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    // Estados para controlar la navegación en vivo
                    var activeScreen by remember { mutableStateOf("home") }
                    var selectedList by remember { mutableStateOf<List<Canal>>(emptyList()) }
                    var selectedIndex by remember { mutableStateOf(0) }

                    when (activeScreen) {
                        // A. Mostramos la pantalla principal
                        "home" -> HomeScreen(
                            onPlayChannel = { lista, indice ->
                                selectedList = lista
                                selectedIndex = indice
                                activeScreen = "player" // Navegar al reproductor de video
                            }
                        )
                        // B. Mostramos el reproductor a pantalla completa
                        "player" -> PlayerScreen(
                            listaCanales = selectedList,
                            indiceInicial = selectedIndex,
                            onBack = {
                                activeScreen = "home" // Volver atrás a la pantalla de canales
                            }
                        )
                    }
                }
            }
        }
    }
}