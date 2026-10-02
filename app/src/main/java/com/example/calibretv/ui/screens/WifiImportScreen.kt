package com.example.calibretv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.server.QrCodeGenerator
import com.example.calibretv.data.server.WifiImportServer
import com.example.calibretv.theme.AmberWarm
import com.example.calibretv.theme.BackgroundDark
import com.example.calibretv.theme.CyanElectric
import com.example.calibretv.theme.SurfaceContainer
import com.example.calibretv.theme.TextMuted
import com.example.calibretv.theme.TextPrimary

@Composable
fun WifiImportScreen(
    repository: BookRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val wifiServer = remember { WifiImportServer(context, repository) }
    var isServerRunning by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val buttonFocusRequester = remember { FocusRequester() }

    BackHandler {
        wifiServer.stopServer()
        onBack()
    }

    LaunchedEffect(Unit) {
        val success = wifiServer.startServer()
        isServerRunning = success
        val ip = wifiServer.getLocalIpAddress()
        val port = wifiServer.activePort
        serverUrl = "http://$ip:$port"
        if (success) {
            qrBitmap = QrCodeGenerator.generateQrBitmap(serverUrl, 360, 360)
        }
        buttonFocusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            wifiServer.stopServer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Instructions & Toggle Button
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                            .clickable { onBack() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = AmberWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Importar por WiFi",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Dirección de tu Servidor en TV:",
                    color = TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isServerRunning) serverUrl else "Servidor Detenido",
                    color = if (isServerRunning) AmberWarm else Color.Gray,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                var isFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (isServerRunning) {
                            wifiServer.stopServer()
                            isServerRunning = false
                        } else {
                            val success = wifiServer.startServer()
                            isServerRunning = success
                            val ip = wifiServer.getLocalIpAddress()
                            val port = wifiServer.activePort
                            serverUrl = "http://$ip:$port"
                            if (success) {
                                qrBitmap = QrCodeGenerator.generateQrBitmap(serverUrl, 360, 360)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServerRunning) Color(0xFF00C853) else AmberWarm
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .focusRequester(buttonFocusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(
                            width = if (isFocused) 3.dp else 0.dp,
                            color = if (isFocused) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = if (isServerRunning) "Detener Servidor" else "Iniciar Servidor",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Para subir archivos, escanee el código QR o ingrese la URL anterior en un dispositivo en la misma red Wi-Fi.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // Vertical Separator
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.8f)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Right Column: QR Code & Server Status
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, AmberWarm, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null && isServerRunning) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Código QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth(0.8f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estado del Servidor:", color = TextMuted, fontSize = 15.sp)
                        Text(
                            text = if (isServerRunning) "Ejecutándose" else "Detenido",
                            color = if (isServerRunning) Color(0xFF00C853) else Color(0xFFD50000),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Clientes Conectados: ${wifiServer.connectedClientsCount}",
                        color = TextMuted,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Archivos Subidos: ${wifiServer.uploadedFilesCount}",
                        color = AmberWarm,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
