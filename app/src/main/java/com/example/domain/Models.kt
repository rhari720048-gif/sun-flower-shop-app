package com.example.domain

enum class AppLanguage(val code: String, val nativeLabel: String) {
    EN("en", "English"),
    TA("ta", "தமிழ்");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

enum class UserRole(val dbValue: String) {
    CUSTOMER("customer"),
    ADMIN("admin");

    companion object {
        fun fromDb(value: String?): UserRole =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: CUSTOMER
    }
}

enum class OrderStatus(val dbValue: String, val stepIndex: Int) {
    PLACED("placed", 0),
    PREPARING("preparing", 1),
    OUT_FOR_DELIVERY("out_for_delivery", 2),
    DELIVERED("delivered", 3),
    CANCELLED("cancelled", -1);

    companion object {
        fun fromDb(value: String?): OrderStatus =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PLACED
    }
}

enum class PaymentMethod(val dbValue: String) {
    COD("cod"),
    ONLINE("online");

    companion object {
        fun fromDb(value: String?): PaymentMethod =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: COD
    }
}

enum class PaymentStatus(val dbValue: String) {
    PENDING("pending"),
    PAID("paid"),
    FAILED("failed"),
    REFUND_PENDING("refund_pending"),
    REFUNDED("refunded");

    companion object {
        fun fromDb(value: String?): PaymentStatus =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class UserProfile(
    val id: String,
    val firebaseUid: String,
    val name: String,
    val phone: String,
    val email: String?,
    val role: UserRole,
    val language: AppLanguage = AppLanguage.EN,
    val isDeleted: Boolean = false
)

data class Category(
    val id: String,
    val nameEn: String,
    val nameTa: String,
    val imageKey: String,
    val sortOrder: Int,
    val isActive: Boolean = true
) {
    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.TA && nameTa.isNotBlank()) nameTa else nameEn
}

data class Product(
    val id: String,
    val categoryId: String,
    val nameEn: String,
    val nameTa: String,
    val descriptionEn: String,
    val descriptionTa: String,
    val pricePaise: Int,
    val mrpPaise: Int?,
    val imageKey: String,
    val unitLabel: String,
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val isActive: Boolean = true
) {
    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.TA && nameTa.isNotBlank()) nameTa else nameEn

    fun localizedDescription(language: AppLanguage): String =
        if (language == AppLanguage.TA && descriptionTa.isNotBlank()) descriptionTa else descriptionEn
}

data class BannerItem(
    val id: String,
    val titleEn: String,
    val titleTa: String,
    val subtitleEn: String,
    val subtitleTa: String,
    val imageKey: String,
    val targetCategoryId: String?,
    val sortOrder: Int,
    val isActive: Boolean = true
) {
    fun localizedTitle(language: AppLanguage): String =
        if (language == AppLanguage.TA && titleTa.isNotBlank()) titleTa else titleEn
}

data class SavedAddress(
    val id: String,
    val userId: String,
    val label: String, // home | work | other
    val recipientName: String,
    val recipientPhone: String,
    val line1: String,
    val line2: String,
    val landmark: String,
    val pincode: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val isDefault: Boolean
)

data class DeliveryPartner(
    val id: String,
    val name: String,
    val phone: String,
    val rating: Double,
    val isActive: Boolean = true
)

data class ShopSettings(
    val isOpen: Boolean = true,
    val closedMessage: String = "",
    val reopenAtEpochMillis: Long? = null,
    val shopName: String = "Sun Flowers and Decoration",
    val shopAddress: String = "42 Flower Bazaar Road, RS Puram, Coimbatore, Tamil Nadu 641002",
    val shopLat: Double = 11.0081,
    val shopLng: Double = 76.9510,
    val deliveryRadiusKm: Double = 6.0,
    val deliveryChargePaise: Int = 4000,
    val freeDeliveryAbovePaise: Int? = 99900,
    val minOrderPaise: Int = 19900,
    // Rule #12: Shop is NOT GST registered; tax stays 0 and is never shown unless > 0
    val taxPercent: Double = 0.0,
    val estimatedDeliveryMinutes: Int = 25,
    val supportPhone: String = "+919842212345",
    val supportWhatsapp: String = "919842212345",
    val codEnabled: Boolean = true,
    val onlinePaymentEnabled: Boolean = true,
    val updatedByName: String = "Shop Owner",
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

data class EffectiveShopStatus(
    val effectiveOpen: Boolean,
    val rawIsOpen: Boolean,
    val displayMessage: String,
    val reopenAtEpochMillis: Long?,
    val serverTimeEpochMillis: Long
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class PricingBreakdown(
    val subtotalPaise: Int,
    val deliveryChargePaise: Int,
    val taxPaise: Int,
    val totalPaise: Int,
    val showTaxRow: Boolean,
    val freeDeliveryUnlocked: Boolean,
    val amountForFreeDeliveryPaise: Int
)

enum class CheckoutBlocker {
    SHOP_CLOSED,
    EMPTY_CART,
    OUT_OF_STOCK,
    BELOW_MIN_ORDER,
    NO_ADDRESS,
    OUT_OF_RADIUS,
    PAYMENT_METHOD_DISABLED
}

data class CartValidationResult(
    val items: List<CartItem>,
    val pricing: PricingBreakdown,
    val shopStatus: EffectiveShopStatus,
    val outOfStockProductIds: Set<String>,
    val blockers: List<CheckoutBlocker>
) {
    val canCheckout: Boolean get() = blockers.isEmpty()
}

data class OrderItem(
    val id: String,
    val productId: String,
    val nameSnapshot: String,
    val unitLabelSnapshot: String,
    val imageKey: String,
    val unitPricePaise: Int,
    val quantity: Int,
    val lineTotalPaise: Int
)

data class OrderStatusHistoryEntry(
    val status: OrderStatus,
    val changedBy: String,
    val note: String?,
    val timestampEpochMillis: Long
)

data class OrderRecord(
    val id: String,
    val orderNumber: Int,
    val userId: String,
    val idempotencyKey: String,
    val customerName: String,
    val customerPhone: String,
    val status: OrderStatus,
    val addressSummary: String,
    val addressLat: Double,
    val addressLng: Double,
    val distanceKm: Double,
    val subtotalPaise: Int,
    val deliveryChargePaise: Int,
    val taxPaise: Int,
    val totalPaise: Int,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val deliveryPartner: DeliveryPartner?,
    val customerNote: String?,
    val cancelReason: String?,
    val cancelledBy: String?,
    val estimatedMinutes: Int,
    val placedAtEpochMillis: Long,
    val outForDeliveryAtEpochMillis: Long?,
    val deliveredAtEpochMillis: Long?,
    val items: List<OrderItem>,
    val history: List<OrderStatusHistoryEntry>
) {
    val formattedOrderNumber: String
        get() = "SF-${orderNumber.toString().padStart(6, '0')}"
}

data class AdminAuditEntry(
    val id: String,
    val adminName: String,
    val action: String,
    val entity: String,
    val summary: String,
    val timestampEpochMillis: Long
)

data class DailyMetric(
    val dayLabel: String,
    val orderCount: Int,
    val revenuePaise: Int
)

data class AdminDashboardMetrics(
    val todayOrdersCount: Int,
    val todayRevenuePaise: Int,
    val pendingCount: Int,
    val preparingCount: Int,
    val outForDeliveryCount: Int,
    val last7Days: List<DailyMetric>
)

data class NeonConnectionStatus(
    val isConfigured: Boolean,
    val isConnected: Boolean,
    val hostMasked: String,
    val lastSyncMessage: String
)
