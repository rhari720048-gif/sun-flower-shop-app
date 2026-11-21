package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AddressEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.DeliveryPartnerEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderStatusHistoryEntity
import com.example.data.local.ProductEntity
import com.example.data.local.ShopSettingsEntity
import com.example.data.local.SunFlowersDao
import com.example.data.local.SunFlowersDatabase
import com.example.data.local.WishlistEntity
import com.example.data.neon.NeonSqlClient
import com.example.domain.AdminAuditEntry
import com.example.domain.AdminDashboardMetrics
import com.example.domain.AppLanguage
import com.example.domain.BannerItem
import com.example.domain.CartItem
import com.example.domain.Category
import com.example.domain.DailyMetric
import com.example.domain.DeliveryPartner
import com.example.domain.GeoUtils
import com.example.domain.NeonConnectionStatus
import com.example.domain.OrderItem
import com.example.domain.OrderRecord
import com.example.domain.OrderStateMachine
import com.example.domain.OrderStatus
import com.example.domain.OrderStatusHistoryEntry
import com.example.domain.PaymentMethod
import com.example.domain.PaymentStatus
import com.example.domain.PricingEngine
import com.example.domain.Product
import com.example.domain.SavedAddress
import com.example.domain.ShopSettings
import com.example.domain.ShopStatusEngine
import com.example.domain.UserProfile
import com.example.domain.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private val Context.sunFlowersPrefs by preferencesDataStore(name = "sunflowers_prefs")

class SunFlowersRepository(
    private val context: Context,
    private val dao: SunFlowersDao = SunFlowersDatabase.getInstance(context).dao(),
    private val neonClient: NeonSqlClient = NeonSqlClient()
) {
    private val keyLanguage = stringPreferencesKey("language_code")
    private val keyCartJson = stringPreferencesKey("cart_items_json")

    private val _neonStatus = MutableStateFlow(
        NeonConnectionStatus(
            isConfigured = neonClient.isConfigured,
            isConnected = false,
            hostMasked = neonClient.maskedHost(),
            lastSyncMessage = if (neonClient.isConfigured) "Connecting to Neon..." else "Local Cache (Add NEON_DATABASE_URL in Secrets)"
        )
    )
    val neonStatus: StateFlow<NeonConnectionStatus> = _neonStatus.asStateFlow()

    val languageFlow: Flow<AppLanguage> = context.sunFlowersPrefs.data.map { prefs ->
        AppLanguage.fromCode(prefs[keyLanguage])
    }

    val persistedCartQuantitiesFlow: Flow<Map<String, Int>> = context.sunFlowersPrefs.data.map { prefs ->
        parseCartJson(prefs[keyCartJson].orEmpty())
    }

    val categoriesFlow: Flow<List<Category>> = dao.observeCategories().map { list ->
        list.map { it.toDomain() }
    }

    val activeProductsFlow: Flow<List<Product>> = dao.observeActiveProducts().map { list ->
        list.map { it.toDomain() }
    }

    val allProductsFlow: Flow<List<Product>> = dao.observeAllProducts().map { list ->
        list.map { it.toDomain() }
    }

    val deliveryPartnersFlow: Flow<List<DeliveryPartner>> = dao.observeDeliveryPartners().map { list ->
        list.map { it.toDomain() }
    }

    val shopSettingsFlow: Flow<ShopSettings> = dao.observeShopSettings().map { entity ->
        entity?.toDomain() ?: ShopSettings()
    }

    val auditLogFlow: Flow<List<AdminAuditEntry>> = dao.observeAuditLog().map { list ->
        list.map {
            AdminAuditEntry(
                id = it.id,
                adminName = it.adminName,
                action = it.action,
                entity = it.entity,
                summary = it.summary,
                timestampEpochMillis = it.timestampEpochMillis
            )
        }
    }

    val banners: List<BannerItem> = listOf(
        BannerItem(
            id = "banner_roses",
            titleEn = "Fresh Flowers\nDelivered\nwith Love",
            titleTa = "புதிய மலர்கள்\nஅன்புடன்\nஇல்லம் தேடி",
            subtitleEn = "Hand-tied bouquets in 25 mins",
            subtitleTa = "25 நிமிடங்களில் உங்கள் இல்லத்தில்",
            imageKey = "red_rose_bouquet",
            targetCategoryId = "cat_bouquets",
            sortOrder = 0
        ),
        BannerItem(
            id = "banner_garlands",
            titleEn = "Festive Garlands\n& Wedding\nDecorations",
            titleTa = "திருமண மாலைகள்\nமற்றும் விழா\nஅலங்காரங்கள்",
            subtitleEn = "Fresh Madurai Malli & Rose Maalai",
            subtitleTa = "மதுரை மல்லிகை & ரோஜா மாலைகள்",
            imageKey = "floral_garland",
            targetCategoryId = "cat_garlands",
            sortOrder = 1
        ),
        BannerItem(
            id = "banner_baskets",
            titleEn = "Bloom Baskets\nfor Every\nCelebration",
            titleTa = "ஒவ்வொரு விழாவிற்கும்\nமலர் கூடைகள்",
            subtitleEn = "Free delivery above ₹999",
            subtitleTa = "₹999-க்கு மேல் இலவச டெலிவரி",
            imageKey = "flower_basket",
            targetCategoryId = "cat_baskets",
            sortOrder = 2
        )
    )

    fun observeWishlistIds(userId: String): Flow<Set<String>> =
        dao.observeWishlistIds(userId).map { it.toSet() }

    fun observeAddresses(userId: String): Flow<List<SavedAddress>> =
        dao.observeAddresses(userId).map { list -> list.map { it.toDomain() } }

    fun observeOrders(): Flow<List<OrderRecord>> = combine(
        dao.observeAllOrders(),
        dao.observeAllOrderItems(),
        dao.observeAllStatusHistory(),
        dao.observeDeliveryPartners()
    ) { orders, items, histories, partners ->
        val itemsByOrder = items.groupBy { it.orderId }
        val historyByOrder = histories.groupBy { it.orderId }
        val partnerMap = partners.associateBy { it.id }
        orders.map { entity ->
            OrderRecord(
                id = entity.id,
                orderNumber = entity.orderNumber,
                userId = entity.userId,
                idempotencyKey = entity.idempotencyKey,
                customerName = entity.customerName,
                customerPhone = entity.customerPhone,
                status = OrderStatus.fromDb(entity.status),
                addressSummary = entity.addressSummary,
                addressLat = entity.addressLat,
                addressLng = entity.addressLng,
                distanceKm = entity.distanceKm,
                subtotalPaise = entity.subtotalPaise,
                deliveryChargePaise = entity.deliveryChargePaise,
                taxPaise = entity.taxPaise,
                totalPaise = entity.totalPaise,
                paymentMethod = PaymentMethod.fromDb(entity.paymentMethod),
                paymentStatus = PaymentStatus.fromDb(entity.paymentStatus),
                deliveryPartner = entity.deliveryPartnerId?.let { partnerMap[it]?.toDomain() },
                customerNote = entity.customerNote,
                cancelReason = entity.cancelReason,
                cancelledBy = entity.cancelledBy,
                estimatedMinutes = entity.estimatedMinutes,
                placedAtEpochMillis = entity.placedAtEpochMillis,
                outForDeliveryAtEpochMillis = entity.outForDeliveryAtEpochMillis,
                deliveredAtEpochMillis = entity.deliveredAtEpochMillis,
                items = itemsByOrder[entity.id].orEmpty().map {
                    OrderItem(
                        id = it.id,
                        productId = it.productId,
                        nameSnapshot = it.nameSnapshot,
                        unitLabelSnapshot = it.unitLabelSnapshot,
                        imageKey = it.imageKey,
                        unitPricePaise = it.unitPricePaise,
                        quantity = it.quantity,
                        lineTotalPaise = it.lineTotalPaise
                    )
                },
                history = historyByOrder[entity.id].orEmpty().map {
                    OrderStatusHistoryEntry(
                        status = OrderStatus.fromDb(it.status),
                        changedBy = it.changedBy,
                        note = it.note,
                        timestampEpochMillis = it.timestampEpochMillis
                    )
                }
            )
        }
    }

    suspend fun bootstrapAndSeed(defaultUserId: String) {
        if (dao.countCategories() == 0) {
            seedLocalCatalogAndDemoOrder(defaultUserId)
        }
        reconnectNeonNow()
    }

    suspend fun reconnectNeonNow(): NeonConnectionStatus {
        val status = neonClient.pingAndEnsureSchema()
        _neonStatus.value = status
        if (status.isConnected) {
            syncSeedToNeon()
        }
        return status
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.sunFlowersPrefs.edit { it[keyLanguage] = language.code }
    }

    suspend fun saveCartQuantities(quantities: Map<String, Int>) {
        val arr = JSONArray()
        quantities.filterValues { it > 0 }.forEach { (productId, qty) ->
            arr.put(JSONObject().apply {
                put("productId", productId)
                put("qty", qty.coerceAtMost(PricingEngine.MAX_ITEM_QUANTITY))
            })
        }
        context.sunFlowersPrefs.edit { it[keyCartJson] = arr.toString() }
    }

    suspend fun clearCart() {
        context.sunFlowersPrefs.edit { it[keyCartJson] = "[]" }
    }

    suspend fun toggleWishlist(userId: String, productId: String, currentlyWishlisted: Boolean) {
        if (currentlyWishlisted) {
            dao.removeWishlist(userId, productId)
        } else {
            dao.addWishlist(WishlistEntity(productId = productId, userId = userId, createdAt = System.currentTimeMillis()))
        }
    }

    suspend fun upsertUserInNeon(profile: UserProfile) {
        if (!_neonStatus.value.isConnected) return
        neonClient.execute(
            sql = """
                INSERT INTO users (id, firebase_uid, name, phone, email, role, language)
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (firebase_uid) DO UPDATE
                SET name = EXCLUDED.name, phone = EXCLUDED.phone, language = EXCLUDED.language, updated_at = now()
            """.trimIndent(),
            params = listOf(
                profile.id,
                profile.firebaseUid,
                profile.name,
                profile.phone,
                profile.email,
                profile.role.dbValue,
                profile.language.code
            )
        )
    }

    suspend fun saveAddress(
        userId: String,
        label: String,
        recipientName: String,
        recipientPhone: String,
        line1: String,
        line2: String,
        landmark: String,
        pincode: String,
        lat: Double,
        lng: Double,
        makeDefault: Boolean
    ): Result<SavedAddress> {
        val settings = dao.getShopSettingsOnce()?.toDomain() ?: ShopSettings()
        val existingCount = dao.countAddresses(userId)
        if (existingCount >= PricingEngine.MAX_SAVED_ADDRESSES) {
            return Result.failure(IllegalStateException("Maximum ${PricingEngine.MAX_SAVED_ADDRESSES} saved addresses allowed."))
        }
        val distanceKm = GeoUtils.haversineKm(settings.shopLat, settings.shopLng, lat, lng)
        if (!GeoUtils.isWithinRadius(distanceKm, settings.deliveryRadiusKm)) {
            return Result.failure(
                IllegalArgumentException(
                    "OUT_OF_RADIUS: Location is ${"%.1f".format(distanceKm)} km away (delivery radius ${settings.deliveryRadiusKm} km)."
                )
            )
        }
        val shouldBeDefault = makeDefault || existingCount == 0
        if (shouldBeDefault) {
            dao.clearDefaultAddresses(userId)
        }
        val entity = AddressEntity(
            id = "addr_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            label = label,
            recipientName = recipientName.trim(),
            recipientPhone = recipientPhone.trim(),
            line1 = line1.trim(),
            line2 = line2.trim(),
            landmark = landmark.trim(),
            pincode = pincode.trim(),
            lat = lat,
            lng = lng,
            distanceKm = distanceKm,
            isDefault = shouldBeDefault
        )
        dao.upsertAddress(entity)
        if (_neonStatus.value.isConnected) {
            neonClient.execute(
                sql = """
                    INSERT INTO addresses (id, user_id, label, recipient_name, recipient_phone, line1, line2, landmark, pincode, lat, lng, distance_km, is_default)
                    VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                params = listOf(
                    entity.id, entity.userId, entity.label, entity.recipientName, entity.recipientPhone,
                    entity.line1, entity.line2, entity.landmark, entity.pincode,
                    entity.lat, entity.lng, entity.distanceKm, entity.isDefault
                )
            )
        }
        return Result.success(entity.toDomain())
    }

    suspend fun deleteAddress(userId: String, addressId: String) {
        dao.deleteAddress(addressId, userId)
        if (_neonStatus.value.isConnected) {
            neonClient.execute("DELETE FROM addresses WHERE id = $1 AND user_id = $2", listOf(addressId, userId))
        }
    }

    suspend fun setDefaultAddress(userId: String, addressId: String, currentList: List<SavedAddress>) {
        dao.clearDefaultAddresses(userId)
        val target = currentList.firstOrNull { it.id == addressId } ?: return
        dao.upsertAddress(target.toEntity(isDefaultOverride = true))
    }

    /**
     * Authoritative order placement enforcing idempotency, shop open status, radius, stock, and server pricing.
     */
    suspend fun placeOrder(
        user: UserProfile,
        items: List<CartItem>,
        address: SavedAddress,
        paymentMethod: PaymentMethod,
        customerNote: String?,
        idempotencyKey: String,
        language: AppLanguage
    ): Result<OrderRecord> {
        val existing = dao.findOrderByIdempotencyKey(user.id, idempotencyKey)
        if (existing != null) {
            val allOrders = observeOrders().first()
            allOrders.firstOrNull { it.id == existing.id }?.let { return Result.success(it) }
        }

        val settings = dao.getShopSettingsOnce()?.toDomain() ?: ShopSettings()
        val now = System.currentTimeMillis()
        val validation = PricingEngine.validateCart(
            items = items,
            address = address,
            paymentMethod = paymentMethod,
            settings = settings,
            serverNowEpochMillis = now,
            language = language
        )
        if (!validation.canCheckout) {
            val primaryCode = validation.blockers.first().name
            return Result.failure(IllegalStateException(primaryCode))
        }

        val recomputedDistance = GeoUtils.haversineKm(settings.shopLat, settings.shopLng, address.lat, address.lng)
        val nextNumber = dao.getMaxOrderNumber() + 1
        val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
        val addressSnapshot = buildString {
            append(address.line1)
            if (address.line2.isNotBlank()) append(", ${address.line2}")
            if (address.landmark.isNotBlank()) append(" (${address.landmark})")
            append(" - ${address.pincode}")
        }
        val trimmedNote = customerNote?.trim()?.take(200)?.takeIf { it.isNotBlank() }

        val orderEntity = OrderEntity(
            id = orderId,
            orderNumber = nextNumber,
            userId = user.id,
            idempotencyKey = idempotencyKey,
            customerName = address.recipientName.ifBlank { user.name },
            customerPhone = address.recipientPhone.ifBlank { user.phone },
            status = OrderStatus.PLACED.dbValue,
            addressSummary = addressSnapshot,
            addressLat = address.lat,
            addressLng = address.lng,
            distanceKm = recomputedDistance,
            subtotalPaise = validation.pricing.subtotalPaise,
            deliveryChargePaise = validation.pricing.deliveryChargePaise,
            taxPaise = validation.pricing.taxPaise,
            totalPaise = validation.pricing.totalPaise,
            paymentMethod = paymentMethod.dbValue,
            paymentStatus = if (paymentMethod == PaymentMethod.ONLINE) PaymentStatus.PAID.dbValue else PaymentStatus.PENDING.dbValue,
            deliveryPartnerId = null,
            customerNote = trimmedNote,
            cancelReason = null,
            cancelledBy = null,
            estimatedMinutes = settings.estimatedDeliveryMinutes,
            placedAtEpochMillis = now,
            outForDeliveryAtEpochMillis = null,
            deliveredAtEpochMillis = null
        )

        val itemEntities = items.map { cartItem ->
            OrderItemEntity(
                id = "oi_${UUID.randomUUID().toString().take(8)}",
                orderId = orderId,
                productId = cartItem.product.id,
                nameSnapshot = cartItem.product.nameEn,
                unitLabelSnapshot = cartItem.product.unitLabel,
                imageKey = cartItem.product.imageKey,
                unitPricePaise = cartItem.product.pricePaise,
                quantity = cartItem.quantity,
                lineTotalPaise = cartItem.product.pricePaise * cartItem.quantity
            )
        }

        val historyEntry = OrderStatusHistoryEntity(
            id = "osh_${UUID.randomUUID().toString().take(8)}",
            orderId = orderId,
            status = OrderStatus.PLACED.dbValue,
            changedBy = user.name,
            note = if (paymentMethod == PaymentMethod.ONLINE) "Paid via Online UPI/Card" else "Cash on Delivery",
            timestampEpochMillis = now
        )

        dao.upsertOrder(orderEntity)
        dao.insertOrderItems(itemEntities)
        dao.insertStatusHistory(historyEntry)
        clearCart()

        if (_neonStatus.value.isConnected) {
            neonClient.execute(
                sql = """
                    INSERT INTO orders (
                        id, order_number, user_id, idempotency_key, customer_name, customer_phone,
                        status, address_snapshot, address_lat, address_lng, distance_km,
                        subtotal_paise, delivery_charge_paise, tax_paise, total_paise,
                        payment_method, payment_status, customer_note
                    ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)
                    ON CONFLICT (user_id, idempotency_key) DO NOTHING
                """.trimIndent(),
                params = listOf(
                    orderEntity.id, orderEntity.orderNumber, orderEntity.userId, orderEntity.idempotencyKey,
                    orderEntity.customerName, orderEntity.customerPhone, orderEntity.status,
                    orderEntity.addressSummary, orderEntity.addressLat, orderEntity.addressLng,
                    orderEntity.distanceKm, orderEntity.subtotalPaise, orderEntity.deliveryChargePaise,
                    orderEntity.taxPaise, orderEntity.totalPaise, orderEntity.paymentMethod,
                    orderEntity.paymentStatus, orderEntity.customerNote
                )
            )
        }

        val created = observeOrders().first().first { it.id == orderId }
        return Result.success(created)
    }

    suspend fun cancelOrderByCustomer(order: OrderRecord, reason: String): Result<Unit> {
        if (!OrderStateMachine.canCustomerCancel(order.status)) {
            return Result.failure(IllegalStateException("Orders can only be cancelled before preparation starts."))
        }
        val newPaymentStatus = if (order.paymentMethod == PaymentMethod.ONLINE && order.paymentStatus == PaymentStatus.PAID) {
            PaymentStatus.REFUND_PENDING
        } else {
            order.paymentStatus
        }
        val updated = OrderEntity(
            id = order.id,
            orderNumber = order.orderNumber,
            userId = order.userId,
            idempotencyKey = order.idempotencyKey,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            status = OrderStatus.CANCELLED.dbValue,
            addressSummary = order.addressSummary,
            addressLat = order.addressLat,
            addressLng = order.addressLng,
            distanceKm = order.distanceKm,
            subtotalPaise = order.subtotalPaise,
            deliveryChargePaise = order.deliveryChargePaise,
            taxPaise = order.taxPaise,
            totalPaise = order.totalPaise,
            paymentMethod = order.paymentMethod.dbValue,
            paymentStatus = newPaymentStatus.dbValue,
            deliveryPartnerId = order.deliveryPartner?.id,
            customerNote = order.customerNote,
            cancelReason = reason.ifBlank { "Cancelled by customer" },
            cancelledBy = "customer",
            estimatedMinutes = order.estimatedMinutes,
            placedAtEpochMillis = order.placedAtEpochMillis,
            outForDeliveryAtEpochMillis = order.outForDeliveryAtEpochMillis,
            deliveredAtEpochMillis = order.deliveredAtEpochMillis
        )
        dao.upsertOrder(updated)
        dao.insertStatusHistory(
            OrderStatusHistoryEntity(
                id = "osh_${UUID.randomUUID().toString().take(8)}",
                orderId = order.id,
                status = OrderStatus.CANCELLED.dbValue,
                changedBy = order.customerName,
                note = updated.cancelReason,
                timestampEpochMillis = System.currentTimeMillis()
            )
        )
        syncOrderStatusToNeon(updated)
        return Result.success(Unit)
    }

    suspend fun updateOrderStatusByAdmin(
        adminName: String,
        order: OrderRecord,
        nextStatus: OrderStatus,
        deliveryPartnerId: String?,
        cancelReason: String?
    ): Result<Unit> {
        val error = OrderStateMachine.validateAdminTransition(
            from = order.status,
            to = nextStatus,
            deliveryPartnerId = deliveryPartnerId ?: order.deliveryPartner?.id,
            cancelReason = cancelReason
        )
        if (error != null) return Result.failure(IllegalArgumentException(error))

        val now = System.currentTimeMillis()
        val resolvedPartnerId = deliveryPartnerId ?: order.deliveryPartner?.id
        val nextPaymentStatus = when {
            nextStatus == OrderStatus.DELIVERED && order.paymentMethod == PaymentMethod.COD -> PaymentStatus.PAID
            nextStatus == OrderStatus.CANCELLED && order.paymentMethod == PaymentMethod.ONLINE && order.paymentStatus == PaymentStatus.PAID -> PaymentStatus.REFUND_PENDING
            else -> order.paymentStatus
        }

        val updated = OrderEntity(
            id = order.id,
            orderNumber = order.orderNumber,
            userId = order.userId,
            idempotencyKey = order.idempotencyKey,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            status = nextStatus.dbValue,
            addressSummary = order.addressSummary,
            addressLat = order.addressLat,
            addressLng = order.addressLng,
            distanceKm = order.distanceKm,
            subtotalPaise = order.subtotalPaise,
            deliveryChargePaise = order.deliveryChargePaise,
            taxPaise = order.taxPaise,
            totalPaise = order.totalPaise,
            paymentMethod = order.paymentMethod.dbValue,
            paymentStatus = nextPaymentStatus.dbValue,
            deliveryPartnerId = resolvedPartnerId,
            customerNote = order.customerNote,
            cancelReason = if (nextStatus == OrderStatus.CANCELLED) cancelReason?.trim() else order.cancelReason,
            cancelledBy = if (nextStatus == OrderStatus.CANCELLED) "admin" else order.cancelledBy,
            estimatedMinutes = order.estimatedMinutes,
            placedAtEpochMillis = order.placedAtEpochMillis,
            outForDeliveryAtEpochMillis = if (nextStatus == OrderStatus.OUT_FOR_DELIVERY) now else order.outForDeliveryAtEpochMillis,
            deliveredAtEpochMillis = if (nextStatus == OrderStatus.DELIVERED) now else order.deliveredAtEpochMillis
        )
        dao.upsertOrder(updated)
        dao.insertStatusHistory(
            OrderStatusHistoryEntity(
                id = "osh_${UUID.randomUUID().toString().take(8)}",
                orderId = order.id,
                status = nextStatus.dbValue,
                changedBy = adminName,
                note = cancelReason?.trim(),
                timestampEpochMillis = now
            )
        )
        recordAudit(
            adminName = adminName,
            action = "order.status",
            entity = order.formattedOrderNumber,
            summary = "Changed status ${order.status.dbValue} -> ${nextStatus.dbValue}"
        )
        syncOrderStatusToNeon(updated)
        return Result.success(Unit)
    }

    suspend fun updateShopSettings(adminName: String, updated: ShopSettings): Result<Unit> {
        val messageErr = ShopStatusEngine.validateClosedMessage(updated.closedMessage)
        if (messageErr != null) return Result.failure(IllegalArgumentException(messageErr))
        val safeRadius = updated.deliveryRadiusKm.coerceIn(1.0, 25.0)
        val safeTax = updated.taxPercent.coerceIn(0.0, 28.0)
        val now = System.currentTimeMillis()
        val finalSettings = updated.copy(
            deliveryRadiusKm = safeRadius,
            taxPercent = safeTax,
            updatedByName = adminName,
            updatedAtEpochMillis = now
        )
        dao.upsertShopSettings(finalSettings.toEntity())
        recordAudit(
            adminName = adminName,
            action = "shop.update",
            entity = "shop_settings",
            summary = if (finalSettings.isOpen) "Shop OPEN (radius ${safeRadius} km)" else "Shop CLOSED: ${finalSettings.closedMessage.ifBlank { "Default message" }}"
        )
        if (_neonStatus.value.isConnected) {
            neonClient.execute(
                sql = """
                    INSERT INTO shop_settings (
                        id, is_open, closed_message, shop_name, shop_lat, shop_lng,
                        delivery_radius_km, delivery_charge_paise, free_delivery_above_paise,
                        min_order_paise, tax_percent, estimated_delivery_minutes,
                        support_phone, support_whatsapp, cod_enabled, online_payment_enabled
                    ) VALUES (1,$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
                    ON CONFLICT (id) DO UPDATE SET
                        is_open = EXCLUDED.is_open,
                        closed_message = EXCLUDED.closed_message,
                        delivery_radius_km = EXCLUDED.delivery_radius_km,
                        delivery_charge_paise = EXCLUDED.delivery_charge_paise,
                        free_delivery_above_paise = EXCLUDED.free_delivery_above_paise,
                        min_order_paise = EXCLUDED.min_order_paise,
                        tax_percent = EXCLUDED.tax_percent,
                        estimated_delivery_minutes = EXCLUDED.estimated_delivery_minutes,
                        cod_enabled = EXCLUDED.cod_enabled,
                        online_payment_enabled = EXCLUDED.online_payment_enabled,
                        updated_at = now()
                """.trimIndent(),
                params = listOf(
                    finalSettings.isOpen, finalSettings.closedMessage, finalSettings.shopName,
                    finalSettings.shopLat, finalSettings.shopLng, finalSettings.deliveryRadiusKm,
                    finalSettings.deliveryChargePaise, finalSettings.freeDeliveryAbovePaise,
                    finalSettings.minOrderPaise, finalSettings.taxPercent,
                    finalSettings.estimatedDeliveryMinutes, finalSettings.supportPhone,
                    finalSettings.supportWhatsapp, finalSettings.codEnabled,
                    finalSettings.onlinePaymentEnabled
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun upsertProductByAdmin(adminName: String, product: Product): Result<Unit> {
        if (product.nameEn.isBlank() || product.pricePaise <= 0) {
            return Result.failure(IllegalArgumentException("Product name and positive price are required."))
        }
        dao.upsertProduct(product.toEntity())
        recordAudit(
            adminName = adminName,
            action = "product.save",
            entity = product.nameEn,
            summary = "Saved ${product.nameEn} (₹${product.pricePaise / 100}, inStock=${product.inStock})"
        )
        if (_neonStatus.value.isConnected) {
            neonClient.execute(
                sql = """
                    INSERT INTO products (id, category_id, name_en, name_ta, description_en, description_ta, price_paise, mrp_paise, image_key, unit_label, in_stock, is_featured, is_active)
                    VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
                    ON CONFLICT (id) DO UPDATE SET
                        name_en = EXCLUDED.name_en, name_ta = EXCLUDED.name_ta,
                        price_paise = EXCLUDED.price_paise, in_stock = EXCLUDED.in_stock,
                        is_featured = EXCLUDED.is_featured, is_active = EXCLUDED.is_active, updated_at = now()
                """.trimIndent(),
                params = listOf(
                    product.id, product.categoryId, product.nameEn, product.nameTa,
                    product.descriptionEn, product.descriptionTa, product.pricePaise,
                    product.mrpPaise, product.imageKey, product.unitLabel,
                    product.inStock, product.isFeatured, product.isActive
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun addDeliveryPartner(adminName: String, name: String, phone: String): Result<Unit> {
        if (name.isBlank() || phone.isBlank()) {
            return Result.failure(IllegalArgumentException("Name and phone are required."))
        }
        val partner = DeliveryPartnerEntity(
            id = "dp_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            phone = phone.trim(),
            rating = 4.9,
            isActive = true
        )
        dao.upsertDeliveryPartner(partner)
        recordAudit(adminName, "partner.add", partner.name, "Added delivery partner ${partner.name}")
        return Result.success(Unit)
    }

    /**
     * DPDP Act & Play Store compliant account deletion (Rule #10):
     * Removes addresses & wishlist, anonymizes orders for accounting retention.
     */
    suspend fun deleteUserAccountData(userId: String) {
        dao.deleteAllAddressesForUser(userId)
        dao.clearWishlistForUser(userId)
        dao.anonymizeOrdersForUser(userId)
        clearCart()
        if (_neonStatus.value.isConnected) {
            neonClient.execute("DELETE FROM addresses WHERE user_id = $1", listOf(userId))
            neonClient.execute(
                "UPDATE users SET is_deleted = true, deleted_at = now(), name = 'Deleted User', phone = NULL, email = NULL WHERE id = $1",
                listOf(userId)
            )
        }
    }

    fun computeAdminDashboardMetrics(orders: List<OrderRecord>): AdminDashboardMetrics {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val dayFmt = SimpleDateFormat("EEE", Locale.ENGLISH).apply { timeZone = ist }
        val keyFmt = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).apply { timeZone = ist }
        val todayKey = keyFmt.format(Date())

        val todayOrders = orders.filter { keyFmt.format(Date(it.placedAtEpochMillis)) == todayKey }
        val todayRevenue = todayOrders
            .filter { it.status != OrderStatus.CANCELLED }
            .sumOf { it.totalPaise }

        val cal = Calendar.getInstance(ist)
        val last7 = (6 downTo 0).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val k = keyFmt.format(cal.time)
            val matching = orders.filter { keyFmt.format(Date(it.placedAtEpochMillis)) == k && it.status != OrderStatus.CANCELLED }
            DailyMetric(
                dayLabel = dayFmt.format(cal.time),
                orderCount = matching.size,
                revenuePaise = matching.sumOf { it.totalPaise }
            )
        }

        return AdminDashboardMetrics(
            todayOrdersCount = todayOrders.size,
            todayRevenuePaise = todayRevenue,
            pendingCount = orders.count { it.status == OrderStatus.PLACED },
            preparingCount = orders.count { it.status == OrderStatus.PREPARING },
            outForDeliveryCount = orders.count { it.status == OrderStatus.OUT_FOR_DELIVERY },
            last7Days = last7
        )
    }

    private suspend fun recordAudit(adminName: String, action: String, entity: String, summary: String) {
        val entry = AuditLogEntity(
            id = "aud_${UUID.randomUUID().toString().take(8)}",
            adminName = adminName,
            action = action,
            entity = entity,
            summary = summary,
            timestampEpochMillis = System.currentTimeMillis()
        )
        dao.insertAuditLog(entry)
        if (_neonStatus.value.isConnected) {
            neonClient.execute(
                "INSERT INTO admin_audit_log (id, admin_name, action, entity, summary) VALUES ($1,$2,$3,$4,$5)",
                listOf(entry.id, entry.adminName, entry.action, entry.entity, entry.summary)
            )
        }
    }

    private suspend fun syncOrderStatusToNeon(order: OrderEntity) {
        if (!_neonStatus.value.isConnected) return
        neonClient.execute(
            sql = """
                UPDATE orders
                SET status = $1, payment_status = $2, delivery_partner_id = $3,
                    cancel_reason = $4, cancelled_by = $5, updated_at = now()
                WHERE id = $6
            """.trimIndent(),
            params = listOf(
                order.status, order.paymentStatus, order.deliveryPartnerId,
                order.cancelReason, order.cancelledBy, order.id
            )
        )
    }

    private suspend fun syncSeedToNeon() {
        val settings = dao.getShopSettingsOnce()?.toDomain() ?: return
        neonClient.execute(
            sql = """
                INSERT INTO shop_settings (id, is_open, closed_message, shop_name, shop_lat, shop_lng, delivery_radius_km, delivery_charge_paise, free_delivery_above_paise, min_order_paise, tax_percent, estimated_delivery_minutes, support_phone, support_whatsapp)
                VALUES (1,$1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
                ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            params = listOf(
                settings.isOpen, settings.closedMessage, settings.shopName,
                settings.shopLat, settings.shopLng, settings.deliveryRadiusKm,
                settings.deliveryChargePaise, settings.freeDeliveryAbovePaise,
                settings.minOrderPaise, settings.taxPercent, settings.estimatedDeliveryMinutes,
                settings.supportPhone, settings.supportWhatsapp
            )
        )

        val categories = dao.observeCategories().first()
        categories.forEach { c ->
            neonClient.execute(
                sql = """
                    INSERT INTO categories (id, name_en, name_ta, image_key, sort_order, is_active)
                    VALUES ($1,$2,$3,$4,$5,$6)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                params = listOf(c.id, c.nameEn, c.nameTa, c.imageKey, c.sortOrder, c.isActive)
            )
        }

        val products = dao.observeAllProducts().first()
        products.forEach { p ->
            neonClient.execute(
                sql = """
                    INSERT INTO products (id, category_id, name_en, name_ta, description_en, description_ta, price_paise, mrp_paise, image_key, unit_label, in_stock, is_featured, is_active)
                    VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                params = listOf(
                    p.id, p.categoryId, p.nameEn, p.nameTa, p.descriptionEn, p.descriptionTa,
                    p.pricePaise, p.mrpPaise, p.imageKey, p.unitLabel, p.inStock, p.isFeatured, p.isActive
                )
            )
        }
    }

    private suspend fun seedLocalCatalogAndDemoOrder(defaultUserId: String) {
        val settings = ShopSettings()
        dao.upsertShopSettings(settings.toEntity())

        val categories = listOf(
            CategoryEntity("cat_bouquets", "Bouquets", "பூங்கொத்துகள்", "red_rose_bouquet", 1, true),
            CategoryEntity("cat_arrangements", "Arrangements", "மலர் அலங்காரம்", "flower_basket", 2, true),
            CategoryEntity("cat_garlands", "Garlands", "மாலைகள்", "floral_garland", 3, true),
            CategoryEntity("cat_baskets", "Flower Baskets", "மலர் கூடைகள்", "flower_basket", 4, true),
            CategoryEntity("cat_decorations", "Decorations", "அலங்காரங்கள்", "floral_garland", 5, true)
        )
        dao.upsertCategories(categories)

        val products = listOf(
            ProductEntity(
                id = "prod_red_rose_bouquet",
                categoryId = "cat_bouquets",
                nameEn = "Red Rose Bouquet",
                nameTa = "சிவப்பு ரோஜா பூங்கொத்து",
                descriptionEn = "Hand-tied bouquet of 12 fresh velvet red roses accented with baby's breath and wrapped in crimson satin.",
                descriptionTa = "12 புதிய சிவப்பு ரோஜாக்கள் மற்றும் வெள்ளை மலர்களுடன் அழகாக கட்டப்பட்ட பூங்கொத்து.",
                pricePaise = 49900,
                mrpPaise = 59900,
                imageKey = "red_rose_bouquet",
                unitLabel = "12 Roses",
                inStock = true,
                isFeatured = true,
                isActive = true
            ),
            ProductEntity(
                id = "prod_jasmine_bunch",
                categoryId = "cat_garlands",
                nameEn = "Jasmine Bunch",
                nameTa = "மதுரை மல்லிகை பூ",
                descriptionEn = "Fragrant fresh-picked Madurai jasmine blossoms with lush green leaves, ideal for daily pooja and auspicious occasions.",
                descriptionTa = "நறுமணம் மிக்க புதிய மதுரை மல்லிகை பூக்கள், பூஜை மற்றும் சுப நிகழ்ச்சிகளுக்கு ஏற்றது.",
                pricePaise = 29900,
                mrpPaise = 34900,
                imageKey = "jasmine_bunch",
                unitLabel = "250g Fresh Bunch",
                inStock = true,
                isFeatured = true,
                isActive = true
            ),
            ProductEntity(
                id = "prod_pastel_rose_basket",
                categoryId = "cat_baskets",
                nameEn = "Royal Pastel Rose Basket",
                nameTa = "ராஜ மலர் கூடை",
                descriptionEn = "Abundant woven wicker basket filled with pink, peach, and sunshine yellow garden roses.",
                descriptionTa = "இளஞ்சிவப்பு, மஞ்சள் மற்றும் ஆரஞ்சு ரோஜாக்கள் நிறைந்த அழகிய பிரம்பு மலர் கூடை.",
                pricePaise = 89900,
                mrpPaise = 109900,
                imageKey = "flower_basket",
                unitLabel = "24 Mixed Blooms",
                inStock = true,
                isFeatured = true,
                isActive = true
            ),
            ProductEntity(
                id = "prod_wedding_garland",
                categoryId = "cat_garlands",
                nameEn = "Wedding Rose & Malli Maalai",
                nameTa = "திருமண ரோஜா மல்லிகை மாலை",
                descriptionEn = "Traditional South Indian bridal garland hand-woven with dense jasmine buds and vibrant crimson roses.",
                descriptionTa = "பாரம்பரிய தென்னிந்திய திருமண மாலை — அடர்த்தியான மல்லிகை மொட்டுகள் மற்றும் சிவப்பு ரோஜாக்கள்.",
                pricePaise = 129900,
                mrpPaise = 149900,
                imageKey = "floral_garland",
                unitLabel = "1 Pair (3.5 ft)",
                inStock = true,
                isFeatured = true,
                isActive = true
            ),
            ProductEntity(
                id = "prod_velvet_24_roses",
                categoryId = "cat_bouquets",
                nameEn = "Grand 24 Red Roses",
                nameTa = "24 சிவப்பு ரோஜா பூங்கொத்து",
                descriptionEn = "Two dozen long-stemmed velvety red roses styled for anniversaries and grand surprises.",
                descriptionTa = "திருமண நாள் மற்றும் சிறப்பு கொண்டாட்டங்களுக்கான 24 சிவப்பு ரோஜா பூங்கொத்து.",
                pricePaise = 94900,
                mrpPaise = 114900,
                imageKey = "red_rose_bouquet",
                unitLabel = "24 Roses",
                inStock = true,
                isFeatured = false,
                isActive = true
            ),
            ProductEntity(
                id = "prod_table_bloom_arrangement",
                categoryId = "cat_arrangements",
                nameEn = "Garden Rose Table Arrangement",
                nameTa = "மேசை மலர் அலங்காரம்",
                descriptionEn = "Lush centerpiece arrangement with peach, cream, and pink roses for home and office celebrations.",
                descriptionTa = "வீடு மற்றும் அலுவலக விழாக்களுக்கான வண்ணமயமான ரோஜா மேசை அலங்காரம்.",
                pricePaise = 74900,
                mrpPaise = 89900,
                imageKey = "flower_basket",
                unitLabel = "18 Mixed Roses",
                inStock = true,
                isFeatured = false,
                isActive = true
            ),
            ProductEntity(
                id = "prod_pooja_malli_string",
                categoryId = "cat_garlands",
                nameEn = "Temple Jasmine String",
                nameTa = "கோவில் மல்லிகை சரம்",
                descriptionEn = "Tightly tied fragrant white jasmine string crafted fresh every morning.",
                descriptionTa = "தினமும் காலையில் புதிதாக கட்டப்படும் நறுமண மல்லிகை சரம்.",
                pricePaise = 19900,
                mrpPaise = null,
                imageKey = "jasmine_bunch",
                unitLabel = "3 Mulam",
                inStock = true,
                isFeatured = false,
                isActive = true
            ),
            ProductEntity(
                id = "prod_festive_arch_decor",
                categoryId = "cat_decorations",
                nameEn = "Festive Entrance Floral Arch",
                nameTa = "விழா நுழைவாயில் மலர் அலங்காரம்",
                descriptionEn = "Fresh marigold, jasmine, and rose doorway floral arch setup for housewarmings and weddings.",
                descriptionTa = "கிரஹப்பிரவேசம் மற்றும் திருமணத்திற்கான புதிய மலர் நுழைவாயில் அலங்காரம்.",
                pricePaise = 349900,
                mrpPaise = 399900,
                imageKey = "floral_garland",
                unitLabel = "Standard Doorway Setup",
                inStock = true,
                isFeatured = false,
                isActive = true
            )
        )
        dao.upsertProducts(products)

        val partners = listOf(
            DeliveryPartnerEntity("dp_karthik", "Karthik", "+919842290011", 4.8, true),
            DeliveryPartnerEntity("dp_arjun", "Arjun", "+919842290022", 4.9, true),
            DeliveryPartnerEntity("dp_vignesh", "Vignesh", "+919842290033", 4.7, true)
        )
        dao.upsertDeliveryPartners(partners)

        val sampleAddress = AddressEntity(
            id = "addr_home_default",
            userId = defaultUserId,
            label = "home",
            recipientName = "Priya Sundaram",
            recipientPhone = "+919842211223",
            line1 = "18 Diwan Bahadur Road, RS Puram",
            line2 = "Near Gandhi Park",
            landmark = "Opposite Heritage Bakery",
            pincode = "641002",
            lat = 11.0145,
            lng = 76.9562,
            distanceKm = GeoUtils.haversineKm(settings.shopLat, settings.shopLng, 11.0145, 76.9562),
            isDefault = true
        )
        dao.upsertAddress(sampleAddress)

        // Pre-seed 1 live active order matching Phone 3 in mockup (Karthik, Red Rose Bouquet ₹499 + Delivery ₹40)
        val now = System.currentTimeMillis()
        val demoOrderId = "ord_sf_demo_101"
        dao.upsertOrder(
            OrderEntity(
                id = demoOrderId,
                orderNumber = 101,
                userId = defaultUserId,
                idempotencyKey = "seed-order-101",
                customerName = "Priya Sundaram",
                customerPhone = "+919842211223",
                status = OrderStatus.OUT_FOR_DELIVERY.dbValue,
                addressSummary = "18 Diwan Bahadur Road, RS Puram, Near Gandhi Park - 641002",
                addressLat = 11.0145,
                addressLng = 76.9562,
                distanceKm = sampleAddress.distanceKm,
                subtotalPaise = 49900,
                deliveryChargePaise = 4000,
                taxPaise = 0, // Rule #12: shop is not GST registered => 0
                totalPaise = 53900,
                paymentMethod = PaymentMethod.COD.dbValue,
                paymentStatus = PaymentStatus.PENDING.dbValue,
                deliveryPartnerId = "dp_karthik",
                customerNote = "Please hand deliver with care",
                cancelReason = null,
                cancelledBy = null,
                estimatedMinutes = 25,
                placedAtEpochMillis = now - 18 * 60_000L,
                outForDeliveryAtEpochMillis = now - 4 * 60_000L,
                deliveredAtEpochMillis = null
            )
        )
        dao.insertOrderItems(
            listOf(
                OrderItemEntity(
                    id = "oi_demo_101_1",
                    orderId = demoOrderId,
                    productId = "prod_red_rose_bouquet",
                    nameSnapshot = "Red Rose Bouquet",
                    unitLabelSnapshot = "12 Roses",
                    imageKey = "red_rose_bouquet",
                    unitPricePaise = 49900,
                    quantity = 1,
                    lineTotalPaise = 49900
                )
            )
        )
        dao.insertStatusHistory(
            OrderStatusHistoryEntity("osh_1", demoOrderId, OrderStatus.PLACED.dbValue, "Customer", "Order received", now - 18 * 60_000L)
        )
        dao.insertStatusHistory(
            OrderStatusHistoryEntity("osh_2", demoOrderId, OrderStatus.PREPARING.dbValue, "Shop Owner", "Hand-tying 12 fresh red roses", now - 12 * 60_000L)
        )
        dao.insertStatusHistory(
            OrderStatusHistoryEntity("osh_3", demoOrderId, OrderStatus.OUT_FOR_DELIVERY.dbValue, "Shop Owner", "Assigned to Karthik", now - 4 * 60_000L)
        )
        dao.insertAuditLog(
            AuditLogEntity(
                id = "aud_seed_1",
                adminName = "Shop Owner",
                action = "shop.init",
                entity = "shop_settings",
                summary = "Initialized Sun Flowers shop (6.0 km radius, GST 0%)",
                timestampEpochMillis = now - 3600_000L
            )
        )
    }

    private fun parseCartJson(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("productId")
                    val qty = obj.optInt("qty", 0)
                    if (id.isNotBlank() && qty > 0) put(id, qty)
                }
            }
        }.getOrDefault(emptyMap())
    }
}

private fun CategoryEntity.toDomain() = Category(id, nameEn, nameTa, imageKey, sortOrder, isActive)
private fun ProductEntity.toDomain() = Product(id, categoryId, nameEn, nameTa, descriptionEn, descriptionTa, pricePaise, mrpPaise, imageKey, unitLabel, inStock, isFeatured, isActive)
private fun Product.toEntity() = ProductEntity(id, categoryId, nameEn, nameTa, descriptionEn, descriptionTa, pricePaise, mrpPaise, imageKey, unitLabel, inStock, isFeatured, isActive)
private fun DeliveryPartnerEntity.toDomain() = DeliveryPartner(id, name, phone, rating, isActive)
private fun AddressEntity.toDomain() = SavedAddress(id, userId, label, recipientName, recipientPhone, line1, line2, landmark, pincode, lat, lng, distanceKm, isDefault)
private fun SavedAddress.toEntity(isDefaultOverride: Boolean = isDefault) = AddressEntity(id, userId, label, recipientName, recipientPhone, line1, line2, landmark, pincode, lat, lng, distanceKm, isDefaultOverride)
private fun ShopSettingsEntity.toDomain() = ShopSettings(
    isOpen = isOpen, closedMessage = closedMessage, reopenAtEpochMillis = reopenAtEpochMillis,
    shopName = shopName, shopAddress = shopAddress, shopLat = shopLat, shopLng = shopLng,
    deliveryRadiusKm = deliveryRadiusKm, deliveryChargePaise = deliveryChargePaise,
    freeDeliveryAbovePaise = freeDeliveryAbovePaise, minOrderPaise = minOrderPaise,
    taxPercent = taxPercent, estimatedDeliveryMinutes = estimatedDeliveryMinutes,
    supportPhone = supportPhone, supportWhatsapp = supportWhatsapp,
    codEnabled = codEnabled, onlinePaymentEnabled = onlinePaymentEnabled,
    updatedByName = updatedByName, updatedAtEpochMillis = updatedAtEpochMillis
)
private fun ShopSettings.toEntity() = ShopSettingsEntity(
    id = 1, isOpen = isOpen, closedMessage = closedMessage, reopenAtEpochMillis = reopenAtEpochMillis,
    shopName = shopName, shopAddress = shopAddress, shopLat = shopLat, shopLng = shopLng,
    deliveryRadiusKm = deliveryRadiusKm, deliveryChargePaise = deliveryChargePaise,
    freeDeliveryAbovePaise = freeDeliveryAbovePaise, minOrderPaise = minOrderPaise,
    taxPercent = taxPercent, estimatedDeliveryMinutes = estimatedDeliveryMinutes,
    supportPhone = supportPhone, supportWhatsapp = supportWhatsapp,
    codEnabled = codEnabled, onlinePaymentEnabled = onlinePaymentEnabled,
    updatedByName = updatedByName, updatedAtEpochMillis = updatedAtEpochMillis
)
