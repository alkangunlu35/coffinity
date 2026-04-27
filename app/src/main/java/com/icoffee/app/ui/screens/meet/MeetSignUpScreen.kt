package com.icoffee.app.ui.screens.meet

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.util.LegalLinks
import com.icoffee.app.viewmodel.AuthViewModel

private val TitleCream = Color(0xFFF5E6D3)
private val SubtitleCream = Color(0xB3F5E6D3)
private val FormBorder = Color(0x1FFFFFFF)
private val DividerTone = Color(0x2EF5E6D3)
private val ErrorRed = Color(0xFFFF6B6B)
private const val TagTerms = "terms"
private const val TagPrivacy = "privacy"

@Composable
fun MeetSignUpScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onJoinCommunity: () -> Unit,
    onSignIn: () -> Unit,
    backgroundRes: Int = R.drawable.coffinity_bg
) {
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            authViewModel.handleGoogleSignInResult(result.data, context) { onJoinCommunity() }
        }
    }

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Content
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
            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = stringResource(R.string.meet_signup_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = TitleCream,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.meet_signup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = SubtitleCream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                textAlign = TextAlign.Center
            )

            // Divider with coffee bean
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerTone)
                CoffeeBeanDividerIcon()
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerTone)
            }

            // Form card
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
                    .border(1.dp, FormBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    SignUpUnderlineField(
                        value = fullName,
                        onValueChange = { fullName = it; validationError = null },
                        placeholder = stringResource(R.string.meet_signup_name),
                        icon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text
                    )
                    SignUpUnderlineField(
                        value = email,
                        onValueChange = { email = it; validationError = null },
                        placeholder = stringResource(R.string.meet_signup_email),
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )
                    SignUpUnderlineField(
                        value = password,
                        onValueChange = { password = it; validationError = null },
                        placeholder = stringResource(R.string.meet_signup_password),
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )
                    SignUpUnderlineField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; validationError = null },
                        placeholder = stringResource(R.string.meet_signup_confirm_password),
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        showDividerAfter = false
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Sign-In
                    SocialAuthButton(
                        text = stringResource(R.string.meet_signup_google),
                        logo = { GoogleLogoMark() },
                        onClick = {
                            val intent = authViewModel.getGoogleSignInIntent(context)
                            googleSignInLauncher.launch(intent)
                        }
                    )
                }
            }

            // Validation / auth error
            val displayError = validationError ?: authViewModel.errorMessage
            if (displayError != null) {
                Text(
                    text = displayError,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Benefit list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BenefitRow(text = stringResource(R.string.meet_signup_benefit_1))
                BenefitRow(text = stringResource(R.string.meet_signup_benefit_2))
                BenefitRow(text = stringResource(R.string.meet_signup_benefit_3))
            }

            // Join the Community button
            val nameErrorText = stringResource(R.string.meet_signup_error_name)
            val emailErrorText = stringResource(R.string.meet_signup_error_email)
            val passwordErrorText = stringResource(R.string.meet_signup_error_password)
            val passwordMismatchErrorText = stringResource(R.string.meet_signup_error_password_mismatch)
            JoinCommunityButton(
                text = stringResource(R.string.meet_signup_cta),
                enabled = !authViewModel.isLoading,
                onClick = {
                    validationError = validate(
                        fullName = fullName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        nameError = nameErrorText,
                        emailError = emailErrorText,
                        passwordError = passwordErrorText,
                        passwordMismatchError = passwordMismatchErrorText
                    )
                    if (validationError == null) {
                        authViewModel.signUp(fullName.trim(), email.trim(), password, context) {
                            onJoinCommunity()
                        }
                    }
                }
            )

            // Terms
            MeetLegalLinksText(modifier = Modifier.fillMaxWidth())

            // Sign in
            Text(
                text = stringResource(R.string.meet_signup_signin),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xE0D6BFA7),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignIn
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Loading overlay
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

private fun validate(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    nameError: String,
    emailError: String,
    passwordError: String,
    passwordMismatchError: String
): String? = when {
    fullName.isBlank() -> nameError
    email.isBlank() || !email.contains("@") -> emailError
    password.length < 6 -> passwordError
    password != confirmPassword -> passwordMismatchError
    else -> null
}

@Composable
private fun SignUpUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    showDividerAfter: Boolean = true
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val visualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation() else VisualTransformation.None
    val effectiveKeyboardType = if (isPassword) KeyboardType.Password else keyboardType

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xCFF5E6D3),
                modifier = Modifier.size(20.dp)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TitleCream,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                cursorBrush = SolidColor(TitleCream),
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = effectiveKeyboardType),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = Color(0x99D6BFA7)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (isPassword) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0x88D6BFA7),
                    modifier = Modifier
                        .size(20.dp)
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
private fun SocialAuthButton(
    text: String,
    logo: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.985f,
                pressedAlpha = 0.95f
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x5E1C120D), Color(0x4D140C08))
                )
            )
            .border(1.dp, Color(0x52FFFFFF), RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            logo()
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TitleCream.copy(alpha = if (pressed) 0.88f else 0.98f)
            )
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "•", color = Color(0xE8D6BFA7), style = MaterialTheme.typography.bodyMedium)
        Text(text = text, color = Color(0xE8D6BFA7), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun JoinCommunityButton(
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
                if (enabled) Brush.horizontalGradient(
                    listOf(Color(0xFFB66A2C), Color(0xFFE69A3A))
                ) else Brush.horizontalGradient(
                    listOf(Color(0xFF7A4A20), Color(0xFF9A6525))
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x52FFE7C8), Color.Transparent, Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CoffeeBeanDividerIcon() {
    Canvas(modifier = Modifier.size(16.dp)) {
        drawOval(color = Color(0x88E7C8A7), topLeft = Offset(1f, 2f), size = Size(13f, 12f))
        drawLine(
            color = Color(0xB65A3A28),
            start = Offset(7.5f, 3f),
            end = Offset(7.5f, 13f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MeetLegalLinksText(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val languageCode = AppLocaleManager.currentLanguage(context).code
    val isTurkish = LegalLinks.normalizeLanguageCode(languageCode) == "tr"
    val termsUrl = LegalLinks.termsUrl(languageCode)
    val privacyUrl = LegalLinks.privacyUrl(languageCode)

    val termsLabel = if (isTurkish) "Koşullar" else "Terms"
    val privacyLabel = if (isTurkish) "Gizlilik Politikası" else "Privacy Policy"
    val fullText = if (isTurkish) {
        "Devam ederek Koşullar ve Gizlilik Politikası'nı kabul etmiş olursun"
    } else {
        "By continuing, you agree to the Terms and Privacy Policy"
    }

    val annotated = buildAnnotatedString {
        append(fullText)

        val termsStart = fullText.indexOf(termsLabel)
        if (termsStart >= 0) {
            val termsEnd = termsStart + termsLabel.length
            addStyle(
                style = SpanStyle(
                    color = Color(0xFFF5E6D3),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                start = termsStart,
                end = termsEnd
            )
            addStringAnnotation(
                tag = TagTerms,
                annotation = termsUrl,
                start = termsStart,
                end = termsEnd
            )
        }

        val privacyStart = fullText.indexOf(privacyLabel)
        if (privacyStart >= 0) {
            val privacyEnd = privacyStart + privacyLabel.length
            addStyle(
                style = SpanStyle(
                    color = Color(0xFFF5E6D3),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                start = privacyStart,
                end = privacyEnd
            )
            addStringAnnotation(
                tag = TagPrivacy,
                annotation = privacyUrl,
                start = privacyStart,
                end = privacyEnd
            )
        }
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall.copy(
            color = Color(0xE0D6BFA7),
            textAlign = TextAlign.Center
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = TagTerms, start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item); return@ClickableText }
            annotated.getStringAnnotations(tag = TagPrivacy, start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}

@Composable
private fun GoogleLogoMark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val strokeWidth = size.minDimension * 0.22f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )

            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 45f,
                sweepAngle = 112f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 157f,
                sweepAngle = 82f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style
            )
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 239f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style
            )
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = 327f,
                sweepAngle = 102f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.52f, size.height * 0.5f),
                end = Offset(size.width * 0.9f, size.height * 0.5f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
