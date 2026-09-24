package com.neawstreamtv.tv

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtreamCodeScreen(
    onVolver: () -> Unit,
    onConectadoExitoso: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("NeawPrefs", Context.MODE_PRIVATE) }

    var servidor by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar datos previos si ya existían
    LaunchedEffect(Unit) {
        servidor = prefs.getString("xc_servidor", "") ?: ""
        usuario = prefs.getString("xc_usuario", "") ?: ""
        password = prefs.getString("xc_password", "") ?: ""
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize().background(ColorOscuroFondo),
            containerColor = ColorOscuroFondo,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Configuración Xtream Codes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = ColorSuperficieOscura),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ingrese los datos de su proveedor",
                            color = ColorDorado,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Campo Servidor / URL Base
                        OutlinedTextField(
                            value = servidor,
                            onValueChange = { servidor = it },
                            label = { Text("URL del Servidor (ej: http://tuservidor.com:8080)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorDorado,
                                unfocusedBorderColor = ColorBlancoAlpha10,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = ColorDorado,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Campo Usuario
                        OutlinedTextField(
                            value = usuario,
                            onValueChange = { usuario = it },
                            label = { Text("Usuario") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorDorado,
                                unfocusedBorderColor = ColorBlancoAlpha10,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = ColorDorado,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Campo Contraseña
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorDorado,
                                unfocusedBorderColor = ColorBlancoAlpha10,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = ColorDorado,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        if (mensajeError.isNotEmpty()) {
                            Text(
                                text = mensajeError,
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón de Conectar / Guardar
                        Button(
                            onClick = {
                                if (servidor.isBlank() || usuario.isBlank() || password.isBlank()) {
                                    mensajeError = "Por favor complete todos los campos."
                                    return@Button
                                }

                                isLoading = true
                                mensajeError = ""

                                scope.launch {
                                    try {
                                        var baseUrl = servidor.trim().removeSuffix("/")
                                        val urlFinal = if (baseUrl.contains("/get.php")) {
                                            if (baseUrl.contains("username=")) baseUrl 
                                            else "$baseUrl?username=$usuario&password=$password&type=m3u"
                                        } else {
                                            "$baseUrl/get.php?username=$usuario&password=$password&type=m3u"
                                        }

                                        prefs.edit().apply {
                                            putString("xc_servidor", servidor)
                                            putString("xc_usuario", usuario)
                                            putString("xc_password", password)
                                            putString("user_url", urlFinal)
                                            apply()
                                        }

                                        LogManager.add("Xtream Codes configurado correctamente.")
                                        isLoading = false
                                        onConectadoExitoso(urlFinal)
                                    } catch (e: Exception) {
                                        isLoading = false
                                        mensajeError = "Error al guardar: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorDorado),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Text("CONECTAR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}