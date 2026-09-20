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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.*

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
    
    // Control para evitar doble pulsación rápida en la TV que sature la memoria
    var cambiandoCanal by remember { mutableStateOf(false) }

    // Cargar animación Lottie desde assets de forma segura
    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("animations/wykos_animation.json")
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = LottieConstants.IterateForever
    )

    // Bypass del User-Agent y tiempos de espera robustos
    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setConnectTimeoutMs(20000)
        .setReadTimeoutMs(20000)

    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

    // ESTABILIZACIÓN MÁXIMA DEL BÚFER PARA IPTV / EN VIVO
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 45000,              // Sube el mínimo a 45 segundos para un colchón masivo
            /* maxBufferMs = */ 180000,             // Permite acumular hasta 3 minutos de búfer en RAM
            /* bufferForPlaybackMs = */ 2500,       // Inicia rápido con solo 2.5s iniciales
            /* bufferForPlaybackAfterRebufferMs = */ 10000 // Exige 10s seguros tras un corte antes de reanudar
        )
        .setBackBuffer(15000, true) // Memoria retroactiva para tolerar microcortes de red
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_READY -> {
                        isLoading = false
                        cambiandoCanal = false
                    }
                    Player.STATE_ENDED -> {}
                    Player.STATE_IDLE -> {}
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
                // Auto-recuperación ante caídas de stream en vivo
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

    LaunchedEffect(indiceActual) {
        isLoading = true
        val canal = listaCanales[indiceActual]
        val mediaItem = MediaItem.fromUri(canal.url)
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun cambiarCanal(direccion: Int) {
        if (cambiandoCanal) return // Ignoramos si ya está procesando un cambio para no saturar la TV

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

        // Pantalla de carga sincronizada con el estado real del reproductor y animación Lottie
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
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Animación Lottie integrada de forma nativa
                    if (compositionResult.value != null) {
                        LottieAnimation(
                            composition = compositionResult.value,
                            progress = { lottieProgress },
                            modifier = Modifier
                                .width(120.dp)
                                .height(70.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

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