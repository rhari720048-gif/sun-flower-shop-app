package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure integer-paise money formatting utilities (Rule #4: no floating-point money math).
 */
object MoneyFormatter {
    fun formatPaise(paise: Int): String {
        val safePaise = paise.coerceAtLeast(0)
        val rupees = safePaise / 100
        val remPaise = safePaise % 100
        val groupedRupees = formatIndianGrouping(rupees)
        return if (remPaise == 0) {
            "₹$groupedRupees"
        } else {
            "₹$groupedRupees.${remPaise.toString().padStart(2, '0')}"
        }
    }

    /**
     * Integer conversion to Cashfree rupees string with 2 decimal places (06-security.md §6).
     */
    fun toCashfreeRupeesString(paise: Int): String {
        val safe = paise.coerceAtLeast(0)
        val rupees = safe / 100
        val rem = safe % 100
        return "$rupees.${rem.toString().padStart(2, '0')}"
    }

    fun discountPercent(pricePaise: Int, mrpPaise: Int?): Int? {
        if (mrpPaise == null || mrpPaise <= pricePaise || mrpPaise <= 0) return null
        val diff = (mrpPaise - pricePaise).toLong()
        return ((diff * 100L) / mrpPaise.toLong()).toInt().takeIf { it > 0 }
    }

    private fun formatIndianGrouping(amount: Int): String {
        val s = amount.toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val groupedRest = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            groupedRest.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                groupedRest.append(',')
            }
        }
        return groupedRest.reverse().toString() + "," + lastThree
    }
}

/**
 * Pure Haversine aerial distance & strict delivery radius checks (Rule #3).
 */
object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val raw = EARTH_RADIUS_KM * c
        return (raw * 100.0).roundToInt() / 100.0
    }

    fun isWithinRadius(distanceKm: Double, deliveryRadiusKm: Double): Boolean {
        return distanceKm <= deliveryRadiusKm
    }
}

/**
 * Computed effective shop status without cron jobs (01-architecture.md §6 & Rule #1, #2).
 */
object ShopStatusEngine {
    const val MAX_CLOSED_MESSAGE_LENGTH = 300
    const val DEFAULT_CLOSED_MESSAGE_EN = "Shop is currently closed. We will reopen soon."
    const val DEFAULT_CLOSED_MESSAGE_TA = "கடை தற்போது மூடப்பட்டுள்ளது. விரைவில் மீண்டும் திறக்கப்படும்."

    fun computeEffectiveStatus(
        settings: ShopSettings,
        serverNowEpochMillis: Long,
        language: AppLanguage = AppLanguage.EN
    ): EffectiveShopStatus {
        val autoReopened = settings.reopenAtEpochMillis != null &&
            settings.reopenAtEpochMillis <= serverNowEpochMillis
        val effectiveOpen = settings.isOpen || autoReopened
        val message = when {
            effectiveOpen -> ""
            settings.closedMessage.isNotBlank() -> settings.closedMessage.trim()
            language == AppLanguage.TA -> DEFAULT_CLOSED_MESSAGE_TA
            else -> DEFAULT_CLOSED_MESSAGE_EN
        }
        return EffectiveShopStatus(
            effectiveOpen = effectiveOpen,
            rawIsOpen = settings.isOpen,
            displayMessage = message,
            reopenAtEpochMillis = if (effectiveOpen) null else settings.reopenAtEpochMillis,
            serverTimeEpochMillis = serverNowEpochMillis
        )
    }

    fun validateClosedMessage(message: String): String? {
        if (message.length > MAX_CLOSED_MESSAGE_LENGTH) {
            return "Closed message must be $MAX_CLOSED_MESSAGE_LENGTH characters or fewer."
        }
        return null
    }

    fun formatReopenTextIst(reopenAtEpochMillis: Long, language: AppLanguage): String {
        val sdf = SimpleDateFormat("dd MMM, h:mm a", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val formatted = sdf.format(Date(reopenAtEpochMillis))
        return if (language == AppLanguage.TA) {
            "மீண்டும் திறக்கும் நேரம்: $formatted"
        } else {
            "Reopens on $formatted"
        }
    }
}

/**
 * Server-authoritative integer-paise pricing & cart validation (Rule #4, #5, #7, #8, #12).
 */
object PricingEngine {
    const val MAX_ITEM_QUANTITY = 50
    const val MAX_SAVED_ADDRESSES = 5

    fun calculatePricing(
        items: List<CartItem>,
        settings: ShopSettings
    ): PricingBreakdown {
        val subtotal = items.sumOf { it.product.pricePaise * it.quantity.coerceIn(0, MAX_ITEM_QUANTITY) }
        val freeThreshold = settings.freeDeliveryAbovePaise
        val freeUnlocked = subtotal > 0 && freeThreshold != null && subtotal >= freeThreshold
        val delivery = when {
            subtotal == 0 -> 0
            freeUnlocked -> 0
            else -> settings.deliveryChargePaise.coerceAtLeast(0)
        }
        // Rule #12: Shop is NOT GST registered; taxPercent == 0 => tax = 0 and never displayed
        val taxBasisPoints = (settings.taxPercent * 100.0).roundToInt().coerceAtLeast(0)
        val tax = if (taxBasisPoints == 0) {
            0
        } else {
            ((subtotal.toLong() * taxBasisPoints.toLong()) + 5000L).div(10000L).toInt()
        }
        val amountForFree = if (freeThreshold != null && subtotal < freeThreshold) {
            (freeThreshold - subtotal).coerceAtLeast(0)
        } else {
            0
        }
        return PricingBreakdown(
            subtotalPaise = subtotal,
            deliveryChargePaise = delivery,
            taxPaise = tax,
            totalPaise = subtotal + delivery + tax,
            showTaxRow = taxBasisPoints > 0 && tax > 0,
            freeDeliveryUnlocked = freeUnlocked,
            amountForFreeDeliveryPaise = amountForFree
        )
    }

    fun validateCart(
        items: List<CartItem>,
        address: SavedAddress?,
        paymentMethod: PaymentMethod,
        settings: ShopSettings,
        serverNowEpochMillis: Long,
        language: AppLanguage = AppLanguage.EN
    ): CartValidationResult {
        val shopStatus = ShopStatusEngine.computeEffectiveStatus(settings, serverNowEpochMillis, language)
        val pricing = calculatePricing(items, settings)
        val outOfStockIds = items.filter { !it.product.inStock || !it.product.isActive }
            .map { it.product.id }
            .toSet()

        val blockers = buildList {
            if (!shopStatus.effectiveOpen) add(CheckoutBlocker.SHOP_CLOSED)
            if (items.isEmpty()) add(CheckoutBlocker.EMPTY_CART)
            if (outOfStockIds.isNotEmpty()) add(CheckoutBlocker.OUT_OF_STOCK)
            if (items.isNotEmpty() && pricing.subtotalPaise < settings.minOrderPaise) {
                add(CheckoutBlocker.BELOW_MIN_ORDER)
            }
            if (address == null) {
                add(CheckoutBlocker.NO_ADDRESS)
            } else {
                val recomputedDist = GeoUtils.haversineKm(
                    settings.shopLat,
                    settings.shopLng,
                    address.lat,
                    address.lng
                )
                if (!GeoUtils.isWithinRadius(recomputedDist, settings.deliveryRadiusKm)) {
                    add(CheckoutBlocker.OUT_OF_RADIUS)
                }
            }
            if (paymentMethod == PaymentMethod.COD && !settings.codEnabled) {
                add(CheckoutBlocker.PAYMENT_METHOD_DISABLED)
            }
            if (paymentMethod == PaymentMethod.ONLINE && !settings.onlinePaymentEnabled) {
                add(CheckoutBlocker.PAYMENT_METHOD_DISABLED)
            }
        }

        return CartValidationResult(
            items = items,
            pricing = pricing,
            shopStatus = shopStatus,
            outOfStockProductIds = outOfStockIds,
            blockers = blockers
        )
    }
}

/**
 * Authoritative Order Status State Machine (Rule #6).
 */
object OrderStateMachine {
    private val allowedTransitions: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.PLACED to setOf(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING to setOf(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED),
        OrderStatus.OUT_FOR_DELIVERY to setOf(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED to emptySet(),
        OrderStatus.CANCELLED to emptySet()
    )

    fun canTransition(from: OrderStatus, to: OrderStatus): Boolean {
        return allowedTransitions[from]?.contains(to) == true
    }

    fun canCustomerCancel(status: OrderStatus): Boolean = status == OrderStatus.PLACED

    fun canAdminCancel(status: OrderStatus): Boolean =
        status == OrderStatus.PLACED || status == OrderStatus.PREPARING

    fun validateAdminTransition(
        from: OrderStatus,
        to: OrderStatus,
        deliveryPartnerId: String?,
        cancelReason: String?
    ): String? {
        if (!canTransition(from, to)) {
            return "Cannot transition order from ${from.dbValue} to ${to.dbValue}."
        }
        if (to == OrderStatus.OUT_FOR_DELIVERY && deliveryPartnerId.isNullOrBlank()) {
            return "Please assign a delivery partner before marking Out for Delivery."
        }
        if (to == OrderStatus.CANCELLED && cancelReason.isNullOrBlank()) {
            return "Please provide a cancellation reason."
        }
        return null
    }
}

object Validators {
    private val indianPhoneRegex = Regex("^(\\+91)?[6-9][0-9]{9}$")
    private val pincodeRegex = Regex("^[0-9]{6}$")

    fun isValidIndianPhone(input: String): Boolean {
        val cleaned = input.replace(" ", "").replace("-", "")
        return indianPhoneRegex.matches(cleaned)
    }

    fun normalizeIndianPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        val ten = if (digits.length > 10) digits.takeLast(10) else digits
        return "+91$ten"
    }

    fun isValidPincode(input: String): Boolean = pincodeRegex.matches(input.trim())
}
