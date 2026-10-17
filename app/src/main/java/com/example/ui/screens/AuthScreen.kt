package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.User
import com.example.ui.components.UrduRtlProvider
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: (User) -> Unit,
    onDemoLogin: (User) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Demo PIN state
    var showDemoSection by remember { mutableStateOf(false) }
    var demoPin by remember { mutableStateOf("") }
    var demoRole by remember { mutableStateOf("super_admin") }
    var demoGroup by remember { mutableStateOf("A") }

    UrduRtlProvider {
        Scaffold(
            containerColor = MainBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Logo
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryDarkGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "ایپ لوگو",
                                tint = AccentLime,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "روزانہ کارکنان ڈیوٹی لسٹ",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryDarkGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "اور کوسٹر بکنگ مینجمنٹ سسٹم",
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "براہ کرم سیکیورٹی اور حاضری کے ریکارڈ کے لیے لاگ ان کریں",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (errorMessage != null) {
                            Surface(
                                color = Color(0xFFFFDAD6),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Google Sign-In Button (Primary Production Authentication)
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                performGoogleSignIn(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = coroutineScope,
                                    onSuccess = { user ->
                                        isLoading = false
                                        onAuthSuccess(user)
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    },
                                    onCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_signin_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AccentLime
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "گوگل اکاؤنٹ سے لاگ ان کریں",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider(color = BorderColor)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Demonstration Environment Section (as prescribed in Section 5)
                        TextButton(
                            onClick = { showDemoSection = !showDemoSection },
                            modifier = Modifier.testTag("toggle_demo_button")
                        ) {
                            Text(
                                text = if (showDemoSection) "ڈیمو پینل چھپائیں" else "ٹیسٹنگ / ڈیمو موڈ (PIN: 3456)",
                                color = SecondaryGreen,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        if (showDemoSection) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LightMint.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "نمائشی ٹیسٹنگ ماحول (Demonstration)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryDarkGreen
                                    )
                                    Text(
                                        text = "مطلوبہ ٹیسٹنگ پن '3456' درج کریں:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = demoPin,
                                        onValueChange = { demoPin = it },
                                        label = { Text("ڈیمو سیکیورٹی پن") },
                                        placeholder = { Text("3456") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("demo_pin_input"),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Role Selection for Demo Testing
                                    Text(
                                        text = "کردار منتخب کریں:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        FilterChip(
                                            selected = demoRole == "super_admin",
                                            onClick = { demoRole = "super_admin"; demoGroup = "ALL" },
                                            label = { Text("سپر ایڈمن") }
                                        )
                                        FilterChip(
                                            selected = demoRole == "group_admin",
                                            onClick = { demoRole = "group_admin"; demoGroup = "A" },
                                            label = { Text("گروپ ایڈمن") }
                                        )
                                        FilterChip(
                                            selected = demoRole == "worker",
                                            onClick = { demoRole = "worker"; demoGroup = "A" },
                                            label = { Text("کارکن") }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            if (demoPin.trim() == "3456") {
                                                val demoUser = User(
                                                    userId = "demo_${demoRole}_101",
                                                    email = if (demoRole == "super_admin") "abdullahamjad347@gmail.com" else "worker@karkunan.pk",
                                                    displayName = when (demoRole) {
                                                        "super_admin" -> "عبداللہ امجد (سپر ایڈمن)"
                                                        "group_admin" -> "ایڈمنسٹریٹر (گروپ $demoGroup)"
                                                        else -> "کارکن ذیشان"
                                                    },
                                                    role = demoRole,
                                                    assignedGroup = demoGroup,
                                                    active = true
                                                )
                                                onDemoLogin(demoUser)
                                            } else {
                                                errorMessage = "غلط ڈیمو پن! درست پن 3456 ہے"
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("demo_login_button")
                                    ) {
                                        Text("ڈیمو لاگ ان کریں")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun performGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: (User) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onError("گوگل لاگ ان کنفیگریشن غائب ہے")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val isSuperAdminEmail = firebaseUser.email?.equals("abdullahamjad347@gmail.com", ignoreCase = true) == true
                    val role = if (isSuperAdminEmail) "super_admin" else "worker"
                    val group = if (isSuperAdminEmail) "ALL" else "A"

                    val user = User(
                        userId = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "کارکن",
                        role = role,
                        assignedGroup = group,
                        active = true
                    )
                    onSuccess(user)
                } else {
                    onError("صارف کی تفصیلات حاصل نہ ہو سکیں")
                }
            } else {
                onError("غیر متوقع سرٹیفکیٹ کی قسم")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}")
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "لاگ ان عمل میں خرابی پیش آگئی")
        }
    }
}
