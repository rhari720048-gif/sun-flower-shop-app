package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.FirebaseAuthManager
import com.example.data.auth.FirebaseAuthStatus
import com.example.data.repository.SunFlowersRepository
import com.example.domain.AdminAuditEntry
import com.example.domain.AdminDashboardMetrics
import com.example.domain.AppLanguage
import com.example.domain.BannerItem
import com.example.domain.CartItem
import com.example.domain.CartValidationResult
import com.example.domain.Category
import com.example.domain.DeliveryPartner
import com.example.domain.EffectiveShopStatus
import com.example.domain.NeonConnectionStatus
import com.example.domain.OrderRecord
import com.example.domain.OrderStatus
import com.example.domain.PaymentMethod
import com.example.domain.PricingEngine
import com.example.domain.Product
import com.example.domain.SavedAddress
import com.example.domain.ShopSettings
import com.example.domain.ShopStatusEngine
import com.example.domain.UserProfile
import com.example.domain.UserRole
import com.example.domain.Validators
import com.example.i18n.AppStrings
import com.example.i18n.StringsProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SunFlowersUiState(
    val isSplashVisible: Boolean = true,
    val currentUser: UserProfile? = null,
    val firebaseAuthStatus: FirebaseAuthStatus = FirebaseAuthStatus(false, null, false),
    val authLoading: Boolean = false,
    val authError: String? = null,
    val language: AppLanguage = AppLanguage.EN,
    val strings: AppStrings = StringsProvider.english,
    val shopSettings: ShopSettings = ShopSettings(),
    val effectiveShopStatus: EffectiveShopStatus = ShopStatusEngine.computeEffectiveStatus(
        ShopSettings(), System.currentTimeMillis()
    ),
    val categories: List<Category> = emptyList(),
    val activeProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val banners: List<BannerItem> = emptyList(),
    val wishlistIds: Set<String> = emptySet(),
    val cartQuantities: Map<String, Int> = emptyMap(),
    val cartItems: List<CartItem> = emptyList(),
    val cartValidation: CartValidationResult? = null,
    val addresses: List<SavedAddress> = emptyList(),
    val selectedAddressId: String? = null,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.COD,
    val orders: List<OrderRecord> = emptyList(),
    val deliveryPartners: List<DeliveryPartner> = emptyList(),
    val auditLog: List<AdminAuditEntry> = emptyList(),
    val adminMetrics: AdminDashboardMetrics = AdminDashboardMetrics(0, 0, 0, 0, 0, emptyList()),
    val neonStatus: NeonConnectionStatus = NeonConnectionStatus(false, false, "Not configured", ""),
    val isSubmittingOrder: Boolean = false,
    val toastMessage: String? = null,
    val lastRemovedCartItem: Pair<String, Int>? = null
) {
    val totalCartCount: Int get() = cartQuantities.values.sum()
    val isAdmin: Boolean get() = currentUser?.role == UserRole.ADMIN
    val selectedAddress: SavedAddress?
        get() = addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
    val userOrders: List<OrderRecord>
        get() = currentUser?.let { u -> orders.filter { it.userId == u.id } }.orEmpty()
    val activeUserOrders: List<OrderRecord>
        get() = userOrders.filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
}

private const val DEFAULT_DEMO_USER_ID = "user_customer_demo"

class SunFlowersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SunFlowersRepository(application)
    private val authManager = FirebaseAuthManager(application)

    private val _splashVisible = MutableStateFlow(true)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    private val _authLoading = MutableStateFlow(false)
    private val _authError = MutableStateFlow<String?>(null)
    private val _selectedAddressId = MutableStateFlow<String?>(null)
    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.COD)
    private val _isSubmittingOrder = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _lastRemoved = MutableStateFlow<Pair<String, Int>?>(null)

    private val activeUserIdFlow: StateFlow<String> = _currentUser.combine(_splashVisible) { user, _ ->
        user?.id ?: DEFAULT_DEMO_USER_ID
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_DEMO_USER_ID)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val wishlistFlow = activeUserIdFlow.flatMapLatest { uid ->
        repository.observeWishlistIds(uid)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val addressesFlow = activeUserIdFlow.flatMapLatest { uid ->
        repository.observeAddresses(uid)
    }

    private data class CoreCatalogSlice(
        val language: AppLanguage,
        val settings: ShopSettings,
        val categories: List<Category>,
        val activeProducts: List<Product>,
        val allProducts: List<Product>
    )

    private data class UserDataSlice(
        val wishlist: Set<String>,
        val cartQuantities: Map<String, Int>,
        val addresses: List<SavedAddress>,
        val orders: List<OrderRecord>,
        val partners: List<DeliveryPartner>
    )

    private data class SessionSlice(
        val splash: Boolean,
        val user: UserProfile?,
        val authLoading: Boolean,
        val authError: String?
    )

    private data class UiMetaSlice(
        val selectedAddressId: String?,
        val paymentMethod: PaymentMethod,
        val submittingOrder: Boolean,
        val toast: String?,
        val lastRemoved: Pair<String, Int>?
    )

    private val catalogSliceFlow = combine(
        repository.languageFlow,
        repository.shopSettingsFlow,
        repository.categoriesFlow,
        repository.activeProductsFlow,
        repository.allProductsFlow
    ) { lang, settings, cats, active, all ->
        CoreCatalogSlice(lang, settings, cats, active, all)
    }

    private val userDataSliceFlow = combine(
        wishlistFlow,
        repository.persistedCartQuantitiesFlow,
        addressesFlow,
        repository.observeOrders(),
        repository.deliveryPartnersFlow
    ) { wish, cart, addrs, orders, partners ->
        UserDataSlice(wish, cart, addrs, orders, partners)
    }

    private val sessionSliceFlow = combine(
        _splashVisible,
        _currentUser,
        _authLoading,
        _authError
    ) { splash, user, loading, err ->
        SessionSlice(splash, user, loading, err)
    }

    private val uiMetaSliceFlow = combine(
        _selectedAddressId,
        _selectedPaymentMethod,
        _isSubmittingOrder,
        _toastMessage,
        _lastRemoved
    ) { addrId, pay, sub, toast, removed ->
        UiMetaSlice(addrId, pay, sub, toast, removed)
    }

    val uiState: StateFlow<SunFlowersUiState> = combine(
        catalogSliceFlow,
        userDataSliceFlow,
        sessionSliceFlow,
        uiMetaSliceFlow,
        combine(repository.auditLogFlow, repository.neonStatus) { a, n -> Pair(a, n) }
    ) { cat, userData, session, meta, extra ->
        val (auditLog, neonStatus) = extra
        val now = System.currentTimeMillis()
        val effectiveStatus = ShopStatusEngine.computeEffectiveStatus(cat.settings, now, cat.language)
        val productById = cat.activeProducts.associateBy { it.id }
        val cartItems = userData.cartQuantities.mapNotNull { (pid, qty) ->
            val p = productById[pid] ?: return@mapNotNull null
            if (qty > 0) CartItem(p, qty) else null
        }
        val selectedAddr = userData.addresses.firstOrNull { it.id == meta.selectedAddressId }
            ?: userData.addresses.firstOrNull { it.isDefault }
            ?: userData.addresses.firstOrNull()
        val validation = PricingEngine.validateCart(
            items = cartItems,
            address = selectedAddr,
            paymentMethod = meta.paymentMethod,
            settings = cat.settings,
            serverNowEpochMillis = now,
            language = cat.language
        )

        SunFlowersUiState(
            isSplashVisible = session.splash,
            currentUser = session.user,
            firebaseAuthStatus = authManager.inspectConfiguration(),
            authLoading = session.authLoading,
            authError = session.authError,
            language = cat.language,
            strings = StringsProvider.forLanguage(cat.language),
            shopSettings = cat.settings,
            effectiveShopStatus = effectiveStatus,
            categories = cat.categories,
            activeProducts = cat.activeProducts,
            allProducts = cat.allProducts,
            banners = repository.banners,
            wishlistIds = userData.wishlist,
            cartQuantities = userData.cartQuantities,
            cartItems = cartItems,
            cartValidation = validation,
            addresses = userData.addresses,
            selectedAddressId = selectedAddr?.id,
            selectedPaymentMethod = meta.paymentMethod,
            orders = userData.orders,
            deliveryPartners = userData.partners,
            auditLog = auditLog,
            adminMetrics = repository.computeAdminDashboardMetrics(userData.orders),
            neonStatus = neonStatus,
            isSubmittingOrder = meta.submittingOrder,
            toastMessage = meta.toast,
            lastRemovedCartItem = meta.lastRemoved
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SunFlowersUiState())

    init {
        viewModelScope.launch {
            repository.bootstrapAndSeed(DEFAULT_DEMO_USER_ID)
            val existingFirebase = authManager.getCurrentFirebaseUser()
            if (existingFirebase != null) {
                _currentUser.value = existingFirebase
                repository.upsertUserInNeon(existingFirebase)
            }
            delay(1350L)
            _splashVisible.value = false
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun attemptSilentGoogleSignIn(activity: Activity) {
        if (_currentUser.value != null) return
        viewModelScope.launch {
            val profile = authManager.attemptSilentGoogleSignIn(activity)
            if (profile != null) {
                _currentUser.value = profile
                repository.upsertUserInNeon(profile)
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = authManager.signInWithGoogleInteractive(activity)
            _authLoading.value = false
            res.onSuccess { profile ->
                _currentUser.value = profile
                repository.upsertUserInNeon(profile)
            }.onFailure { err ->
                _authError.value = err.message ?: "Google Sign-In failed"
            }
        }
    }

    /**
     * Allows previewing Customer or Shop Admin role while google-services.json OAuth client is pending.
     */
    fun enterPreviewSession(role: UserRole, customName: String = "") {
        val profile = if (role == UserRole.ADMIN) {
            UserProfile(
                id = DEFAULT_DEMO_USER_ID,
                firebaseUid = "preview_admin_uid",
                name = customName.ifBlank { "Sun Flowers Owner" },
                phone = "+919842212345",
                email = FirebaseAuthManager.OWNER_ADMIN_EMAIL,
                role = UserRole.ADMIN,
                language = uiState.value.language
            )
        } else {
            UserProfile(
                id = DEFAULT_DEMO_USER_ID,
                firebaseUid = "preview_customer_uid",
                name = customName.ifBlank { "Priya Sundaram" },
                phone = "+919842211223",
                email = "priya@sunflowers.in",
                role = UserRole.CUSTOMER,
                language = uiState.value.language
            )
        }
        _currentUser.value = profile
        _authError.value = null
        viewModelScope.launch { repository.upsertUserInNeon(profile) }
    }

    fun updateProfileName(newName: String) {
        val current = _currentUser.value ?: return
        val cleaned = newName.trim()
        if (cleaned.length < 2) {
            _toastMessage.value = "Name must be at least 2 characters."
            return
        }
        val updated = current.copy(name = cleaned)
        _currentUser.value = updated
        viewModelScope.launch {
            repository.upsertUserInNeon(updated)
            _toastMessage.value = "Profile updated"
        }
    }

    fun switchRoleForDemo(toAdmin: Boolean) {
        val current = _currentUser.value ?: return
        _currentUser.value = current.copy(role = if (toAdmin) UserRole.ADMIN else UserRole.CUSTOMER)
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            _currentUser.value = _currentUser.value?.copy(language = language)
        }
    }

    fun toggleWishlist(productId: String) {
        val uid = uiState.value.currentUser?.id ?: DEFAULT_DEMO_USER_ID
        val currently = uiState.value.wishlistIds.contains(productId)
        viewModelScope.launch {
            repository.toggleWishlist(uid, productId, currently)
        }
    }

    fun incrementCart(product: Product) {
        val state = uiState.value
        if (!state.effectiveShopStatus.effectiveOpen) {
            _toastMessage.value = state.effectiveShopStatus.displayMessage.ifBlank { state.strings.shopClosedTitle }
            return
        }
        if (!product.inStock) {
            _toastMessage.value = state.strings.outOfStock
            return
        }
        val currentQty = state.cartQuantities[product.id] ?: 0
        if (currentQty >= PricingEngine.MAX_ITEM_QUANTITY) {
            _toastMessage.value = "Maximum ${PricingEngine.MAX_ITEM_QUANTITY} per item"
            return
        }
        val next = state.cartQuantities.toMutableMap().apply { put(product.id, currentQty + 1) }
        viewModelScope.launch { repository.saveCartQuantities(next) }
    }

    fun decrementCart(productId: String) {
        val state = uiState.value
        val currentQty = state.cartQuantities[productId] ?: return
        val next = state.cartQuantities.toMutableMap()
        if (currentQty <= 1) {
            next.remove(productId)
            _lastRemoved.value = productId to currentQty
        } else {
            next[productId] = currentQty - 1
        }
        viewModelScope.launch { repository.saveCartQuantities(next) }
    }

    fun removeFromCart(productId: String) {
        val state = uiState.value
        val oldQty = state.cartQuantities[productId] ?: return
        val next = state.cartQuantities.toMutableMap().apply { remove(productId) }
        _lastRemoved.value = productId to oldQty
        viewModelScope.launch { repository.saveCartQuantities(next) }
    }

    fun undoRemoveFromCart() {
        val removed = _lastRemoved.value ?: return
        val next = uiState.value.cartQuantities.toMutableMap().apply { put(removed.first, removed.second) }
        _lastRemoved.value = null
        viewModelScope.launch { repository.saveCartQuantities(next) }
    }

    fun selectAddress(addressId: String) {
        _selectedAddressId.value = addressId
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun saveAddress(
        label: String,
        recipientName: String,
        recipientPhone: String,
        line1: String,
        line2: String,
        landmark: String,
        pincode: String,
        lat: Double,
        lng: Double,
        makeDefault: Boolean,
        onSuccess: () -> Unit
    ) {
        val uid = uiState.value.currentUser?.id ?: DEFAULT_DEMO_USER_ID
        if (recipientName.isBlank() || line1.isBlank()) {
            _toastMessage.value = "Recipient name and address line 1 are required."
            return
        }
        if (!Validators.isValidIndianPhone(recipientPhone)) {
            _toastMessage.value = "Enter a valid 10-digit Indian mobile number."
            return
        }
        if (!Validators.isValidPincode(pincode)) {
            _toastMessage.value = "Enter a valid 6-digit pincode."
            return
        }
        viewModelScope.launch {
            val res = repository.saveAddress(
                userId = uid,
                label = label,
                recipientName = recipientName,
                recipientPhone = Validators.normalizeIndianPhone(recipientPhone),
                line1 = line1,
                line2 = line2,
                landmark = landmark,
                pincode = pincode,
                lat = lat,
                lng = lng,
                makeDefault = makeDefault
            )
            res.onSuccess { saved ->
                _selectedAddressId.value = saved.id
                _toastMessage.value = "Delivery address saved (${"%.1f".format(saved.distanceKm)} km)"
                onSuccess()
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Could not save address"
            }
        }
    }

    fun deleteAddress(addressId: String) {
        val uid = uiState.value.currentUser?.id ?: DEFAULT_DEMO_USER_ID
        viewModelScope.launch {
            repository.deleteAddress(uid, addressId)
            _toastMessage.value = "Address removed"
        }
    }

    fun placeOrder(
        customerNote: String,
        idempotencyKey: String,
        onOrderCreated: (OrderRecord) -> Unit
    ) {
        val state = uiState.value
        val user = state.currentUser ?: return
        val address = state.selectedAddress
        if (address == null) {
            _toastMessage.value = "Please select a delivery address."
            return
        }
        if (_isSubmittingOrder.value) return
        viewModelScope.launch {
            _isSubmittingOrder.value = true
            val res = repository.placeOrder(
                user = user,
                items = state.cartItems,
                address = address,
                paymentMethod = state.selectedPaymentMethod,
                customerNote = customerNote,
                idempotencyKey = idempotencyKey,
                language = state.language
            )
            _isSubmittingOrder.value = false
            res.onSuccess { order ->
                onOrderCreated(order)
            }.onFailure { err ->
                _toastMessage.value = when (err.message) {
                    "SHOP_CLOSED" -> state.effectiveShopStatus.displayMessage.ifBlank { state.strings.shopClosedTitle }
                    "OUT_OF_RADIUS" -> "Selected address is outside our ${state.shopSettings.deliveryRadiusKm} km delivery radius."
                    "OUT_OF_STOCK" -> "Some items in your cart are currently out of stock."
                    "BELOW_MIN_ORDER" -> "Minimum order value is ₹${state.shopSettings.minOrderPaise / 100}."
                    else -> err.message ?: "Unable to place order"
                }
            }
        }
    }

    fun cancelOrderByCustomer(order: OrderRecord, reason: String) {
        viewModelScope.launch {
            val res = repository.cancelOrderByCustomer(order, reason)
            res.onSuccess {
                _toastMessage.value = "Order ${order.formattedOrderNumber} cancelled"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Unable to cancel order"
            }
        }
    }

    fun reorder(order: OrderRecord, onReady: () -> Unit) {
        val state = uiState.value
        if (!state.effectiveShopStatus.effectiveOpen) {
            _toastMessage.value = state.effectiveShopStatus.displayMessage.ifBlank { state.strings.shopClosedTitle }
            return
        }
        val activeById = state.activeProducts.associateBy { it.id }
        val next = state.cartQuantities.toMutableMap()
        var added = 0
        order.items.forEach { item ->
            val p = activeById[item.productId]
            if (p != null && p.inStock) {
                next[p.id] = ((next[p.id] ?: 0) + item.quantity).coerceAtMost(PricingEngine.MAX_ITEM_QUANTITY)
                added++
            }
        }
        if (added == 0) {
            _toastMessage.value = "Items from this order are currently unavailable."
            return
        }
        viewModelScope.launch {
            repository.saveCartQuantities(next)
            _toastMessage.value = "Added $added item(s) to cart"
            onReady()
        }
    }

    // --- Admin Actions (Role-Guarded) ---

    fun updateShopSettingsByAdmin(updated: ShopSettings) {
        val user = uiState.value.currentUser
        if (user == null || user.role != UserRole.ADMIN) {
            _toastMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = repository.updateShopSettings(user.name, updated)
            res.onSuccess {
                _toastMessage.value = if (updated.isOpen) "Shop is now OPEN" else "Shop closed & notice live"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Failed to save shop settings"
            }
        }
    }

    fun updateOrderStatusByAdmin(
        order: OrderRecord,
        nextStatus: OrderStatus,
        deliveryPartnerId: String? = null,
        cancelReason: String? = null
    ) {
        val user = uiState.value.currentUser
        if (user == null || user.role != UserRole.ADMIN) {
            _toastMessage.value = "Admin authorization required"
            return
        }
        viewModelScope.launch {
            val res = repository.updateOrderStatusByAdmin(
                adminName = user.name,
                order = order,
                nextStatus = nextStatus,
                deliveryPartnerId = deliveryPartnerId,
                cancelReason = cancelReason
            )
            res.onSuccess {
                _toastMessage.value = "${order.formattedOrderNumber} → ${nextStatus.dbValue.replace('_', ' ')}"
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Invalid status transition"
            }
        }
    }

    fun saveProductByAdmin(product: Product) {
        val user = uiState.value.currentUser
        if (user == null || user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val res = repository.upsertProductByAdmin(user.name, product)
            res.onSuccess { _toastMessage.value = "Saved ${product.nameEn}" }
                .onFailure { _toastMessage.value = it.message ?: "Could not save product" }
        }
    }

    fun addDeliveryPartnerByAdmin(name: String, phone: String) {
        val user = uiState.value.currentUser
        if (user == null || user.role != UserRole.ADMIN) return
        viewModelScope.launch {
            val res = repository.addDeliveryPartner(user.name, name, phone)
            res.onSuccess { _toastMessage.value = "Added partner $name" }
                .onFailure { _toastMessage.value = it.message ?: "Invalid partner details" }
        }
    }

    fun syncNeonNow() {
        viewModelScope.launch {
            val status = repository.reconnectNeonNow()
            _toastMessage.value = status.lastSyncMessage
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.clearCart()
            _currentUser.value = null
            _authError.value = null
        }
    }

    fun deleteAccount() {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.deleteUserAccountData(uid)
            authManager.deleteFirebaseUserIfPresent()
            authManager.signOut()
            _currentUser.value = null
            _toastMessage.value = "Account deleted and personal data removed."
        }
    }

    fun newIdempotencyKey(): String = UUID.randomUUID().toString()
}
