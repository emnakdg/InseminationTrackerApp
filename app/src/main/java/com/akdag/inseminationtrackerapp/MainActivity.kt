package com.akdag.inseminationtrackerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.akdag.inseminationtrackerapp.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InseminationTrackerTheme {
                AuthFlow(onAuthSuccess = {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                })
            }
        }
    }
}

private enum class AuthScreen { SPLASH, LOGIN, REGISTER }

@Composable
private fun AuthFlow(onAuthSuccess: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var screen by remember { mutableStateOf(AuthScreen.SPLASH) }

    LaunchedEffect(Unit) {
        delay(1800)
        if (auth.currentUser != null) onAuthSuccess()
        else screen = AuthScreen.LOGIN
    }

    Box(Modifier.fillMaxSize().background(Bg0)) {
        when (screen) {
            AuthScreen.SPLASH -> SplashContent()
            AuthScreen.LOGIN -> LoginContent(auth, onAuthSuccess, onGoRegister = { screen = AuthScreen.REGISTER })
            AuthScreen.REGISTER -> RegisterContent(auth, onBack = { screen = AuthScreen.LOGIN }, onSuccess = { screen = AuthScreen.LOGIN })
        }
    }
}

@Composable
private fun SplashContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(Brush.linearGradient(listOf(GreenDim, GreenPrimary)), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🐄", fontSize = 44.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("İnek Takip", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text("Sürü yönetimi, kolaylaştı", fontSize = 14.sp, color = TextMid)
            }
        }
    }
}

@Composable
private fun LoginContent(auth: FirebaseAuth, onSuccess: () -> Unit, onGoRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }

    fun signInWithGoogle() {
        loading = true; error = ""
        val credentialManager = CredentialManager.create(context)
        val googleSignInOption = GetSignInWithGoogleOption.Builder(
            context.getString(R.string.default_web_client_id)
        ).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleSignInOption)
            .build()
        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user ?: run { loading = false; onSuccess(); return@addOnSuccessListener }
                            db.collection("Users").document(user.uid).get()
                                .addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        db.collection("Users").document(user.uid).set(
                                            mapOf(
                                                "uid" to user.uid,
                                                "name" to (user.displayName ?: ""),
                                                "farm_name" to (user.displayName ?: ""),
                                                "email" to (user.email ?: "")
                                            )
                                        ).addOnCompleteListener { loading = false; onSuccess() }
                                    } else {
                                        loading = false; onSuccess()
                                    }
                                }
                                .addOnFailureListener { loading = false; onSuccess() }
                        }
                        .addOnFailureListener { loading = false; error = it.localizedMessage ?: "Giriş başarısız." }
                } else {
                    loading = false; error = "Google girişi başarısız."
                }
            } catch (e: GetCredentialCancellationException) {
                loading = false
            } catch (e: GetCredentialException) {
                loading = false; error = e.message ?: e::class.simpleName ?: "Google girişi başarısız."
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(Brush.linearGradient(listOf(GreenDim, GreenPrimary)), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) { Text("🐄", fontSize = 34.sp) }
        Spacer(Modifier.height(16.dp))
        Text("İnek Takip", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("Hesabına giriş yap", fontSize = 13.sp, color = TextMid)
        Spacer(Modifier.height(36.dp))

        AuthTextField("E-POSTA", email, { email = it }, KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        AuthTextField(
            "ŞİFRE", password, { password = it },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton({ passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null, tint = TextMid
                    )
                }
            }
        )
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = RedAccent, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) { error = "Tüm alanları doldurun."; return@Button }
                loading = true; error = ""
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { loading = false; onSuccess() }
                    .addOnFailureListener { loading = false; error = "E-posta veya şifre hatalı." }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Bg0)
        ) { Text(if (loading) "Giriş yapılıyor…" else "Giriş Yap", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = BorderColor)
            Text("  veya  ", color = TextDim, fontSize = 12.sp)
            HorizontalDivider(Modifier.weight(1f), color = BorderColor)
        }
        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { signInWithGoogle() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Text("G", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary, modifier = Modifier.padding(end = 10.dp))
            Text("Google ile Devam Et", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
        Row {
            Text("Hesabın yok mu? ", color = TextMid, fontSize = 14.sp)
            Text(
                "Kayıt Ol", color = GreenPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onGoRegister() }
            )
        }
    }
}

@Composable
private fun RegisterContent(auth: FirebaseAuth, onBack: () -> Unit, onSuccess: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var name by remember { mutableStateOf("") }
    var farm by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg0)) {
        AppTopBar("Hesap Oluştur", onBack = onBack)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthTextField("AD SOYAD", name, { name = it })
            AuthTextField("ÇİFTLİK ADI (opsiyonel)", farm, { farm = it })
            AuthTextField("E-POSTA", email, { email = it }, KeyboardType.Email)
            AuthTextField("ŞİFRE (min. 6 karakter)", password, { password = it },
                visualTransformation = PasswordVisualTransformation())
            if (error.isNotEmpty()) Text(error, color = RedAccent, fontSize = 12.sp)
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        error = "Ad, e-posta ve şifre zorunludur."; return@Button
                    }
                    if (password.length < 6) { error = "Şifre en az 6 karakter olmalı."; return@Button }
                    loading = true; error = ""
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: ""
                            db.collection("Users").document(uid).set(
                                mapOf("uid" to uid, "name" to name, "farm_name" to farm.ifBlank { name }, "email" to email.trim())
                            ).addOnCompleteListener { loading = false; onSuccess() }
                        }
                        .addOnFailureListener { loading = false; error = it.localizedMessage ?: "Kayıt başarısız." }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Bg0)
            ) { Text(if (loading) "Oluşturuluyor…" else "Hesap Oluştur", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            label, color = TextMid, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary, unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = GreenPrimary, focusedContainerColor = Bg3, unfocusedContainerColor = Bg3,
            )
        )
    }
}
