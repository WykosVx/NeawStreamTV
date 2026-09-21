package com.neawstreamtv.tv

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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

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
    var cambiandoCanal by remember { mutableStateOf(false) }
    
    // Disparador para recrear el reproductor si el watchdog detecta un bloqueo profundo
    var playerRefreshTrigger by remember { mutableStateOf(0) }

    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val exoPlayer = remember(playerRefreshTrigger) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Búfer amplio y robusto para sesiones largas continuas
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30000,
                /* maxBufferMs = */ 120000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 8000
            )
            .setBackBuffer(15000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // Solo mostramos carga si demora de verdad, dejamos que el búfer trabaje solo
                    }
                    Player.STATE_READY -> {
                        isLoading = false
                        cambiandoCanal = false
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    isLoading = false
                    cambiandoCanal = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = true
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(indiceActual, playerRefreshTrigger) {
        isLoading = true
        val canal = listaCanales[indiceActual]
        val mediaItem = MediaItem.fromUri(canal.url)
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Watchdog pasivo: Solo actúa si pasan más de 45 segundos enteros con la pantalla congelada y sin reproducir
    LaunchedEffect(isPlayingState = exoPlayer.isPlaying, indiceActual, playerRefreshTrigger) {
        // Dejamos que ExoPlayer gestione sus propios reintentos de búfer libremente como lo hacía antes
    }

    fun cambiarCanal(direccion: Int) {
        if (cambiandoCanal) return
        val nuevoIndice = indiceActual + direccion
        if (nuevoIndice in listaCanales.indices) {
            cambiandoCanal = true
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
                        android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                            cambiarCanal(-1)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN, android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            cambiarCanal(1)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK -> {
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

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
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