package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.AppLanguage
import com.example.ui.components.MainBottomTab
import com.example.ui.components.SunFlowersBottomBar
import com.example.ui.components.SunFlowersEmblem
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AddressesScreen
import com.example.ui.screens.AdminPortalScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalPolicyDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrderDetailsScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.OrderSuccessScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TrackTabScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.screens.launchDialer
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandBorder
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.SunFlowersTheme
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val TRACK = "track"
    const val WISHLIST = "wishlist"
    const val ACCOUNT = "account"
    const val CATALOG = "catalog?categoryId={categoryId}"
    const val PRODUCT = "product/{productId}"
    const val CART = "cart"
    const val ADDRESSES = "addresses"
    const val CHECKOUT = "checkout"
    const val ORDER_SUCCESS = "order_success/{orderId}"
    const val ORDER_DETAILS = "order_details/{orderId}"
    const val ORDER_HISTORY = "order_history"
    const val ADMIN = "admin"

    fun catalog(categoryId: String? = null): String =
        if (categoryId.isNullOrBlank()) "catalog" else "catalog?categoryId=$categoryId"

    fun product(productId: String): String = "product/$productId"
    fun orderSuccess(orderId: String): String = "order_success/$orderId"
    fun orderDetails(orderId: String): String = "order_details/$orderId"
}

@Composable
fun SunFlowersApp(viewModel: SunFlowersViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var legalTopic by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    SunFlowersTheme(isTamil = uiState.language == AppLanguage.TA) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isSplashVisible -> {
                    SplashScreen(uiState = uiState)
                }
                uiState.currentUser == null -> {
                    LoginScreen(
                        uiState = uiState,
                        onAttemptSilentSignIn = viewModel::attemptSilentGoogleSignIn,
                        onGoogleSignIn = viewModel::signInWithGoogle,
                        onEnterPreview = { role -> viewModel.enterPreviewSession(role) },
                        onToggleLanguage = viewModel::setLanguage,
                        onOpenLegal = { legalTopic = it }
                    )
                }
                else -> {
                    AuthenticatedMainShell(
                        uiState = uiState,
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onOpenLegal = { legalTopic = it }
                    )
                }
            }

            legalTopic?.let { topic ->
                LegalPolicyDialog(
                    topic = topic,
                    shopAddress = uiState.shopSettings.shopAddress,
                    supportPhone = uiState.shopSettings.supportPhone,
                    radiusKm = uiState.shopSettings.deliveryRadiusKm,
                    onDismiss = { legalTopic = null }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedMainShell(
    uiState: SunFlowersUiState,
    viewModel: SunFlowersViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenLegal: (String) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.TRACK, Routes.WISHLIST, Routes.ACCOUNT)
    val selectedTab = when (currentRoute) {
        Routes.TRACK -> MainBottomTab.TRACK
        Routes.WISHLIST -> MainBottomTab.WISHLIST
        Routes.ACCOUNT -> MainBottomTab.ACCOUNT
        else -> MainBottomTab.HOME
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showBottomBar,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BrandSurface,
                modifier = Modifier.width(290.dp)
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SunFlowersEmblem(size = 52.dp)
                        Column {
                            Text(uiState.strings.brandName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BrandNavy)
                            Text(uiState.strings.brandSubtitle, style = MaterialTheme.typography.labelSmall, color = BrandPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(uiState.strings.brandTagline, style = MaterialTheme.typography.bodySmall, color = BrandTextMuted)
                }
                HorizontalDivider(color = BrandBorder)
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isAdmin) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = BrandPrimary) },
                        label = { Text(uiState.strings.shopAdminPortal, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.ADMIN)
                        }
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Language, contentDescription = null, tint = BrandPrimary) },
                    label = {
                        Text(
                            if (uiState.language == AppLanguage.EN) "Switch to தமிழ்" else "Switch to English"
                        )
                    },
                    selected = false,
                    onClick = {
                        viewModel.setLanguage(if (uiState.language == AppLanguage.EN) AppLanguage.TA else AppLanguage.EN)
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Call, contentDescription = null, tint = BrandPrimary) },
                    label = { Text(uiState.strings.helpSupport) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        launchDialer(context, uiState.shopSettings.supportPhone)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = BrandPrimary) },
                    label = { Text(uiState.strings.aboutUs) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenLegal("about")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Gavel, contentDescription = null, tint = BrandPrimary) },
                    label = { Text(uiState.strings.privacyPolicy) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenLegal("privacy")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Share, contentDescription = null, tint = BrandPrimary) },
                    label = { Text("Share Sun Flowers") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Order fresh roses, Madurai jasmine & garlands from Sun Flowers and Decoration!"
                            )
                        }
                        runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share Sun Flowers")) }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = BrandBackground,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    SunFlowersBottomBar(
                        selectedTab = selectedTab,
                        onSelectTab = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        strings = uiState.strings
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        uiState = uiState,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenCart = { navController.navigate(Routes.CART) },
                        onOpenSearch = { navController.navigate(Routes.catalog(null)) },
                        onOpenCategory = { catId -> navController.navigate(Routes.catalog(catId)) },
                        onOpenProduct = { pid -> navController.navigate(Routes.product(pid)) },
                        onToggleWishlist = viewModel::toggleWishlist,
                        onIncrementCart = viewModel::incrementCart,
                        onDecrementCart = viewModel::decrementCart
                    )
                }

                composable(Routes.TRACK) {
                    TrackTabScreen(
                        uiState = uiState,
                        onOpenOrder = { oid -> navController.navigate(Routes.orderDetails(oid)) },
                        onCancelOrder = viewModel::cancelOrderByCustomer,
                        onReorder = { order -> viewModel.reorder(order) { navController.navigate(Routes.CART) } },
                        onBrowseHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.WISHLIST) {
                    WishlistScreen(
                        uiState = uiState,
                        onOpenProduct = { pid -> navController.navigate(Routes.product(pid)) },
                        onBrowseHome = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        onToggleWishlist = viewModel::toggleWishlist,
                        onIncrementCart = viewModel::incrementCart,
                        onDecrementCart = viewModel::decrementCart
                    )
                }

                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        uiState = uiState,
                        onOpenOrders = { navController.navigate(Routes.ORDER_HISTORY) },
                        onOpenAddresses = { navController.navigate(Routes.ADDRESSES) },
                        onOpenAdminPortal = { navController.navigate(Routes.ADMIN) },
                        onSwitchRoleDemo = viewModel::switchRoleForDemo,
                        onToggleLanguage = viewModel::setLanguage,
                        onUpdateName = viewModel::updateProfileName,
                        onOpenLegal = onOpenLegal,
                        onDeleteAccount = viewModel::deleteAccount,
                        onSignOut = viewModel::signOut
                    )
                }

                composable(
                    route = Routes.CATALOG,
                    arguments = listOf(navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { backStack ->
                    val catId = backStack.arguments?.getString("categoryId")
                    CatalogScreen(
                        uiState = uiState,
                        initialCategoryId = catId,
                        onBack = { navController.popBackStack() },
                        onOpenProduct = { pid -> navController.navigate(Routes.product(pid)) },
                        onToggleWishlist = viewModel::toggleWishlist,
                        onIncrementCart = viewModel::incrementCart,
                        onDecrementCart = viewModel::decrementCart
                    )
                }

                composable(
                    route = Routes.PRODUCT,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) { backStack ->
                    val productId = backStack.arguments?.getString("productId").orEmpty()
                    ProductDetailScreen(
                        uiState = uiState,
                        productId = productId,
                        onBack = { navController.popBackStack() },
                        onOpenCart = { navController.navigate(Routes.CART) },
                        onToggleWishlist = viewModel::toggleWishlist,
                        onIncrementCart = viewModel::incrementCart,
                        onDecrementCart = viewModel::decrementCart
                    )
                }

                composable(Routes.CART) {
                    CartScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onBrowseHome = {
                            navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                        },
                        onIncrement = viewModel::incrementCart,
                        onDecrement = viewModel::decrementCart,
                        onRemove = viewModel::removeFromCart,
                        onUndoRemove = viewModel::undoRemoveFromCart,
                        onProceedCheckout = { navController.navigate(Routes.CHECKOUT) }
                    )
                }

                composable(Routes.ADDRESSES) {
                    AddressesScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onSelectAddress = { id ->
                            viewModel.selectAddress(id)
                            navController.popBackStack()
                        },
                        onDeleteAddress = viewModel::deleteAddress,
                        onSaveAddress = viewModel::saveAddress
                    )
                }

                composable(Routes.CHECKOUT) {
                    CheckoutScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onChangeAddress = { navController.navigate(Routes.ADDRESSES) },
                        onSelectPayment = viewModel::selectPaymentMethod,
                        onPlaceOrder = { note, idemKey ->
                            viewModel.placeOrder(note, idemKey) { created ->
                                navController.navigate(Routes.orderSuccess(created.id)) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        },
                        newIdempotencyKey = viewModel::newIdempotencyKey
                    )
                }

                composable(
                    route = Routes.ORDER_SUCCESS,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { backStack ->
                    val oid = backStack.arguments?.getString("orderId").orEmpty()
                    val order = uiState.orders.firstOrNull { it.id == oid }
                    if (order != null) {
                        OrderSuccessScreen(
                            order = order,
                            uiState = uiState,
                            onTrackOrder = {
                                navController.navigate(Routes.orderDetails(order.id)) {
                                    popUpTo(Routes.HOME)
                                }
                            },
                            onContinueShopping = {
                                navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                            }
                        )
                    }
                }

                composable(
                    route = Routes.ORDER_DETAILS,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { backStack ->
                    val oid = backStack.arguments?.getString("orderId").orEmpty()
                    val order = uiState.orders.firstOrNull { it.id == oid }
                    if (order != null) {
                        OrderDetailsScreen(
                            order = order,
                            uiState = uiState,
                            showBackArrow = true,
                            onBack = { navController.popBackStack() },
                            onCancelOrder = viewModel::cancelOrderByCustomer,
                            onReorder = { o -> viewModel.reorder(o) { navController.navigate(Routes.CART) } }
                        )
                    }
                }

                composable(Routes.ORDER_HISTORY) {
                    OrderHistoryScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() },
                        onOpenOrder = { oid -> navController.navigate(Routes.orderDetails(oid)) },
                        onBrowseHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
                    )
                }

                composable(Routes.ADMIN) {
                    if (uiState.isAdmin) {
                        AdminPortalScreen(
                            uiState = uiState,
                            onBack = { navController.popBackStack() },
                            onUpdateShopSettings = viewModel::updateShopSettingsByAdmin,
                            onUpdateOrderStatus = viewModel::updateOrderStatusByAdmin,
                            onSaveProduct = viewModel::saveProductByAdmin,
                            onAddPartner = viewModel::addDeliveryPartnerByAdmin,
                            onSyncNeon = viewModel::syncNeonNow
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }
        }
    }
}
