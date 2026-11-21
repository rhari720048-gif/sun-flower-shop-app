package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.domain.AppLanguage
import com.example.domain.MoneyFormatter
import com.example.domain.OrderRecord
import com.example.domain.OrderStateMachine
import com.example.domain.OrderStatus
import com.example.ui.SunFlowersUiState
import com.example.ui.components.DeliveryRouteMapCard
import com.example.ui.components.EmptyFloralState
import com.example.ui.components.FlowerPhoto
import com.example.ui.components.OrderStatusStepper
import com.example.ui.components.PrimaryPillButton
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

fun launchDialer(context: Context, phone: String) {
    val safePhone = phone.filter { it.isDigit() || it == '+' }
    if (safePhone.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$safePhone")))
    }
}

fun launchWhatsApp(context: Context, whatsappNumber: String, message: String) {
    val digits = whatsappNumber.filter { it.isDigit() }
    if (digits.isBlank()) return
    val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Order Details / Tracking Screen matching Phone 3 of the mockup:
 * Top bar (back, "Order Details", call shop), 3-stage pink stepper,
 * stylized delivery route map with ETA chip, Delivery Partner card (Karthik 4.8★ + Call/Chat),
 * Order Summary card (with Taxes & Fees strictly hidden when 0), Contact Support pill CTA,
 * and Cancel Order button (visible only in PLACED status).
 */
@Composable
fun OrderDetailsScreen(
    order: OrderRecord,
    uiState: SunFlowersUiState,
    showBackArrow: Boolean,
    onBack: () -> Unit,
    onCancelOrder: (OrderRecord, String) -> Unit,
    onReorder: (OrderRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("order_details_screen")
    ) {
        // Header matching Phone 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showBackArrow) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("order_details_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = uiState.strings.orderDetailsTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = BrandNavy
            )

            IconButton(
                onClick = { launchDialer(context, uiState.shopSettings.supportPhone) },
                modifier = Modifier.testTag("order_details_call_shop_btn")
            ) {
                Icon(Icons.Outlined.Call, contentDescription = "Call shop", tint = BrandNavy)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Order Status Stepper matching Phone 3
            OrderStatusStepper(
                status = order.status,
                strings = uiState.strings,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 2. Stylized Delivery Map Card with ETA pill ("Arriving in 25 min")
            DeliveryRouteMapCard(
                order = order,
                strings = uiState.strings
            )

            // 3. Delivery Partner Card (Karthik 4.8 ★ + Call & Chat buttons)
            val partner = order.deliveryPartner
            if (partner != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(0.8.dp, BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pinkCardShadow(elevation = 4.dp, cornerRadius = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BrandPrimarySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = partner.name,
                                tint = BrandPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.strings.deliveryPartner,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandTextMuted
                            )
                            Text(
                                text = partner.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandNavy
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "%.1f".format(partner.rating),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandNavy
                                )
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = BrandWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Surface(
                            color = BrandPrimarySoft,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { launchDialer(context, partner.phone) }
                                .testTag("partner_call_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Call, contentDescription = "Call delivery partner", tint = BrandPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Surface(
                            color = BrandPrimarySoft,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    launchWhatsApp(
                                        context,
                                        partner.phone,
                                        "Hi ${partner.name}, checking on my Sun Flowers order ${order.formattedOrderNumber}."
                                    )
                                }
                                .testTag("partner_chat_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Message delivery partner", tint = BrandPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }

            // 4. Order Summary Section matching Phone 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.strings.orderSummary,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = BrandNavy
                )
                Text(
                    text = order.formattedOrderNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandPrimary
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(0.8.dp, BrandBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .pinkCardShadow(elevation = 4.dp, cornerRadius = 18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FlowerPhoto(
                                imageKey = item.imageKey,
                                contentDescription = item.nameSnapshot,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.nameSnapshot,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandNavy
                                )
                                Text(
                                    text = "(${item.unitLabelSnapshot}) × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandTextMuted
                                )
                            }
                            Text(
                                text = MoneyFormatter.formatPaise(item.lineTotalPaise),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandNavy
                            )
                        }
                    }

                    HorizontalDivider(color = BrandBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(uiState.strings.deliveryCharges, style = MaterialTheme.typography.bodyMedium, color = BrandTextMuted)
                        Text(
                            text = if (order.deliveryChargePaise == 0) uiState.strings.freeLabel else MoneyFormatter.formatPaise(order.deliveryChargePaise),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = BrandNavy
                        )
                    }

                    // Rule #12: Never show Taxes & Fees while taxPaise == 0
                    if (order.taxPaise > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(uiState.strings.taxesAndFees, style = MaterialTheme.typography.bodyMedium, color = BrandTextMuted)
                            Text(
                                text = MoneyFormatter.formatPaise(order.taxPaise),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = BrandNavy
                            )
                        }
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
                            text = MoneyFormatter.formatPaise(order.totalPaise),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BrandPrimary
                        )
                    }
                }
            }

            // 5. Contact Support Primary Pink Pill Button (exact Phone 3 match)
            PrimaryPillButton(
                text = uiState.strings.contactSupport,
                icon = Icons.Filled.HeadsetMic,
                onClick = {
                    launchWhatsApp(
                        context,
                        uiState.shopSettings.supportWhatsapp,
                        "Hello Sun Flowers Support, I need help with order ${order.formattedOrderNumber}."
                    )
                },
                testTag = "contact_support_btn"
            )

            // 6. Cancel Order (strictly allowed ONLY in PLACED state) or Reorder
            if (OrderStateMachine.canCustomerCancel(order.status)) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.2.dp, BrandDanger),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_order_btn")
                ) {
                    Text(uiState.strings.cancelOrder, color = BrandDanger, fontWeight = FontWeight.SemiBold)
                }
            } else if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
                OutlinedButton(
                    onClick = { onReorder(order) },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.2.dp, BrandPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(uiState.strings.reorder, color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel ${order.formattedOrderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You can cancel before preparation starts.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelOrder(order, cancelReason)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDanger)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep Order") }
            }
        )
    }
}

@Composable
fun TrackTabScreen(
    uiState: SunFlowersUiState,
    onOpenOrder: (String) -> Unit,
    onCancelOrder: (OrderRecord, String) -> Unit,
    onReorder: (OrderRecord) -> Unit,
    onBrowseHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOrders = uiState.activeUserOrders
    if (activeOrders.size == 1) {
        // Directly show the live active delivery tracking screen (exact Phone 3 experience!)
        OrderDetailsScreen(
            order = activeOrders.first(),
            uiState = uiState,
            showBackArrow = false,
            onBack = {},
            onCancelOrder = onCancelOrder,
            onReorder = onReorder,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("track_tab_screen")
    ) {
        Text(
            text = uiState.strings.navTrack,
            style = MaterialTheme.typography.headlineLarge,
            color = BrandNavy,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (activeOrders.isEmpty()) {
            EmptyFloralState(
                title = uiState.strings.noActiveOrders,
                subtitle = uiState.strings.noActiveOrdersSubtitle,
                ctaText = uiState.strings.browseFlowers,
                onCtaClick = onBrowseHome
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeOrders, key = { it.id }) { order ->
                    OrderHistoryCard(order = order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    uiState: SunFlowersUiState,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onBrowseHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
            }
            Text(uiState.strings.myOrders, style = MaterialTheme.typography.titleLarge, color = BrandNavy)
        }
        if (uiState.userOrders.isEmpty()) {
            EmptyFloralState(
                title = uiState.strings.noActiveOrders,
                subtitle = uiState.strings.noActiveOrdersSubtitle,
                ctaText = uiState.strings.browseFlowers,
                onCtaClick = onBrowseHome
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.userOrders, key = { it.id }) { order ->
                    OrderHistoryCard(order = order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(order: OrderRecord, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(0.8.dp, BrandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowerPhoto(
                imageKey = order.items.firstOrNull()?.imageKey ?: "red_rose_bouquet",
                contentDescription = order.formattedOrderNumber,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(order.formattedOrderNumber, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = BrandNavy)
                Text(
                    text = "${order.items.sumOf { it.quantity }} item(s) • ${order.status.dbValue.replace('_', ' ').uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandPrimaryDark
                )
            }
            Text(
                text = MoneyFormatter.formatPaise(order.totalPaise),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandPrimary
            )
        }
    }
}

@Composable
fun AccountScreen(
    uiState: SunFlowersUiState,
    onOpenOrders: () -> Unit,
    onOpenAddresses: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onSwitchRoleDemo: (Boolean) -> Unit,
    onToggleLanguage: (AppLanguage) -> Unit,
    onUpdateName: (String) -> Unit,
    onOpenLegal: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditName by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var nameDraft by remember(uiState.currentUser?.name) { mutableStateOf(uiState.currentUser?.name.orEmpty()) }
    var deleteKeyword by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("account_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = uiState.strings.accountTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = BrandNavy
        )

        // Profile Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.currentUser?.name?.take(1)?.uppercase() ?: "S",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrandPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.currentUser?.name ?: "Sun Flowers Guest",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandNavy
                    )
                    Text(
                        text = uiState.currentUser?.phone?.ifBlank { uiState.currentUser.email ?: "" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Neon DB connection pill
                    Surface(
                        color = if (uiState.neonStatus.isConnected) BrandSuccessSoft else BrandPrimarySoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.neonStatus.isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (uiState.neonStatus.isConnected) BrandSuccess else BrandPrimaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (uiState.neonStatus.isConnected) uiState.strings.neonConnected else uiState.strings.neonOfflineCache,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (uiState.neonStatus.isConnected) BrandSuccess else BrandPrimaryDark
                            )
                        }
                    }
                }
                IconButton(onClick = { showEditName = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit profile name", tint = BrandPrimary)
                }
            }
        }

        // Shop Admin Entry (shown for Admin role, plus preview switcher when Firebase isn't attached yet)
        if (uiState.isAdmin) {
            Surface(
                color = BrandNavy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAdminPortal)
                    .testTag("open_admin_portal_btn")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = BrandPrimarySoft, modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.strings.shopAdminPortal, style = MaterialTheme.typography.titleMedium, color = BrandSurface)
                        Text("Open/Close shop, manage orders, products & radius", style = MaterialTheme.typography.bodySmall, color = BrandPrimarySoft)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BrandSurface)
                }
            }
        } else if (!uiState.firebaseAuthStatus.isGoogleSignInReady) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPrimarySoft.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Preview Shop Admin Role", style = MaterialTheme.typography.titleSmall, color = BrandNavy)
                        Text("Enable owner dashboard while Firebase is unlinked", style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
                    }
                    Switch(
                        checked = uiState.isAdmin,
                        onCheckedChange = onSwitchRoleDemo,
                        colors = SwitchDefaults.colors(checkedThumbColor = BrandSurface, checkedTrackColor = BrandPrimary),
                        modifier = Modifier.testTag("preview_admin_role_switch")
                    )
                }
            }
        }

        // Core Account Actions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AccountMenuRow(Icons.Filled.ReceiptLong, uiState.strings.myOrders, onClick = onOpenOrders)
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(Icons.Filled.LocationOn, uiState.strings.savedAddresses, onClick = onOpenAddresses)
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = BrandPrimary)
                        Text(uiState.strings.languageTitle, style = MaterialTheme.typography.bodyLarge, color = BrandNavy)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = uiState.language == lang
                            Surface(
                                color = if (selected) BrandPrimary else BrandPrimarySoft,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .clickable { onToggleLanguage(lang) }
                                    .testTag("account_lang_${lang.code}")
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
        }

        // Support & Legal Section (Play Store compliant)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AccountMenuRow(Icons.Filled.HelpOutline, uiState.strings.helpSupport) {
                    launchWhatsApp(context, uiState.shopSettings.supportWhatsapp, "Hello Sun Flowers Support!")
                }
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(Icons.Filled.Info, uiState.strings.aboutUs) { onOpenLegal("about") }
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(Icons.Filled.Gavel, uiState.strings.privacyPolicy) { onOpenLegal("privacy") }
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(Icons.Filled.Gavel, uiState.strings.termsOfUse) { onOpenLegal("terms") }
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(Icons.Filled.Gavel, uiState.strings.refundPolicy) { onOpenLegal("refund") }
            }
        }

        // Danger / Logout
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(0.8.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AccountMenuRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = uiState.strings.logout,
                    tint = BrandNavy,
                    onClick = onSignOut
                )
                HorizontalDivider(color = BrandBorder.copy(alpha = 0.5f))
                AccountMenuRow(
                    icon = Icons.Filled.DeleteForever,
                    label = uiState.strings.deleteAccount,
                    tint = BrandDanger,
                    onClick = { showDeleteConfirm = true }
                )
            }
        }

        Text(
            text = "Sun Flowers and Decoration • v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = BrandTextMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 18.dp)
        )
    }

    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateName(nameDraft)
                        showEditName = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(uiState.strings.deleteAccount, color = BrandDanger) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your profile, saved addresses, and wishlist will be permanently deleted. Past order invoices are anonymized for legal accounting compliance.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = deleteKeyword,
                        onValueChange = { deleteKeyword = it },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = deleteKeyword.trim().equals("DELETE", ignoreCase = true),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDanger)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color = BrandPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (tint == BrandDanger) BrandDanger else BrandNavy, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = BrandTextMuted)
    }
}

@Composable
fun LegalPolicyDialog(
    topic: String,
    shopAddress: String,
    supportPhone: String,
    radiusKm: Double,
    onDismiss: () -> Unit
) {
    val (title, body) = when (topic) {
        "privacy" -> "Privacy Policy" to """
            Operator: Sun Flowers and Decoration, $shopAddress.
            • Data collected: Name, mobile number, delivery addresses, map pin coordinates (foreground only while selecting an address), and order history.
            • Authentication: Firebase Authentication (Google / Phone OTP). Business database: Neon PostgreSQL.
            • Location: Used strictly to check our $radiusKm km delivery radius; never tracked in the background.
            • Your Rights (DPDP Act 2023): Edit profile or permanently delete your account anytime in Account → Delete Account.
            • Grievance Contact: $supportPhone.
        """.trimIndent()
        "terms" -> "Terms of Use" to """
            • Service Area: Fresh flowers and garlands are delivered within $radiusKm km of our shop.
            • Pricing: All prices shown in ₹ are final. Sun Flowers and Decoration is not GST registered (0% GST charged).
            • Shop Availability: When the shop is marked closed by the owner, browsing remains available while ordering is paused until reopening.
            • Order Acceptance: Orders are confirmed when status moves to Preparing.
        """.trimIndent()
        "refund" -> "Refund & Cancellation Policy" to """
            • Customer Cancellation: Allowed free of charge while order status is 'Placed' (before preparation begins).
            • Perishable Fresh Flowers: Returns are not accepted after delivery except for damaged or wrong items reported within 2 hours with a photo.
            • Online Refunds: Processed within 5–7 business days to the original payment method.
            • Support: Call or WhatsApp $supportPhone.
        """.trimIndent()
        else -> "About Sun Flowers and Decoration" to """
            Flowers... Always with You...
            
            Sun Flowers and Decoration crafts hand-tied rose bouquets, fragrant Madurai jasmine bunches, traditional South Indian wedding garlands (maalai), bloom baskets, and festive doorway decorations delivered fresh within $radiusKm km in minutes.
            
            Visit us: $shopAddress
            Support: $supportPhone
        """.trimIndent()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = BrandNavy) },
        text = { Text(body, style = MaterialTheme.typography.bodySmall, color = BrandNavy) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)) {
                Text("Close")
            }
        }
    )
}
