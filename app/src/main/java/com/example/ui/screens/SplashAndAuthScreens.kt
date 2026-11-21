package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AppLanguage
import com.example.domain.UserRole
import com.example.ui.SunFlowersUiState
import com.example.ui.components.SunFlowersEmblem
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDangerSoft
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandPrimarySoft
import com.example.ui.theme.BrandSplashPinkBottom
import com.example.ui.theme.BrandSplashPinkTop
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.PlayfairDisplayFontFamily

/**
 * Splash Screen matching Phone 1 of the mockup:
 * Blush gradient, floating petals, Rose-in-Diamond logo, SUN FLOWERS AND DECORATION,
 * italic tagline with curved pink underline, pink arc loader, and bottom-left floral accent.
 */
@Composable
fun SplashScreen(
    uiState: SunFlowersUiState,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 220f),
        label = "splash_logo_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val infinite = rememberInfiniteTransition(label = "splash_motion")
    val spinnerAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1150, easing = LinearEasing)),
        label = "spinner_rotation"
    )
    val petalDrift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "petal_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandSplashPinkTop,
                        BrandBackground,
                        BrandSurface,
                        BrandSplashPinkBottom.copy(alpha = 0.72f)
                    )
                )
            )
            .testTag("splash_screen")
    ) {
        // Decorative drifting rose petals top-left (matching Phone 1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val petals = listOf(
                Triple(Offset(size.width * 0.12f, 65f + petalDrift), 28f, BrandPrimary.copy(alpha = 0.34f)),
                Triple(Offset(size.width * 0.28f, 110f - petalDrift * 0.6f), 20f, BrandPrimary.copy(alpha = 0.22f)),
                Triple(Offset(size.width * 0.08f, 165f + petalDrift * 0.5f), 16f, Color(0xFFF375AC).copy(alpha = 0.40f)),
                Triple(Offset(size.width * 0.85f, 95f + petalDrift * 0.4f), 22f, BrandPrimary.copy(alpha = 0.16f))
            )
            petals.forEach { (center, r, color) ->
                drawOval(
                    color = color,
                    topLeft = Offset(center.x - r, center.y - r * 0.65f),
                    size = Size(r * 2f, r * 1.3f)
                )
            }
        }

        // Bottom-left lush floral cluster matching Phone 1
        Image(
            painter = painterResource(id = R.drawable.img_flower_basket_1795246897667),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-32).dp, y = 24.dp)
                .size(210.dp)
                .clip(CircleShape)
                .alpha(0.92f)
        )

        // Subtle pink silk wave across bottom right
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val wave = Path().apply {
                moveTo(size.width * 0.35f, size.height)
                quadraticTo(size.width * 0.72f, size.height * 0.25f, size.width, size.height * 0.42f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = wave,
                brush = Brush.linearGradient(
                    listOf(BrandPrimary.copy(alpha = 0.10f), BrandPrimary.copy(alpha = 0.26f))
                )
            )
        }

        // Center Brand Hero Lockup
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .offset(y = (-24).dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SunFlowersEmblem(
                size = 152.dp,
                modifier = Modifier.scale(logoScale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.strings.brandName,
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 31.sp,
                letterSpacing = 1.6.sp,
                color = BrandNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uiState.strings.brandSubtitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.2.sp
                ),
                color = BrandPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = uiState.strings.brandTagline,
                fontFamily = PlayfairDisplayFontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = BrandNavy,
                textAlign = TextAlign.Center
            )

            // Graceful curved pink underline stroke under tagline (matching Phone 1)
            Canvas(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(132.dp)
                    .height(12.dp)
            ) {
                val p = Path().apply {
                    moveTo(0f, size.height * 0.75f)
                    quadraticTo(size.width * 0.5f, 0f, size.width, size.height * 0.35f)
                }
                drawPath(
                    path = p,
                    brush = Brush.horizontalGradient(listOf(BrandPrimary, BrandPrimary.copy(alpha = 0.25f))),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Pink circular arc loader matching Phone 1
            Canvas(
                modifier = Modifier
                    .size(34.dp)
                    .rotate(spinnerAngle)
            ) {
                drawArc(
                    color = BrandPrimarySoft,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 5.5f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = BrandPrimary,
                    startAngle = -90f,
                    sweepAngle = 250f,
                    useCenter = false,
                    style = Stroke(width = 5.5f, cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = uiState.strings.loading,
                style = MaterialTheme.typography.bodySmall,
                color = BrandNavy.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * Phase 3 Auth Screen — Google Sign-In ONLY via Firebase Auth + Jetpack Credential Manager.
 */
@Composable
fun LoginScreen(
    uiState: SunFlowersUiState,
    onAttemptSilentSignIn: (Activity) -> Unit,
    onGoogleSignIn: (Activity) -> Unit,
    onEnterPreview: (UserRole) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    onOpenLegal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(uiState.firebaseAuthStatus.isGoogleSignInReady) {
        if (uiState.firebaseAuthStatus.isGoogleSignInReady && activity != null) {
            onAttemptSilentSignIn(activity)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BrandSplashPinkTop.copy(alpha = 0.6f), BrandBackground, BrandSurface)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("login_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Language Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = BrandSurface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = uiState.language == lang
                            Surface(
                                color = if (selected) BrandPrimary else Color.Transparent,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onToggleLanguage(lang) }
                                    .testTag("login_lang_${lang.code}")
                            ) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) BrandSurface else BrandNavy,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            SunFlowersEmblem(size = 118.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = uiState.strings.brandName,
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 1.4.sp,
                color = BrandNavy
            )
            Text(
                text = uiState.strings.brandSubtitle,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.8.sp),
                color = BrandPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.strings.brandTagline,
                fontFamily = PlayfairDisplayFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                color = BrandNavy.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = BrandSurface,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.strings.loginWelcome,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = BrandNavy,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.strings.loginSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { if (activity != null) onGoogleSignIn(activity) },
                        enabled = !uiState.authLoading,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_signin_button")
                    ) {
                        if (uiState.authLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = BrandSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Surface(
                                color = BrandSurface,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.strings.continueWithGoogle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    if (uiState.authError != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = BrandDangerSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.authError,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandDanger,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Helpful notice if google-services.json does not yet contain OAuth client (SHA-1/SHA-256)
            if (!uiState.firebaseAuthStatus.isGoogleSignInReady) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = BrandPrimarySoft.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = BrandPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Firebase Project: ${uiState.firebaseAuthStatus.projectId ?: "Pending"}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandPrimaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.strings.firebaseOAuthPendingNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onEnterPreview(UserRole.CUSTOMER) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("preview_customer_btn")
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Customer", style = MaterialTheme.typography.labelLarge)
                            }
                            OutlinedButton(
                                onClick = { onEnterPreview(UserRole.ADMIN) },
                                border = BorderStroke(1.2.dp, BrandNavy),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("preview_admin_btn")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shop Admin", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = uiState.strings.consentPrefix,
                style = MaterialTheme.typography.bodySmall,
                color = BrandTextMuted,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { onOpenLegal("terms") }) {
                    Text(uiState.strings.termsOfUse, style = MaterialTheme.typography.labelSmall, color = BrandPrimaryDark)
                }
                TextButton(onClick = { onOpenLegal("privacy") }) {
                    Text(uiState.strings.privacyPolicy, style = MaterialTheme.typography.labelSmall, color = BrandPrimaryDark)
                }
            }
        }
    }
}
