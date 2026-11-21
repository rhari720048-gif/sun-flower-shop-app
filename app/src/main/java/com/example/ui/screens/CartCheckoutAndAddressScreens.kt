package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CheckoutBlocker
import com.example.domain.GeoUtils
import com.example.domain.MoneyFormatter
import com.example.domain.OrderRecord
import com.example.domain.PaymentMethod
import com.example.domain.PricingBreakdown
import com.example.domain.Product
import com.example.domain.SavedAddress
import com.example.ui.SunFlowersUiState
import com.example.ui.components.AnimatedAddOrStepperButton
import com.example.ui.components.EmptyFloralState
import com.example.ui.components.FlowerPhoto
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.ShopClosedStatusBanner
import com.example.ui.components.pinkCardShadow
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDangerSoft
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryDark
import com.example.ui.theme.BrandPrimarySoft
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandSuccessSoft
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandWarning
import com.example.ui.theme.BrandWarningSoft

@Composable
fun CartScreen(
    uiState: SunFlowersUiState,
    onBack: () -> Unit,
    onBrowseHome: () -> Unit,
    onIncrement: (Product) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onUndoRemove: () -> Unit,
    onProceedCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validation = uiState.cartValidation
    val pricing = validation?.pricing
    val shopOpen = uiState.effectiveShopStatus.effectiveOpen

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("cart_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("cart_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
            }
            Text(
                text = uiState.strings.cartTitle,
                style = MaterialTheme.typography.titleLarge,
                color = BrandNavy
            )
        }

        if (uiState.cartItems.isEmpty()) {
            EmptyFloralState(
                title = uiState.strings.emptyCartTitle,
                subtitle = uiState.strings.emptyCartSubtitle,
                ctaText = uiState.strings.browseFlowers,
                onCtaClick = onBrowseHome,
                modifier = Modifier.weight(1f)
            )
            return
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!shopOpen) {
                item(key = "cart_closed") {
                    ShopClosedStatusBanner(
                        status = uiState.effectiveShopStatus,
                        language = uiState.language,
                        strings = uiState.strings
                    )
                }
            }

            if (uiState.lastRemovedCartItem != null) {
                item(key = "undo_bar") {
                    Surface(
                        color = BrandNavy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Item removed from cart", color = BrandSurface, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onUndoRemove) {
                                Text("UNDO", color = BrandPrimarySoft, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (pricing != null && uiState.shopSettings.freeDeliveryAbovePaise != null) {
                item(key = "free_delivery_progress") {
                    val threshold = uiState.shopSettings.freeDeliveryAbovePaise
                    val ratio = (pricing.subtotalPaise.toFloat() / threshold.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    Surface(
                        color = if (pricing.freeDeliveryUnlocked) BrandSuccessSoft else BrandPrimarySoft.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (pricing.freeDeliveryUnlocked) {
                                    "Free delivery unlocked on this order!"
                                } else {
                                    "Add ${MoneyFormatter.formatPaise(pricing.amountForFreeDeliveryPaise)} more for FREE delivery"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (pricing.freeDeliveryUnlocked) BrandSuccess else BrandPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = if (pricing.freeDeliveryUnlocked) BrandSuccess else BrandPrimary,
                                trackColor = BrandSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                }
            }

            items(uiState.cartItems, key = { it.product.id }) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(0.8.dp, BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pinkCardShadow(elevation = 3.dp, cornerRadius = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowerPhoto(
                            imageKey = item.product.imageKey,
                            contentDescription = item.product.localizedName(uiState.language),
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.localizedName(uiState.language),
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandNavy
                            )
                            Text(
                                text = item.product.unitLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyFormatter.formatPaise(item.product.pricePaise * item.quantity),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(
                                onClick = { onRemove(item.product.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove item",
                                    tint = BrandTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedAddOrStepperButton(
                                quantity = item.quantity,
                                enabled = shopOpen && item.product.inStock,
                                onIncrement = { onIncrement(item.product) },
                                onDecrement = { onDecrement(item.product.id) },
                                testTagPrefix = "cart_${item.product.id}"
                            )
                        }
                    }
                }
            }

            if (pricing != null) {
                item(key = "bill_summary") {
                    BillSummaryCard(pricing = pricing, uiState = uiState)
                }
            }

            if (validation != null && CheckoutBlocker.BELOW_MIN_ORDER in validation.blockers) {
                item(key = "min_order_warn") {
                    Surface(
                        color = BrandWarningSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = BrandWarning)
                            Text(
                                text = "Minimum order amount is ${MoneyFormatter.formatPaise(uiState.shopSettings.minOrderPaise)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandNavy
                            )
                        }
                    }
                }
            }
        }

        // Sticky Checkout Bar
        Surface(
            color = BrandSurface,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            val blockedByCart = !shopOpen ||
                (validation?.blockers?.any {
                    it == CheckoutBlocker.BELOW_MIN_ORDER || it == CheckoutBlocker.OUT_OF_STOCK
                } == true)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                PrimaryPillButton(
                    text = if (!shopOpen) {
                        uiState.strings.shopClosedShort
                    } else {
                        "${uiState.strings.selectAddress} • ${MoneyFormatter.formatPaise(pricing?.totalPaise ?: 0)}"
                    },
                    enabled = !blockedByCart,
                    onClick = onProceedCheckout,
                    testTag = "cart_checkout_btn"
                )
            }
        }
    }
}

@Composable
fun BillSummaryCard(
    pricing: PricingBreakdown,
    uiState: SunFlowersUiState,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(0.8.dp, BrandBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = uiState.strings.billSummary,
                style = MaterialTheme.typography.titleMedium,
                color = BrandNavy
            )
            PriceRow(uiState.strings.itemTotal, MoneyFormatter.formatPaise(pricing.subtotalPaise))
            PriceRow(
                label = uiState.strings.deliveryCharges,
                value = if (pricing.freeDeliveryUnlocked) uiState.strings.freeLabel else MoneyFormatter.formatPaise(pricing.deliveryChargePaise),
                valueColor = if (pricing.freeDeliveryUnlocked) BrandSuccess else BrandNavy
            )
            // Rule #12: Never display GST/Taxes line while tax_percent == 0
            if (pricing.showTaxRow) {
                PriceRow(uiState.strings.taxesAndFees, MoneyFormatter.formatPaise(pricing.taxPaise))
            }
            HorizontalDivider(color = BrandBorder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.strings.totalAmount,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = BrandNavy
                )
                Text(
                    text = MoneyFormatter.formatPaise(pricing.totalPaise),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = BrandNavy
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = BrandTextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

/**
 * Phase 5 Addresses & Delivery Radius Management Screen.
 */
@Composable
fun AddressesScreen(
    uiState: SunFlowersUiState,
    onBack: () -> Unit,
    onSelectAddress: (String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSaveAddress: (String, String, String, String, String, String, String, Double, Double, Boolean, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("addresses_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
            }
            Text(uiState.strings.savedAddresses, style = MaterialTheme.typography.titleLarge, color = BrandNavy)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.addresses, key = { it.id }) { addr ->
                val isSelected = addr.id == uiState.selectedAddress?.id
                val inRadius = GeoUtils.isWithinRadius(addr.distanceKm, uiState.shopSettings.deliveryRadiusKm)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(if (isSelected) 1.8.dp else 0.8.dp, if (isSelected) BrandPrimary else BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAddress(addr.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) BrandPrimary else BrandTextMuted
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = addr.label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BrandNavy
                                )
                                Surface(
                                    color = if (inRadius) BrandSuccessSoft else BrandDangerSoft,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "${"%.1f".format(addr.distanceKm)} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (inRadius) BrandSuccess else BrandDanger,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${addr.recipientName} • ${addr.recipientPhone}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = BrandNavy
                            )
                            Text(
                                text = "${addr.line1}, ${addr.line2} - ${addr.pincode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandTextMuted
                            )
                        }
                        IconButton(onClick = { onDeleteAddress(addr.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete address", tint = BrandDanger)
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryPillButton(
                text = uiState.strings.addAddress,
                icon = Icons.Filled.AddLocationAlt,
                onClick = { showAddDialog = true },
                testTag = "add_address_btn"
            )
        }
    }

    if (showAddDialog) {
        AddAddressDialog(
            uiState = uiState,
            onDismiss = { showAddDialog = false },
            onSave = { label, name, phone, line1, line2, landmark, pin, lat, lng ->
                onSaveAddress(label, name, phone, line1, line2, landmark, pin, lat, lng, true) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun AddAddressDialog(
    uiState: SunFlowersUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Double, Double) -> Unit
) {
    var label by remember { mutableStateOf("home") }
    var name by remember { mutableStateOf(uiState.currentUser?.name.orEmpty()) }
    var phone by remember { mutableStateOf(uiState.currentUser?.phone?.takeLast(10).orEmpty()) }
    var line1 by remember { mutableStateOf("") }
    var line2 by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("641002") }

    // Area presets near Coimbatore shop (11.0081, 76.9510) + 1 outside-radius preset for live verification
    val areaPins = remember {
        listOf(
            Triple("RS Puram (~1.1 km)", 11.0145, 76.9562),
            Triple("Race Course (~2.4 km)", 11.0012, 76.9718),
            Triple("Saibaba Colony (~3.5 km)", 11.0285, 76.9420),
            Triple("Peelamedu (~5.7 km)", 11.0290, 77.0010),
            Triple("Saravanampatti (~9.4 km - Out of Radius)", 11.0802, 76.9965)
        )
    }
    var pickedLat by remember { mutableDoubleStateOf(areaPins.first().second) }
    var pickedLng by remember { mutableDoubleStateOf(areaPins.first().third) }

    val liveDist = GeoUtils.haversineKm(uiState.shopSettings.shopLat, uiState.shopSettings.shopLng, pickedLat, pickedLng)
    val inRadius = GeoUtils.isWithinRadius(liveDist, uiState.shopSettings.deliveryRadiusKm)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(uiState.strings.addAddress, color = BrandNavy) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("home", "work", "other").forEach { item ->
                        FilterChip(
                            selected = label == item,
                            onClick = { label = item },
                            label = { Text(item.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Text("Select Delivery Pin Location:", style = MaterialTheme.typography.labelMedium, color = BrandNavy)
                areaPins.forEach { (title, lat, lng) ->
                    val active = pickedLat == lat && pickedLng == lng
                    Surface(
                        color = if (active) BrandPrimarySoft else BrandBackground,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (active) BrandPrimary else BrandBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pickedLat = lat
                                pickedLng = lng
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            Text(title, style = MaterialTheme.typography.bodySmall, color = BrandNavy)
                        }
                    }
                }

                // Live Haversine Radius Feedback Banner
                Surface(
                    color = if (inRadius) BrandSuccessSoft else BrandDangerSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (inRadius) {
                            uiState.strings.deliverableWithinRadius.format(liveDist)
                        } else {
                            uiState.strings.outsideDeliveryRadius.format(liveDist, uiState.shopSettings.deliveryRadiusKm)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (inRadius) BrandSuccess else BrandDanger,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Recipient Name") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) phone = it },
                    label = { Text("10-digit Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = line1, onValueChange = { line1 = it }, label = { Text("House / Street (Line 1)") }, singleLine = true)
                OutlinedTextField(value = line2, onValueChange = { line2 = it }, label = { Text("Area / Locality (Line 2)") }, singleLine = true)
                OutlinedTextField(value = landmark, onValueChange = { landmark = it }, label = { Text("Landmark (Optional)") }, singleLine = true)
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pincode = it },
                    label = { Text("6-digit Pincode") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = inRadius,
                onClick = { onSave(label, name, phone, line1, line2, landmark, pincode, pickedLat, pickedLng) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Phase 6 Checkout Screen with idempotency key per attempt, payment method selection, and bill verification.
 */
@Composable
fun CheckoutScreen(
    uiState: SunFlowersUiState,
    onBack: () -> Unit,
    onChangeAddress: () -> Unit,
    onSelectPayment: (PaymentMethod) -> Unit,
    onPlaceOrder: (String, String) -> Unit,
    newIdempotencyKey: () -> String,
    modifier: Modifier = Modifier
) {
    var customerNote by remember { mutableStateOf("") }
    // Single idempotency key generated once per checkout attempt to prevent duplicate orders on double tap / retry
    val attemptIdempotencyKey = remember { newIdempotencyKey() }
    val validation = uiState.cartValidation
    val pricing = validation?.pricing
    val address = uiState.selectedAddress

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("checkout_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
            }
            Text(uiState.strings.checkoutTitle, style = MaterialTheme.typography.titleLarge, color = BrandNavy)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedVisibility(visible = !uiState.effectiveShopStatus.effectiveOpen) {
                ShopClosedStatusBanner(
                    status = uiState.effectiveShopStatus,
                    language = uiState.language,
                    strings = uiState.strings
                )
            }

            // Delivery Address Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(0.8.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                            Text("Deliver To", style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                        }
                        TextButton(onClick = onChangeAddress) {
                            Text(if (address == null) "Add Address" else "Change", color = BrandPrimary)
                        }
                    }
                    if (address != null) {
                        Text(
                            text = "${address.recipientName} (${address.label.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = BrandNavy
                        )
                        Text(
                            text = "${address.line1}, ${address.line2} - ${address.pincode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(color = BrandSuccessSoft, shape = RoundedCornerShape(999.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = BrandSuccess, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "ETA ${uiState.shopSettings.estimatedDeliveryMinutes} mins • ${"%.1f".format(address.distanceKm)} km from shop",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSuccess
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No delivery address selected yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandDanger
                        )
                    }
                }
            }

            // Payment Method Selector (COD + Cashfree Online)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(0.8.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                    if (uiState.shopSettings.codEnabled) {
                        PaymentOptionRow(
                            selected = uiState.selectedPaymentMethod == PaymentMethod.COD,
                            icon = Icons.Filled.Payments,
                            title = uiState.strings.codLabel,
                            subtitle = uiState.strings.codSubtitle,
                            onClick = { onSelectPayment(PaymentMethod.COD) },
                            testTag = "payment_cod_option"
                        )
                    }
                    if (uiState.shopSettings.onlinePaymentEnabled) {
                        PaymentOptionRow(
                            selected = uiState.selectedPaymentMethod == PaymentMethod.ONLINE,
                            icon = Icons.Filled.CreditCard,
                            title = uiState.strings.onlinePayLabel,
                            subtitle = uiState.strings.onlinePaySubtitle,
                            onClick = { onSelectPayment(PaymentMethod.ONLINE) },
                            testTag = "payment_online_option"
                        )
                    }
                }
            }

            // Customer Ribbon / Delivery Note (<= 200 chars)
            OutlinedTextField(
                value = customerNote,
                onValueChange = { if (it.length <= 200) customerNote = it },
                label = { Text(uiState.strings.deliveryNoteHint) },
                supportingText = { Text("${customerNote.length}/200") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (pricing != null) {
                BillSummaryCard(pricing = pricing, uiState = uiState)
            }
        }

        Surface(color = BrandSurface, shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                PrimaryPillButton(
                    text = if (uiState.isSubmittingOrder) {
                        uiState.strings.loading
                    } else {
                        "${uiState.strings.placeOrder} • ${MoneyFormatter.formatPaise(pricing?.totalPaise ?: 0)}"
                    },
                    enabled = !uiState.isSubmittingOrder && (validation?.canCheckout == true),
                    onClick = { onPlaceOrder(customerNote, attemptIdempotencyKey) },
                    testTag = "checkout_place_order_btn"
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) BrandPrimarySoft.copy(alpha = 0.65f) else BrandBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrandPrimary else BrandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BrandPrimary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
            }
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) BrandPrimary else BrandTextMuted
            )
        }
    }
}

@Composable
fun OrderSuccessScreen(
    order: OrderRecord,
    uiState: SunFlowersUiState,
    onTrackOrder: () -> Unit,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    var bloomed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { bloomed = true }
    val scale by animateFloatAsState(
        targetValue = if (bloomed) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "success_bloom"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp)
            .testTag("order_success_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(BrandSuccessSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Order Placed",
                tint = BrandSuccess,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = uiState.strings.orderPlacedSuccess,
            style = MaterialTheme.typography.headlineLarge,
            color = BrandNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.strings.orderSuccessSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BrandTextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            color = BrandSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = order.formattedOrderNumber,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = BrandPrimary
                )
                Text(
                    text = "Total: ${MoneyFormatter.formatPaise(order.totalPaise)} • ETA ${order.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandNavy
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        PrimaryPillButton(
            text = uiState.strings.trackOrder,
            onClick = onTrackOrder,
            testTag = "order_success_track_btn"
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onContinueShopping,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.2.dp, BrandPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(uiState.strings.continueShopping, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
