package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.MoneyFormatter
import com.example.domain.OrderRecord
import com.example.domain.OrderStatus
import com.example.domain.Product
import com.example.domain.ShopSettings
import com.example.domain.ShopStatusEngine
import com.example.ui.SunFlowersUiState
import com.example.ui.components.FlowerPhoto
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.ShopClosedStatusBanner
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
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * Phase 7 Complete Admin Portal:
 * Tab 0: Dashboard + 7-day bar chart + Quick Shop Toggle
 * Tab 1: Orders Management (state machine actions, assign partner, cancel with reason)
 * Tab 2: Products CRUD + Instant Stock/Featured toggles
 * Tab 3: Shop Status (custom EN/TA message, 300-char counter, reopen time, live customer preview)
 *        + Delivery Settings (radius, charges, GST=0 warning) + Partners + Audit Log
 */
@Composable
fun AdminPortalScreen(
    uiState: SunFlowersUiState,
    onBack: () -> Unit,
    onUpdateShopSettings: (ShopSettings) -> Unit,
    onUpdateOrderStatus: (OrderRecord, OrderStatus, String?, String?) -> Unit,
    onSaveProduct: (Product) -> Unit,
    onAddPartner: (String, String) -> Unit,
    onSyncNeon: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddProduct by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("admin_portal_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Admin Header
            Surface(color = BrandNavy, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandSurface)
                        }
                        Column {
                            Text(
                                text = uiState.strings.shopAdminPortal,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandSurface
                            )
                            Text(
                                text = if (uiState.effectiveShopStatus.effectiveOpen) uiState.strings.shopOpenLabel else uiState.strings.shopClosedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.effectiveShopStatus.effectiveOpen) BrandSuccessSoft else BrandPrimarySoft
                            )
                        }
                    }
                    Surface(
                        color = if (uiState.effectiveShopStatus.effectiveOpen) BrandSuccess else BrandDanger,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (uiState.effectiveShopStatus.effectiveOpen) "OPEN" else "CLOSED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Admin Tab Strip
            Surface(color = BrandSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val tabs = listOf(
                        Triple(0, uiState.strings.adminDashboard, Icons.Filled.Dashboard),
                        Triple(1, uiState.strings.adminOrders, Icons.Filled.ReceiptLong),
                        Triple(2, uiState.strings.adminProducts, Icons.Filled.Inventory2),
                        Triple(3, uiState.strings.adminSettings, Icons.Filled.Settings)
                    )
                    tabs.forEach { (idx, label, icon) ->
                        val active = selectedTab == idx
                        Surface(
                            color = if (active) BrandPrimarySoft else BrandSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = idx }
                                .testTag("admin_tab_$idx")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, contentDescription = label, tint = if (active) BrandPrimary else BrandTextMuted, modifier = Modifier.size(20.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) BrandPrimaryDark else BrandTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Tab Body
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> AdminDashboardTab(
                        uiState = uiState,
                        onQuickToggleShop = { open ->
                            onUpdateShopSettings(
                                uiState.shopSettings.copy(
                                    isOpen = open,
                                    closedMessage = if (!open && uiState.shopSettings.closedMessage.isBlank()) {
                                        ShopStatusEngine.DEFAULT_CLOSED_MESSAGE_EN
                                    } else uiState.shopSettings.closedMessage,
                                    reopenAtEpochMillis = if (open) null else uiState.shopSettings.reopenAtEpochMillis
                                )
                            )
                        },
                        onGoToShopStatus = { selectedTab = 3 }
                    )
                    1 -> AdminOrdersTab(
                        uiState = uiState,
                        onUpdateOrderStatus = onUpdateOrderStatus
                    )
                    2 -> AdminProductsTab(
                        uiState = uiState,
                        onSaveProduct = onSaveProduct
                    )
                    else -> AdminShopAndDeliverySettingsTab(
                        uiState = uiState,
                        onUpdateShopSettings = onUpdateShopSettings,
                        onAddPartner = onAddPartner,
                        onSyncNeon = onSyncNeon
                    )
                }
            }
        }

        if (selectedTab == 2) {
            FloatingActionButton(
                onClick = { showAddProduct = true },
                containerColor = BrandPrimary,
                contentColor = BrandSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("admin_add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add product")
            }
        }
    }

    if (showAddProduct) {
        AdminAddProductDialog(
            uiState = uiState,
            onDismiss = { showAddProduct = false },
            onSave = { product ->
                onSaveProduct(product)
                showAddProduct = false
            }
        )
    }
}

@Composable
private fun AdminDashboardTab(
    uiState: SunFlowersUiState,
    onQuickToggleShop: (Boolean) -> Unit,
    onGoToShopStatus: () -> Unit
) {
    val metrics = uiState.adminMetrics
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Big Shop Status Quick Toggle Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.effectiveShopStatus.effectiveOpen) BrandSuccessSoft else BrandDangerSoft
            ),
            border = BorderStroke(
                1.dp,
                if (uiState.effectiveShopStatus.effectiveOpen) BrandSuccess.copy(alpha = 0.4f) else BrandDanger.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = if (uiState.effectiveShopStatus.effectiveOpen) BrandSuccess else BrandDanger,
                    modifier = Modifier.size(30.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.effectiveShopStatus.effectiveOpen) uiState.strings.shopOpenLabel else uiState.strings.shopClosedLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandNavy
                    )
                    Text(
                        text = if (uiState.effectiveShopStatus.effectiveOpen) {
                            "Accepting orders within ${uiState.shopSettings.deliveryRadiusKm} km"
                        } else {
                            uiState.effectiveShopStatus.displayMessage
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextMuted
                    )
                    Text(
                        text = "Customize closed message & reopen time →",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandPrimaryDark,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = onGoToShopStatus)
                    )
                }
                Switch(
                    checked = uiState.effectiveShopStatus.effectiveOpen,
                    onCheckedChange = onQuickToggleShop,
                    colors = SwitchDefaults.colors(checkedTrackColor = BrandSuccess, uncheckedTrackColor = BrandDanger),
                    modifier = Modifier.testTag("admin_quick_shop_toggle")
                )
            }
        }

        // KPI Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminMetricCard("Today's Orders", metrics.todayOrdersCount.toString(), BrandPrimary, Modifier.weight(1f))
            AdminMetricCard("Today's Revenue", MoneyFormatter.formatPaise(metrics.todayRevenuePaise), BrandSuccess, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminMetricCard("New / Pending", metrics.pendingCount.toString(), BrandWarning, Modifier.weight(1f))
            AdminMetricCard("Preparing / Out", "${metrics.preparingCount + metrics.outForDeliveryCount}", BrandNavy, Modifier.weight(1f))
        }

        // 7-Day Orders Bar Chart
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Last 7 Days Orders (IST)", style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                Spacer(modifier = Modifier.height(14.dp))
                val maxVal = (metrics.last7Days.maxOfOrNull { it.orderCount } ?: 1).coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val bars = metrics.last7Days
                    if (bars.isEmpty()) return@Canvas
                    val slotW = size.width / bars.size
                    val barW = slotW * 0.48f
                    bars.forEachIndexed { idx, day ->
                        val ratio = day.orderCount.toFloat() / maxVal.toFloat()
                        val barH = (size.height * 0.85f * ratio).coerceAtLeast(10f)
                        val left = idx * slotW + (slotW - barW) / 2f
                        drawRoundRect(
                            color = if (day.orderCount > 0) BrandPrimary else BrandPrimarySoft,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    metrics.last7Days.forEach { day ->
                        Text(day.dayLabel, style = MaterialTheme.typography.labelSmall, color = BrandTextMuted)
                    }
                }
            }
        }

        // Recent Audit Log
        Text(uiState.strings.auditLogTitle, style = MaterialTheme.typography.titleMedium, color = BrandNavy)
        uiState.auditLog.take(6).forEach { log ->
            Surface(
                color = BrandSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.8.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} • ${log.adminName}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = BrandPrimaryDark)
                    Text(log.summary, style = MaterialTheme.typography.bodySmall, color = BrandNavy)
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(0.8.dp, BrandBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = BrandTextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
private fun AdminOrdersTab(
    uiState: SunFlowersUiState,
    onUpdateOrderStatus: (OrderRecord, OrderStatus, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf<OrderStatus?>(null) }
    var partnerPickerOrder by remember { mutableStateOf<OrderRecord?>(null) }
    var cancelOrderTarget by remember { mutableStateOf<OrderRecord?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    val filtered = remember(uiState.orders, filterStatus) {
        uiState.orders.filter { filterStatus == null || it.status == filterStatus }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All (${uiState.orders.size})") })
            }
            items(OrderStatus.entries) { status ->
                val count = uiState.orders.count { it.status == status }
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { filterStatus = status },
                    label = { Text("${status.dbValue.replace('_', ' ')} ($count)") }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { order ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(0.8.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.formattedOrderNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandNavy)
                            Surface(color = BrandPrimarySoft, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    text = order.status.dbValue.replace('_', ' ').uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.customerName} • ${"%.1f".format(order.distanceKm)} km", style = MaterialTheme.typography.bodyMedium, color = BrandNavy)
                                Text(order.addressSummary, style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
                            }
                            if (order.customerPhone.isNotBlank()) {
                                IconButton(onClick = { launchDialer(context, order.customerPhone) }) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call customer", tint = BrandPrimary)
                                }
                            }
                        }

                        Text(
                            text = order.items.joinToString(", ") { "${it.nameSnapshot} × ${it.quantity}" },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = BrandNavy
                        )

                        Text(
                            text = "Total: ${MoneyFormatter.formatPaise(order.totalPaise)} (${order.paymentMethod.dbValue.uppercase()} • ${order.paymentStatus.dbValue})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandPrimary
                        )

                        // State Machine Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (order.status) {
                                OrderStatus.PLACED -> {
                                    Button(
                                        onClick = { onUpdateOrderStatus(order, OrderStatus.PREPARING, null, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Accept & Prepare") }
                                    OutlinedButton(
                                        onClick = { cancelOrderTarget = order; cancelReason = "" },
                                        border = BorderStroke(1.dp, BrandDanger)
                                    ) { Text("Cancel", color = BrandDanger) }
                                }
                                OrderStatus.PREPARING -> {
                                    Button(
                                        onClick = { partnerPickerOrder = order },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Assign Partner & Dispatch") }
                                    OutlinedButton(
                                        onClick = { cancelOrderTarget = order; cancelReason = "" },
                                        border = BorderStroke(1.dp, BrandDanger)
                                    ) { Text("Cancel", color = BrandDanger) }
                                }
                                OrderStatus.OUT_FOR_DELIVERY -> {
                                    Button(
                                        onClick = { onUpdateOrderStatus(order, OrderStatus.DELIVERED, null, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Mark Delivered") }
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    partnerPickerOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { partnerPickerOrder = null },
            title = { Text("Assign Delivery Partner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.deliveryPartners.forEach { partner ->
                        Surface(
                            color = BrandBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BrandBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateOrderStatus(order, OrderStatus.OUT_FOR_DELIVERY, partner.id, null)
                                    partnerPickerOrder = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(partner.name, fontWeight = FontWeight.SemiBold, color = BrandNavy)
                                Text(partner.phone, style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { partnerPickerOrder = null }) { Text("Cancel") }
            }
        )
    }

    cancelOrderTarget?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelOrderTarget = null },
            title = { Text("Cancel ${order.formattedOrderNumber}") },
            text = {
                OutlinedTextField(
                    value = cancelReason,
                    onValueChange = { cancelReason = it },
                    label = { Text("Cancellation reason (required)") }
                )
            },
            confirmButton = {
                Button(
                    enabled = cancelReason.isNotBlank(),
                    onClick = {
                        onUpdateOrderStatus(order, OrderStatus.CANCELLED, null, cancelReason)
                        cancelOrderTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDanger)
                ) { Text("Confirm Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { cancelOrderTarget = null }) { Text("Back") }
            }
        )
    }
}

@Composable
private fun AdminProductsTab(
    uiState: SunFlowersUiState,
    onSaveProduct: (Product) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(uiState.allProducts, key = { it.id }) { product ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(0.8.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowerPhoto(
                        imageKey = product.imageKey,
                        contentDescription = product.nameEn,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.nameEn, style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                        Text(product.nameTa, style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
                        Text(
                            text = "${MoneyFormatter.formatPaise(product.pricePaise)} • ${product.unitLabel}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = BrandPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Stock", style = MaterialTheme.typography.labelSmall, color = BrandTextMuted)
                        Switch(
                            checked = product.inStock,
                            onCheckedChange = { onSaveProduct(product.copy(inStock = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandSuccess)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAddProductDialog(
    uiState: SunFlowersUiState,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameTa by remember { mutableStateOf("") }
    var unitLabel by remember { mutableStateOf("1 Bunch") }
    var priceRupees by remember { mutableStateOf("399") }
    var selectedCategoryId by remember { mutableStateOf(uiState.categories.firstOrNull()?.id ?: "cat_bouquets") }
    var imageKey by remember { mutableStateOf("red_rose_bouquet") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Flower Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (English)") }, singleLine = true)
                OutlinedTextField(value = nameTa, onValueChange = { nameTa = it }, label = { Text("Name (தமிழ்)") }, singleLine = true)
                OutlinedTextField(value = unitLabel, onValueChange = { unitLabel = it }, label = { Text("Unit (e.g. 12 Roses)") }, singleLine = true)
                OutlinedTextField(
                    value = priceRupees,
                    onValueChange = { if (it.all(Char::isDigit)) priceRupees = it },
                    label = { Text("Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text("Photo Style:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("red_rose_bouquet" to "Roses", "jasmine_bunch" to "Jasmine", "flower_basket" to "Basket", "floral_garland" to "Garland").forEach { (k, label) ->
                        FilterChip(selected = imageKey == k, onClick = { imageKey = k }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = nameEn.isNotBlank() && (priceRupees.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val paise = (priceRupees.toIntOrNull() ?: 0) * 100
                    onSave(
                        Product(
                            id = "prod_${UUID.randomUUID().toString().take(8)}",
                            categoryId = selectedCategoryId,
                            nameEn = nameEn.trim(),
                            nameTa = nameTa.trim().ifBlank { nameEn.trim() },
                            descriptionEn = "Freshly handcrafted at Sun Flowers and Decoration.",
                            descriptionTa = "சன் ஃபிளவர்ஸ் அண்ட் டெக்கரேஷனில் புதிதாக தயாரிக்கப்பட்டது.",
                            pricePaise = paise,
                            mrpPaise = null,
                            imageKey = imageKey,
                            unitLabel = unitLabel.trim().ifBlank { "1 Piece" },
                            inStock = true,
                            isFeatured = true,
                            isActive = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AdminShopAndDeliverySettingsTab(
    uiState: SunFlowersUiState,
    onUpdateShopSettings: (ShopSettings) -> Unit,
    onAddPartner: (String, String) -> Unit,
    onSyncNeon: () -> Unit
) {
    val s = uiState.shopSettings
    var isOpen by remember(s) { mutableStateOf(s.isOpen) }
    var closedMessage by remember(s) { mutableStateOf(s.closedMessage) }
    var reopenAt by remember(s) { mutableStateOf(s.reopenAtEpochMillis) }
    var radiusText by remember(s) { mutableStateOf(s.deliveryRadiusKm.toString()) }
    var chargeText by remember(s) { mutableStateOf((s.deliveryChargePaise / 100).toString()) }
    var freeAboveText by remember(s) { mutableStateOf(((s.freeDeliveryAbovePaise ?: 0) / 100).toString()) }
    var minOrderText by remember(s) { mutableStateOf((s.minOrderPaise / 100).toString()) }
    var taxText by remember(s) { mutableStateOf(s.taxPercent.toString()) }
    var etaText by remember(s) { mutableStateOf(s.estimatedDeliveryMinutes.toString()) }
    var codEnabled by remember(s) { mutableStateOf(s.codEnabled) }
    var onlineEnabled by remember(s) { mutableStateOf(s.onlinePaymentEnabled) }
    var newPartnerName by remember { mutableStateOf("") }
    var newPartnerPhone by remember { mutableStateOf("") }

    val previewStatus = ShopStatusEngine.computeEffectiveStatus(
        settings = s.copy(isOpen = isOpen, closedMessage = closedMessage, reopenAtEpochMillis = reopenAt),
        serverNowEpochMillis = System.currentTimeMillis(),
        language = uiState.language
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Shop Status Control with Live Customer Preview
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiState.strings.shopStatusControl, style = MaterialTheme.typography.titleMedium, color = BrandNavy)
                    Switch(
                        checked = isOpen,
                        onCheckedChange = {
                            isOpen = it
                            if (it) reopenAt = null
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = BrandSuccess, uncheckedTrackColor = BrandDanger),
                        modifier = Modifier.testTag("admin_settings_shop_switch")
                    )
                }

                if (!isOpen) {
                    OutlinedTextField(
                        value = closedMessage,
                        onValueChange = { if (it.length <= 300) closedMessage = it },
                        label = { Text(uiState.strings.closedMessageHint) },
                        supportingText = { Text("${closedMessage.length}/300") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_closed_message_input")
                    )

                    Text("Quick Templates:", style = MaterialTheme.typography.labelSmall, color = BrandTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { closedMessage = "Shop is closed today. We will reopen tomorrow at 8:00 AM." },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Tomorrow 8 AM (EN)", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = { closedMessage = "இன்று கடை மூடப்பட்டுள்ளது. நாளை காலை 8:00 மணிக்கு மீண்டும் திறக்கப்படும்." },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("நாளை 8 AM (தமிழ்)", style = MaterialTheme.typography.labelSmall) }
                    }

                    Text("Auto-Reopen Time (IST):", style = MaterialTheme.typography.labelSmall, color = BrandTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reopenAt != null,
                            onClick = {
                                val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 8)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                }
                                reopenAt = cal.timeInMillis
                            },
                            label = { Text("Tomorrow 8:00 AM") }
                        )
                        FilterChip(
                            selected = reopenAt == null,
                            onClick = { reopenAt = null },
                            label = { Text("Manual Reopen") }
                        )
                    }

                    Text("Live Customer Preview:", style = MaterialTheme.typography.labelSmall, color = BrandPrimaryDark)
                    ShopClosedStatusBanner(
                        status = previewStatus,
                        language = uiState.language,
                        strings = uiState.strings
                    )
                }
            }
        }

        // 2. Delivery & Tax Settings (with GST=0 Notice per Rule #12)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Delivery & Pricing Rules", style = MaterialTheme.typography.titleMedium, color = BrandNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = radiusText,
                        onValueChange = { radiusText = it },
                        label = { Text("Radius (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = etaText,
                        onValueChange = { if (it.all(Char::isDigit)) etaText = it },
                        label = { Text("ETA (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chargeText,
                        onValueChange = { if (it.all(Char::isDigit)) chargeText = it },
                        label = { Text("Delivery ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = freeAboveText,
                        onValueChange = { if (it.all(Char::isDigit)) freeAboveText = it },
                        label = { Text("Free Above ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minOrderText,
                        onValueChange = { if (it.all(Char::isDigit)) minOrderText = it },
                        label = { Text("Min Order ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxText,
                        onValueChange = { taxText = it },
                        label = { Text("GST % (Default 0)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Surface(color = BrandWarningSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Keep GST at 0% unless Sun Flowers is GST registered. Taxes line stays hidden for customers while 0%.",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandNavy
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cash on Delivery (COD)", style = MaterialTheme.typography.bodyMedium, color = BrandNavy)
                    Switch(checked = codEnabled, onCheckedChange = { codEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Online Payment (Cashfree)", style = MaterialTheme.typography.bodyMedium, color = BrandNavy)
                    Switch(checked = onlineEnabled, onCheckedChange = { onlineEnabled = it })
                }
            }
        }

        PrimaryPillButton(
            text = uiState.strings.saveChanges,
            onClick = {
                val freeAbove = (freeAboveText.toIntOrNull() ?: 0).let { if (it > 0) it * 100 else null }
                onUpdateShopSettings(
                    s.copy(
                        isOpen = isOpen,
                        closedMessage = closedMessage.trim(),
                        reopenAtEpochMillis = if (isOpen) null else reopenAt,
                        deliveryRadiusKm = (radiusText.toDoubleOrNull() ?: s.deliveryRadiusKm).coerceIn(1.0, 25.0),
                        deliveryChargePaise = (chargeText.toIntOrNull() ?: 40) * 100,
                        freeDeliveryAbovePaise = freeAbove,
                        minOrderPaise = (minOrderText.toIntOrNull() ?: 0) * 100,
                        taxPercent = (taxText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 28.0),
                        estimatedDeliveryMinutes = (etaText.toIntOrNull() ?: 25).coerceIn(10, 180),
                        codEnabled = codEnabled,
                        onlinePaymentEnabled = onlineEnabled
                    )
                )
            },
            testTag = "admin_save_settings_btn"
        )

        // 3. Delivery Partners
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delivery Partners", style = MaterialTheme.typography.titleMedium, color = BrandNavy)
                uiState.deliveryPartners.forEach { p ->
                    Text("• ${p.name} (${p.phone}) — ★ ${p.rating}", style = MaterialTheme.typography.bodySmall, color = BrandNavy)
                }
                HorizontalDivider(color = BrandBorder)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newPartnerName, onValueChange = { newPartnerName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = newPartnerPhone, onValueChange = { newPartnerPhone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedButton(
                    enabled = newPartnerName.isNotBlank() && newPartnerPhone.isNotBlank(),
                    onClick = {
                        onAddPartner(newPartnerName, newPartnerPhone)
                        newPartnerName = ""
                        newPartnerPhone = ""
                    }
                ) { Text("Add Delivery Partner") }
            }
        }

        // 4. Neon PostgreSQL & Firebase Auth Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.neonStatus.isConnected) BrandSuccessSoft else BrandSurface
            ),
            border = BorderStroke(
                1.dp,
                if (uiState.neonStatus.isConnected) BrandSuccess.copy(alpha = 0.45f) else BrandBorder
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cloud Database & Auth Status", style = MaterialTheme.typography.titleMedium, color = BrandNavy)
                Text(
                    text = "Neon PostgreSQL: ${if (uiState.neonStatus.isConnected) "Connected (${uiState.neonStatus.hostMasked})" else uiState.neonStatus.lastSyncMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.neonStatus.isConnected) BrandSuccess else BrandTextMuted
                )
                Text(
                    text = "Firebase Auth: ${uiState.firebaseAuthStatus.projectId ?: "Not configured"} • Google OAuth ${if (uiState.firebaseAuthStatus.hasWebClientId) "Ready" else "Pending SHA-1/SHA-256"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onSyncNeon,
                    modifier = Modifier.testTag("admin_sync_neon_btn")
                ) {
                    Text("Test & Sync Neon DB")
                }
            }
        }
    }
}
