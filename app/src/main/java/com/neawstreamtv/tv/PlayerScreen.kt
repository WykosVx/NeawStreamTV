package com.neawstreamtv.tv

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    listaCanales: List<Canal>,
    indiceInicial: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var indiceActual by remember { mutableStateOf(indiceInicial) }
    var isLoading by remember { mutableStateOf(true) }

    // Bypass del User-Agent
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val dataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent(userAgent)
    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    // OPTIMIZACIÓN DE BUFFER PARA TV (IPTV / En vivo)
    // Esto fuerza a tener un buffer saludable para evitar microcortes constantes
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 15000,      // Mínimo buffer antes de arrancar/reanudar (15 segundos)
            /* maxBufferMs = */ 50000,      // Máximo buffer acumulado (50 segundos)
            /* bufferForPlaybackMs = */ 2500, // Cuánto buffer necesita para empezar a reproducir rápido (2.5s)
            /* bufferForPlaybackAfterRebufferMs = */ 5000 // Buffer tras un corte antes de reanudar (5s)
        )
        .build()

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }

    // Listener real para detectar el estado del reproductor y quitar el loader de forma inteligente
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> isLoading = false
                    Player.STATE_ENDED -> {}
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Si ya está reproduciendo, aseguramos que el loader se oculte
                if (isPlaying) isLoading = false
            }
        }
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(indiceActual) {
        isLoading = true
        val canal = listaCanales[indiceActual]
        val mediaItem = MediaItem.fromUri(canal.url)
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun cambiarCanal(direccion: Int) {
        val nuevoIndice = indiceActual + direccion
        if (nuevoIndice in listaCanales.indices) {
            indiceActual = nuevoIndice
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                            cambiarCanal(-1)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            cambiarCanal(1)
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            }
        )

        // Pantalla de carga sincronizada con el estado real del reproductor
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)), // Fondo ligeramente oscuro para que no parpadee tan fuerte
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFBF00))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Cargando: ${listaCanales[indiceActual].nombre}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}