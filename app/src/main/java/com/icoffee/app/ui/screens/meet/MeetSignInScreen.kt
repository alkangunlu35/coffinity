package com.icoffee.app.ui.screens.meet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.viewmodel.AuthViewModel

private val SignInTitleCream = Color(0xFFF5E6D3)
private val SignInSubtitleCream = Color(0xB3F5E6D3)
private val SignInFormBorder = Color(0x1FFFFFFF)
private val SignInErrorRed = Color(0xFFFF6B6B)

@Composable
fun MeetSignInScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignInSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    backgroundRes: Int = R.drawable.coffinity_bg
) {
    val context = LocalContext.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC1A0F0A), Color.Transparent, Color(0xCC1A0F0A))
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3AD08945), Color.Transparent),
                        radius = 920f
                    )
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp)
                .align(Alignment.TopStart)
                .background(Color(0x2D1A0F0A), RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.meet_back),
                tint = SignInTitleCream
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            Text(
                text = stringResource(R.string.meet_signin_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = SignInTitleCream,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.meet_signin_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = SignInSubtitleCream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = Color(0x4D140C08),
                        spotColor = Color(0x56140C08)
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x352A1A12), Color(0x2B1A100B))
                        )
                    )
                    .border(1.dp, SignInFormBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column {
                    SignInUnderlineField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationError = null
                        },
                        placeholder = stringResource(R.string.meet_signup_email),
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )
                    SignInUnderlineField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        placeholder = stringResource(R.string.meet_signup_password),
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        showDividerAfter = false
                    )
                }
            }

            val displayError = validationError ?: authViewModel.errorMessage
            if (displayError != null) {
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodySmall,
                    color = SignInErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val emailErrorText = stringResource(R.string.meet_signup_error_email)
            val passwordErrorText = stringResource(R.string.meet_signin_error_password)
            SignInActionButton(
                text = stringResource(R.string.meet_signin_cta),
                enabled = !authViewModel.isLoading,
                onClick = {
                    validationError = when {
                        email.isBlank() || !email.contains("@") -> emailErrorText
                        password.isBlank() -> passwordErrorText
                        else -> null
                    }
                    if (validationError == null) {
                        authViewModel.signIn(
                            email = email.trim(),
                            password = password,
                            context = context,
                            onSuccess = onSignInSuccess
                        )
                    }
                }
            )

            Text(
                text = stringResource(R.string.meet_signin_create_account),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xB3D6BFA7),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCreateAccount
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (authViewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE69A3A))
            }
        }
    }
}

@Composable
private fun SignInUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    showDividerAfter: Boolean = true
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val visualTransformation = if (isPassword && !passwordVisible) {
        PasswordVisualTransformation()
    } else {
        VisualTransformation.None
    }
    val effectiveKeyboardType = if (isPassword) KeyboardType.Password else keyboardType

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xCFF5E6D3),
                modifier = Modifier.size(18.dp)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SignInTitleCream),
                cursorBrush = SolidColor(SignInTitleCream),
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = effectiveKeyboardType),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0x99D6BFA7)
                        )
                    }
                    innerTextField()
                }
            )
            if (isPassword) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0x88D6BFA7),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { passwordVisible = !passwordVisible }
                        )
                )
            }
        }
        if (showDividerAfter) {
            HorizontalDivider(color = Color(0x33F5E6D3), thickness = 0.8.dp)
        }
    }
}

@Composable
private fun SignInActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = if (enabled) 0.985f else 1f,
                pressedAlpha = if (enabled) 0.96f else 1f
            )
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(50.dp),
                clip = false,
                ambientColor = Color(0x66E69A3A),
                spotColor = Color(0x5CE69A3A)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(Color(0xFFB66A2C), Color(0xFFE69A3A)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF7A4A20), Color(0xFF9A6525)))
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
