package com.neawstreamtv.tv

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

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
    
    // Disparador para refrescar el reproductor en caso de errores de red persistentes
    var playerRefreshTrigger by remember { mutableStateOf(0) }

    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val exoPlayer = remember(playerRefreshTrigger) {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // Tiempos de conexión más agresivos
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)

        val cacheDir = File(context.cacheDir, "media_cache")
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
        val databaseProvider = StandaloneDatabaseProvider(context)
        val simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Búfer optimizado para inicio rápido (empieza a reproducir en menos de 1 segundo)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 50000,
                /* bufferForPlaybackMs = */ 1500, // Menor tiempo para arrancar rápido
                /* bufferForPlaybackAfterRebufferMs = */ 3000
            )
            .setBackBuffer(10000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        ExoPlayer.Builder(context, renderersFactory)
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
                    Player.STATE_READY -> {
                        isLoading = false
                        cambiandoCanal = false
                    }
                    else -> {}
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
                // Si el stream falla de inmediato, forzamos un refresh limpio
                playerRefreshTrigger++
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
        val mediaItem = MediaItem.Builder()
            .setUri(canal.url)
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .build()
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(isLoading, indiceActual, playerRefreshTrigger) {
        if (isLoading) {
            delay(4000) // 4 segundos exactos, ideal para TV
            if (isLoading && !exoPlayer.isPlaying) {
                // Primer intento rápido: re-preparar la fuente sin recrear todo el player
                exoPlayer.prepare()
                exoPlayer.play()
                
                // Si pasa otro segundo más y sigue trabado, ejecutamos un reset completo de sockets
                delay(1500)
                if (isLoading && !exoPlayer.isPlaying) {
                    playerRefreshTrigger++
                }
            }
        }
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
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
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
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
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