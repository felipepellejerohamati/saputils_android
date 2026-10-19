package com.serviall.saputils.ui.login

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import com.serviall.saputils.ui.theme.SaputilsTheme
import com.serviall.saputils.ui.theme.ServialRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.serviall.saputils.R;
import com.serviall.saputils.apicalls.ServicioAPI
import com.serviall.saputils.utils.get64BaseString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passwordFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isLargeScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val handleLogin: () -> Unit = {
        errorMessage = null
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Por favor ingrese usuario y contraseña."
        } else if (password.length < 4) {
            errorMessage = "La contraseña debe tener al menos 4 caracteres."
        } else {
            isLoading = true
            coroutineScope.launch {
                // Simulate network authentication
                delay(1000L)
                isLoading = false
//                if (username.equals("admin", ignoreCase = true) || username.contains("@") || username.length >= 3) {
//                    onLoginSuccess(username)
//                } else {
//                    errorMessage = "Credenciales inválidas. Intente nuevamente."
//                }
                val servicioAPI = ServicioAPI.getInstance();
                val base64String = "Basic " + get64BaseString("serviall:8400Abn.,")
                val validaUser = servicioAPI.login(base64String,username, password);
                if(validaUser=="")
                    onLoginSuccess(username)
                else
                    errorMessage = validaUser;
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .then(if (isLargeScreen) Modifier.width(460.dp) else Modifier.fillMaxWidth(0.9f))
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Branding Logo / Icon
//                    Surface(
//                        modifier = Modifier
//                            .size(76.dp)
//                            .clip(RoundedCornerShape(20.dp)),
//                        color = ServialRed,
//                        tonalElevation = 4.dp
//                    ) {
//                        Box(contentAlignment = Alignment.Center) {
//                            Text(
//                                text = "SV",
//                                color = Color.White,
//                                fontSize = 30.sp,
//                                fontWeight = FontWeight.Bold,
//                                letterSpacing = 2.sp
//                            )
//                        }
//                    }

                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.serviall_isotipo_rojo),
                            contentDescription = "Compose Multiplatform logo",
                            modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

//                    Text(
//                        text = "SERVIALL",
//                        fontSize = 28.sp,
//                        fontWeight = FontWeight.ExtraBold,
//                        color = MaterialTheme.colorScheme.onSurface,
//                        letterSpacing = 3.sp
//                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Utilidades SAP y Operaciones de Campo",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Error Message Banner
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario o ID") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Username Icon",
                                tint = ServialRed
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ServialRed,
                            cursorColor = ServialRed
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Password Icon",
                                tint = ServialRed
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Rounded.Visibility
                            else
                                Icons.Rounded.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleLogin() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ServialRed,
                            cursorColor = ServialRed
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Login Button
                    Button(
                        onClick = { handleLogin() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ServialRed,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "INICIAR SESIÓN",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Versión 1.0 • Serviall 2026",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(name = "Login Light", showBackground = true)
@Composable
fun LoginScreenLightPreview() {
    SaputilsTheme(darkTheme = false) {
        LoginScreen(onLoginSuccess = {})
    }
}

@Preview(name = "Login Dark", showBackground = true)
@Composable
fun LoginScreenDarkPreview() {
    SaputilsTheme(darkTheme = true) {
        LoginScreen(onLoginSuccess = {})
    }
}
