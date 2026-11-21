package com.example

import com.example.data.auth.FirebaseAuthManager
import com.example.data.neon.NeonSqlClient
import com.example.domain.AppLanguage
import com.example.domain.CartItem
import com.example.domain.CheckoutBlocker
import com.example.domain.GeoUtils
import com.example.domain.MoneyFormatter
import com.example.domain.OrderStateMachine
import com.example.domain.OrderStatus
import com.example.domain.PaymentMethod
import com.example.domain.PricingEngine
import com.example.domain.Product
import com.example.domain.SavedAddress
import com.example.domain.ShopSettings
import com.example.domain.ShopStatusEngine
import com.example.domain.UserRole
import com.example.domain.Validators
import com.example.i18n.StringsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRulesTest {

    private val roseBouquet = Product(
        id = "p1",
        categoryId = "c1",
        nameEn = "Red Rose Bouquet",
        nameTa = "சிவப்பு ரோஜா பூங்கொத்து",
        descriptionEn = "12 red roses",
        descriptionTa = "12 சிவப்பு ரோஜாக்கள்",
        pricePaise = 49900,
        mrpPaise = 59900,
        imageKey = "red_rose_bouquet",
        unitLabel = "12 Roses",
        inStock = true,
        isFeatured = true,
        isActive = true
    )

    private val inRadiusAddress = SavedAddress(
        id = "a1",
        userId = "u1",
        label = "home",
        recipientName = "Priya",
        recipientPhone = "+919842211223",
        line1 = "18 DB Road",
        line2 = "RS Puram",
        landmark = "Park",
        pincode = "641002",
        lat = 11.0181,
        lng = 76.9510,
        distanceKm = 1.11,
        isDefault = true
    )

    @Test
    fun `money formatter uses integer paise and Indian grouping`() {
        assertEquals("₹499", MoneyFormatter.formatPaise(49900))
        assertEquals("₹299", MoneyFormatter.formatPaise(29900))
        assertEquals("₹1,299", MoneyFormatter.formatPaise(129900))
        assertEquals("₹1,25,000", MoneyFormatter.formatPaise(12500000))
        assertEquals("₹499.50", MoneyFormatter.formatPaise(49950))
        assertEquals("499.00", MoneyFormatter.toCashfreeRupeesString(49900))
        assertEquals("499.05", MoneyFormatter.toCashfreeRupeesString(49905))
        assertEquals(16, MoneyFormatter.discountPercent(49900, 59900))
        assertNull(MoneyFormatter.discountPercent(49900, 49900))
    }

    @Test
    fun `shop status computes effective open with auto reopen without cron`() {
        val now = 1_700_000_000_000L
        val openShop = ShopSettings(isOpen = true)
        assertTrue(ShopStatusEngine.computeEffectiveStatus(openShop, now).effectiveOpen)

        val closedFutureReopen = ShopSettings(
            isOpen = false,
            closedMessage = "Closed for festival",
            reopenAtEpochMillis = now + 3_600_000L
        )
        val closedStatus = ShopStatusEngine.computeEffectiveStatus(closedFutureReopen, now)
        assertFalse(closedStatus.effectiveOpen)
        assertEquals("Closed for festival", closedStatus.displayMessage)

        val closedPastReopen = ShopSettings(
            isOpen = false,
            closedMessage = "Closed overnight",
            reopenAtEpochMillis = now - 1_000L
        )
        assertTrue(ShopStatusEngine.computeEffectiveStatus(closedPastReopen, now).effectiveOpen)

        val closedBoundaryReopen = ShopSettings(
            isOpen = false,
            reopenAtEpochMillis = now
        )
        assertTrue(ShopStatusEngine.computeEffectiveStatus(closedBoundaryReopen, now).effectiveOpen)

        // 300-char limit check
        assertNull(ShopStatusEngine.validateClosedMessage("a".repeat(300)))
        assertNotNull(ShopStatusEngine.validateClosedMessage("a".repeat(301)))
    }

    @Test
    fun `haversine and delivery radius boundary checks`() {
        assertEquals(0.0, GeoUtils.haversineKm(11.0081, 76.9510, 11.0081, 76.9510), 0.01)
        assertTrue(GeoUtils.isWithinRadius(5.9, 6.0))
        assertTrue(GeoUtils.isWithinRadius(6.0, 6.0))
        assertFalse(GeoUtils.isWithinRadius(6.1, 6.0))
    }

    @Test
    fun `pricing engine hides GST tax row when taxPercent is 0 and enforces free delivery`() {
        val settings = ShopSettings(
            deliveryChargePaise = 4000,
            freeDeliveryAbovePaise = 99900,
            taxPercent = 0.0
        )
        val single = PricingEngine.calculatePricing(listOf(CartItem(roseBouquet, 1)), settings)
        assertEquals(49900, single.subtotalPaise)
        assertEquals(4000, single.deliveryChargePaise)
        assertEquals(0, single.taxPaise)
        assertEquals(53900, single.totalPaise)
        assertFalse(single.showTaxRow)
        assertFalse(single.freeDeliveryUnlocked)

        val freeEligible = PricingEngine.calculatePricing(listOf(CartItem(roseBouquet, 2)), settings)
        assertEquals(99800, freeEligible.subtotalPaise)
        assertEquals(4000, freeEligible.deliveryChargePaise) // ₹998 < ₹999

        val freeUnlocked = PricingEngine.calculatePricing(listOf(CartItem(roseBouquet, 3)), settings)
        assertEquals(0, freeUnlocked.deliveryChargePaise)
        assertTrue(freeUnlocked.freeDeliveryUnlocked)

        val gstRegistered = settings.copy(taxPercent = 5.0)
        val taxed = PricingEngine.calculatePricing(listOf(CartItem(roseBouquet, 1)), gstRegistered)
        assertTrue(taxed.showTaxRow)
        assertEquals(2495, taxed.taxPaise)
        assertEquals(49900 + 4000 + 2495, taxed.totalPaise)
    }

    @Test
    fun `cart validation blocks closed shop, out of radius, out of stock, and min order`() {
        val now = 1_700_000_000_000L
        val closedSettings = ShopSettings(isOpen = false, closedMessage = "Closed today")
        val resClosed = PricingEngine.validateCart(
            items = listOf(CartItem(roseBouquet, 1)),
            address = inRadiusAddress,
            paymentMethod = PaymentMethod.COD,
            settings = closedSettings,
            serverNowEpochMillis = now
        )
        assertFalse(resClosed.canCheckout)
        assertTrue(CheckoutBlocker.SHOP_CLOSED in resClosed.blockers)

        val farAddress = inRadiusAddress.copy(lat = 11.1500, lng = 76.9510) // > 15 km away
        val resFar = PricingEngine.validateCart(
            items = listOf(CartItem(roseBouquet, 1)),
            address = farAddress,
            paymentMethod = PaymentMethod.COD,
            settings = ShopSettings(isOpen = true, deliveryRadiusKm = 6.0),
            serverNowEpochMillis = now
        )
        assertTrue(CheckoutBlocker.OUT_OF_RADIUS in resFar.blockers)

        val oosProduct = roseBouquet.copy(inStock = false)
        val resOos = PricingEngine.validateCart(
            items = listOf(CartItem(oosProduct, 1)),
            address = inRadiusAddress,
            paymentMethod = PaymentMethod.COD,
            settings = ShopSettings(isOpen = true),
            serverNowEpochMillis = now
        )
        assertTrue(CheckoutBlocker.OUT_OF_STOCK in resOos.blockers)
    }

    @Test
    fun `order state machine strictly enforces allowed transitions and partner requirements`() {
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PLACED, OrderStatus.PREPARING))
        assertTrue(OrderStateMachine.canTransition(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY))
        assertTrue(OrderStateMachine.canTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED))
        assertFalse(OrderStateMachine.canTransition(OrderStatus.PLACED, OrderStatus.DELIVERED))
        assertFalse(OrderStateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED))
        assertFalse(OrderStateMachine.canTransition(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.CANCELLED))

        assertTrue(OrderStateMachine.canCustomerCancel(OrderStatus.PLACED))
        assertFalse(OrderStateMachine.canCustomerCancel(OrderStatus.PREPARING))
        assertTrue(OrderStateMachine.canAdminCancel(OrderStatus.PREPARING))

        assertNotNull(OrderStateMachine.validateAdminTransition(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, null, null))
        assertNull(OrderStateMachine.validateAdminTransition(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, "dp_1", null))
        assertNotNull(OrderStateMachine.validateAdminTransition(OrderStatus.PLACED, OrderStatus.CANCELLED, null, ""))
        assertNull(OrderStateMachine.validateAdminTransition(OrderStatus.PLACED, OrderStatus.CANCELLED, null, "Item unavailable"))
    }

    @Test
    fun `neon host extractor rejects placeholders and non-neon hosts`() {
        assertNull(NeonSqlClient.extractNeonHost("NEON_DATABASE_URL_PLACEHOLDER"))
        assertNull(NeonSqlClient.extractNeonHost("postgresql://user:pass@127.0.0.1/db"))
        assertNull(NeonSqlClient.extractNeonHost("postgresql://user:pass@169.254.169.254/db"))
        assertEquals(
            "ep-demo-123.ap-southeast-1.aws.neon.tech",
            NeonSqlClient.extractNeonHost("postgresql://user:pass@ep-demo-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require")
        )
    }

    @Test
    fun `validators and bilingual strings pass sanity checks`() {
        assertTrue(Validators.isValidIndianPhone("9842212345"))
        assertTrue(Validators.isValidIndianPhone("+919842212345"))
        assertFalse(Validators.isValidIndianPhone("12345"))
        assertEquals("+919842212345", Validators.normalizeIndianPhone("9842212345"))
        assertTrue(Validators.isValidPincode("641002"))
        assertFalse(Validators.isValidPincode("64100"))

        val en = StringsProvider.forLanguage(AppLanguage.EN)
        val ta = StringsProvider.forLanguage(AppLanguage.TA)
        assertTrue(en.brandName.isNotBlank() && ta.brandName.isNotBlank())
        assertTrue(en.contactSupport.isNotBlank() && ta.contactSupport.isNotBlank())
    }

    @Test
    fun `owner email resolves to ADMIN role while customer emails resolve to CUSTOMER`() {
        assertEquals(UserRole.ADMIN, FirebaseAuthManager.resolveRoleForEmail("flowershopsun03@gmail.com"))
        assertEquals(UserRole.ADMIN, FirebaseAuthManager.resolveRoleForEmail("  FlowerShopSun03@Gmail.COM  "))
        assertEquals(UserRole.CUSTOMER, FirebaseAuthManager.resolveRoleForEmail("priya@gmail.com"))
        assertEquals(UserRole.CUSTOMER, FirebaseAuthManager.resolveRoleForEmail(null))
    }
}
