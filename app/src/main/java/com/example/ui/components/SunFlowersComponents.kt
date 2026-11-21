package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AppLanguage
import com.example.domain.EffectiveShopStatus
import com.example.domain.OrderRecord
import com.example.domain.OrderStatus
import com.example.domain.ShopStatusEngine
import com.example.i18n.AppStrings
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDangerSoft
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandPrimarySoft
import com.example.ui.theme.BrandShadowPink
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted

fun resolveDrawableForKey(imageKey: String): Int = when (imageKey) {
    "red_rose_bouquet" -> R.drawable.img_red_rose_bouquet_1795246896262
    "jasmine_bunch" -> R.drawable.img_jasmine_bunch_1795246896598
    "flower_basket" -> R.drawable.img_flower_basket_1795246897667
    "floral_garland" -> R.drawable.img_floral_garland_1795246897948
    "logo" -> R.drawable.img_app_icon_1795246899109
    else -> R.drawable.img_red_rose_bouquet_1795246896262
}

@Composable
fun FlowerPhoto(
    imageKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    Image(
        painter = painterResource(id = resolveDrawableForKey(imageKey)),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Brand rose-in-diamond logo matching Phone 1 & Phone 2 of the mockup.
 */
@Composable
fun SunFlowersEmblem(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_app_icon_1795246899109),
            contentDescription = "Sun Flowers and Decoration Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.22f))
        )
    }
}

/**
 * Animated Shop Closed status banner showing admin's custom message (EN / Tamil) and reopen time.
 */
@Composable
fun ShopClosedStatusBanner(
    status: EffectiveShopStatus,
    language: AppLanguage,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !status.effectiveOpen,
        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = BrandDangerSoft,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("shop_closed_banner")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = strings.shopClosedTitle,
                        tint = BrandPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.shopClosedTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BrandPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = status.displayMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandNavy
                    )
                    status.reopenAtEpochMillis?.let { reopenTs ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = BrandSurface,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = ShopStatusEngine.formatReopenTextIst(reopenTs, language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Circular pink '+' button that morphs with spring animation into a quantity stepper.
 */
@Composable
fun AnimatedAddOrStepperButton(
    quantity: Int,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    AnimatedContent(
        targetState = quantity > 0,
        transitionSpec = {
            (fadeIn(tween(180)) + scaleIn(initialScale = 0.85f)) togetherWith
                (fadeOut(tween(140)) + scaleOut(targetScale = 0.85f))
        },
        label = "cart_stepper_morph",
        modifier = modifier
    ) { hasQty ->
        if (!hasQty) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (enabled) BrandPrimary else BrandBorder)
                    .clickable(enabled = enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIncrement()
                    }
                    .testTag("${testTagPrefix}_add_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add to cart",
                    tint = if (enabled) BrandSurface else BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Surface(
                color = BrandPrimary,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDecrement()
                            }
                            .testTag("${testTagPrefix}_dec_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease quantity",
                            tint = BrandSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIncrement()
                            }
                            .testTag("${testTagPrefix}_inc_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase quantity",
                            tint = BrandSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWishlistHeart(
    isWishlisted: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isWishlisted) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "wishlist_pop"
    )
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(BrandSurface.copy(alpha = 0.92f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
            tint = if (isWishlisted) BrandPrimary else BrandNavy,
            modifier = Modifier
                .size(18.dp)
                .scale(scale)
        )
    }
}

/**
 * Exact 3-stage pink horizontal stepper from Mockup Phone 3 (adapts to Delivered & Cancelled too).
 */
@Composable
fun OrderStatusStepper(
    status: OrderStatus,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    if (status == OrderStatus.CANCELLED) {
        Surface(
            color = BrandDangerSoft,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = strings.statusCancelled,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandDanger,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }
        return
    }

    val activeStep = when (status) {
        OrderStatus.PLACED -> 0
        OrderStatus.PREPARING -> 1
        OrderStatus.OUT_FOR_DELIVERY -> 2
        OrderStatus.DELIVERED -> 3
        OrderStatus.CANCELLED -> -1
    }

    val progressFraction by animateFloatAsState(
        targetValue = when (activeStep) {
            0 -> 0.08f
            1 -> 0.5f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "stepper_line_progress"
    )

    val stepLabels = if (status == OrderStatus.DELIVERED) {
        listOf(strings.statusPlaced, strings.statusPreparing, strings.statusDelivered)
    } else {
        listOf(strings.statusPlaced, strings.statusPreparing, strings.statusOutForDelivery)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Connecting line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
                    .height(4.dp)
            ) {
                drawLine(
                    color = BrandBorder,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = BrandPrimary,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width * progressFraction, size.height / 2f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stepLabels.indices.forEach { index ->
                    val isCompleted = activeStep > index || (status == OrderStatus.DELIVERED && index == 2)
                    val isCurrent = activeStep == index || (status == OrderStatus.DELIVERED && index == 2)
                    val circleSize = if (index == 2 && isCurrent) 42.dp else 30.dp
                    val bgColor = if (isCompleted || isCurrent) BrandPrimary else BrandSurface
                    val borderColor = if (isCompleted || isCurrent) BrandPrimary else BrandBorder

                    Surface(
                        shape = CircleShape,
                        color = bgColor,
                        border = BorderStroke(2.dp, borderColor),
                        shadowElevation = if (isCurrent) 6.dp else 0.dp,
                        modifier = Modifier.size(circleSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                index == 2 && status == OrderStatus.OUT_FOR_DELIVERY -> Icon(
                                    imageVector = Icons.Filled.LocalShipping,
                                    contentDescription = strings.statusOutForDelivery,
                                    tint = BrandSurface,
                                    modifier = Modifier.size(21.dp)
                                )
                                isCompleted || isCurrent -> Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = BrandSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                else -> Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BrandBorder)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepLabels.forEachIndexed { index, label ->
                val isCurrent = (activeStep == index) || (status == OrderStatus.DELIVERED && index == 2)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(86.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = 14.sp
                        ),
                        color = if (isCurrent) BrandNavy else BrandTextMuted,
                        textAlign = TextAlign.Center
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandPrimary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stylized Delivery Map Card matching Mockup Phone 3:
 * Clean city map grid, pink route curve from shop to destination, decorative static delivery scooter
 * with flower basket, and floating white ETA pill ("Arriving in 25 min").
 * Note per Phase 8: decorative static vehicle icon in V1 (no fake live GPS movement).
 */
@Composable
fun DeliveryRouteMapCard(
    order: OrderRecord,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .height(188.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Soft pastel parks
                drawRoundRect(
                    color = Color(0xFFE7F3E6),
                    topLeft = Offset(w * 0.04f, h * 0.12f),
                    size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.28f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFFEBF5EA),
                    topLeft = Offset(w * 0.68f, h * 0.54f),
                    size = androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.32f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                // Clean city road network
                val roadColor = Color.White
                drawLine(roadColor, Offset(0f, h * 0.30f), Offset(w, h * 0.18f), strokeWidth = 14f)
                drawLine(roadColor, Offset(0f, h * 0.74f), Offset(w, h * 0.56f), strokeWidth = 18f)
                drawLine(roadColor, Offset(w * 0.26f, 0f), Offset(w * 0.42f, h), strokeWidth = 14f)
                drawLine(roadColor, Offset(w * 0.66f, 0f), Offset(w * 0.78f, h), strokeWidth = 14f)
                drawLine(Color(0xFFECEAF0), Offset(0f, h * 0.52f), Offset(w, h * 0.44f), strokeWidth = 6f)

                // Soft pink route curve (shop -> destination)
                val routePath = Path().apply {
                    moveTo(w * 0.04f, h * 0.78f)
                    cubicTo(
                        w * 0.30f, h * 0.64f,
                        w * 0.58f, h * 0.60f,
                        w * 0.88f, h * 0.54f
                    )
                }
                drawPath(
                    path = routePath,
                    color = BrandPrimary.copy(alpha = 0.22f),
                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = routePath,
                    color = BrandPrimary,
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                // Shop origin dot
                drawCircle(color = BrandSurface, radius = 12f, center = Offset(w * 0.08f, h * 0.75f))
                drawCircle(color = BrandNavy, radius = 7f, center = Offset(w * 0.08f, h * 0.75f))

                // Destination pulse glow
                drawCircle(color = BrandPrimary.copy(alpha = 0.18f), radius = 26f, center = Offset(w * 0.88f, h * 0.54f))
                drawCircle(color = BrandPrimary, radius = 11f, center = Offset(w * 0.88f, h * 0.54f))
                drawCircle(color = BrandSurface, radius = 4.5f, center = Offset(w * 0.88f, h * 0.54f))
            }

            // Static decorative delivery badge along route (V1 compliant — no fake live GPS)
            Surface(
                color = BrandPrimary,
                shape = CircleShape,
                shadowElevation = 6.dp,
                border = BorderStroke(2.5.dp, BrandSurface),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 86.dp, top = 26.dp)
                    .size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = "Delivery vehicle on route",
                        tint = BrandSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Destination Pin Icon
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Delivery destination",
                tint = BrandPrimary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp, bottom = 8.dp)
                    .size(34.dp)
            )

            // Floating ETA chip ("Arriving in 25 min")
            val etaLabel = when (order.status) {
                OrderStatus.DELIVERED -> strings.statusDelivered
                OrderStatus.CANCELLED -> strings.statusCancelled
                else -> strings.arrivingInMinutes.format(order.estimatedMinutes)
            }
            Surface(
                color = BrandSurface,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = BrandNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = etaLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandNavy
                        )
                        Text(
                            text = "${"%.1f".format(order.distanceKm)} km from shop",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandTextMuted
                        )
                    }
                }
            }
        }
    }
}

enum class MainBottomTab(val route: String) {
    HOME("home"),
    TRACK("track"),
    WISHLIST("wishlist"),
    ACCOUNT("account")
}

@Composable
fun SunFlowersBottomBar(
    selectedTab: MainBottomTab,
    onSelectTab: (MainBottomTab) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BrandSurface,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = BrandBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    Triple(MainBottomTab.HOME, strings.navHome, Pair(Icons.Filled.Home, Icons.Outlined.Home)),
                    Triple(MainBottomTab.TRACK, strings.navTrack, Pair(Icons.Filled.LocationOn, Icons.Outlined.LocationOn)),
                    Triple(MainBottomTab.WISHLIST, strings.navWishlist, Pair(Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)),
                    Triple(MainBottomTab.ACCOUNT, strings.navAccount, Pair(Icons.Filled.Person, Icons.Outlined.Person))
                )
                items.forEach { (tab, label, icons) ->
                    val selected = tab == selectedTab
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("bottom_tab_${tab.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selected) icons.first else icons.second,
                            contentDescription = label,
                            tint = if (selected) BrandPrimary else BrandNavy.copy(alpha = 0.72f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (selected) BrandPrimary else BrandNavy.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = BrandNavy
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = "$actionLabel →",
                style = MaterialTheme.typography.labelLarge,
                color = BrandPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("section_action_${title.lowercase().replace(' ', '_')}")
            )
        }
    }
}

fun Modifier.pinkCardShadow(elevation: Dp = 6.dp, cornerRadius: Dp = 16.dp): Modifier =
    this.shadow(
        elevation = elevation,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = BrandShadowPink,
        spotColor = BrandShadowPink
    )

@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    testTag: String = "primary_button"
) {
    Surface(
        color = if (enabled) BrandPrimary else BrandBorder,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = if (enabled) 4.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) BrandSurface else BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) BrandSurface else BrandTextMuted
            )
        }
    }
}

@Composable
fun EmptyFloralState(
    title: String,
    subtitle: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(BrandPrimarySoft, BrandSurface)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            SunFlowersEmblem(size = 64.dp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = BrandNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BrandTextMuted,
            textAlign = TextAlign.Center
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryPillButton(
                text = ctaText,
                onClick = onCtaClick,
                modifier = Modifier.width(220.dp),
                testTag = "empty_state_cta"
            )
        }
    }
}
