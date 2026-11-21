package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.domain.AppLanguage
import com.example.domain.BannerItem
import com.example.domain.Category
import com.example.domain.DeliveryPartner
import com.example.domain.OrderItem
import com.example.domain.OrderRecord
import com.example.domain.OrderStatus
import com.example.domain.PaymentMethod
import com.example.domain.PaymentStatus
import com.example.domain.Product
import com.example.domain.ShopSettings
import com.example.domain.ShopStatusEngine
import com.example.i18n.StringsProvider
import com.example.ui.SunFlowersUiState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrderDetailsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.SunFlowersTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class SunFlowersCujRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCategories = listOf(
        Category("cat_bouquets", "Bouquets", "பூங்கொத்துகள்", "red_rose_bouquet", 1),
        Category("cat_arrangements", "Arrangements", "மலர் அலங்காரம்", "flower_basket", 2),
        Category("cat_garlands", "Garlands", "மாலைகள்", "floral_garland", 3),
        Category("cat_baskets", "Flower Baskets", "மலர் கூடைகள்", "flower_basket", 4),
        Category("cat_decorations", "Decorations", "அலங்காரங்கள்", "floral_garland", 5)
    )

    private val sampleProducts = listOf(
        Product(
            id = "prod_red_rose_bouquet",
            categoryId = "cat_bouquets",
            nameEn = "Red Rose Bouquet",
            nameTa = "சிவப்பு ரோஜா பூங்கொத்து",
            descriptionEn = "12 fresh velvet red roses",
            descriptionTa = "12 சிவப்பு ரோஜாக்கள்",
            pricePaise = 49900,
            mrpPaise = 59900,
            imageKey = "red_rose_bouquet",
            unitLabel = "12 Roses",
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_jasmine_bunch",
            categoryId = "cat_garlands",
            nameEn = "Jasmine Bunch",
            nameTa = "மதுரை மல்லிகை பூ",
            descriptionEn = "Fresh jasmine",
            descriptionTa = "புதிய மல்லிகை",
            pricePaise = 29900,
            mrpPaise = 34900,
            imageKey = "jasmine_bunch",
            unitLabel = "250g",
            inStock = true,
            isFeatured = true
        )
    )

    @Test
    fun `splash screen renders Sun Flowers branding and tagline`() {
        composeTestRule.setContent {
            SunFlowersTheme {
                SplashScreen(uiState = SunFlowersUiState())
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("splash_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("SUN FLOWERS").assertIsDisplayed()
        composeTestRule.onNodeWithText("AND DECORATION").assertIsDisplayed()
    }

    @Test
    fun `home screen displays mockup categories, special picks, and closed shop banner when closed`() {
        val closedSettings = ShopSettings(isOpen = false, closedMessage = "Reopening tomorrow at 8:00 AM")
        val state = SunFlowersUiState(
            categories = sampleCategories,
            activeProducts = sampleProducts,
            banners = listOf(
                BannerItem("b1", "Fresh Flowers\nDelivered\nwith Love", "புதிய மலர்கள்", "25 mins", "25 நிமிடம்", "red_rose_bouquet", null, 0)
            ),
            shopSettings = closedSettings,
            effectiveShopStatus = ShopStatusEngine.computeEffectiveStatus(closedSettings, System.currentTimeMillis())
        )

        composeTestRule.setContent {
            SunFlowersTheme {
                HomeScreen(
                    uiState = state,
                    onOpenDrawer = {},
                    onOpenCart = {},
                    onOpenSearch = {},
                    onOpenCategory = {},
                    onOpenProduct = {},
                    onToggleWishlist = {},
                    onIncrementCart = {},
                    onDecrementCart = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shop_closed_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reopening tomorrow at 8:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bouquets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Garlands").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_screen").performScrollToNode(hasText("Red Rose Bouquet"))
        composeTestRule.onNodeWithText("Our Special Picks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Red Rose Bouquet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jasmine Bunch").assertIsDisplayed()
    }

    @Test
    fun `order details screen matches Phone 3 and hides GST tax row when taxPaise is zero`() {
        val order = OrderRecord(
            id = "o1",
            orderNumber = 101,
            userId = "u1",
            idempotencyKey = "k1",
            customerName = "Priya",
            customerPhone = "+919842211223",
            status = OrderStatus.OUT_FOR_DELIVERY,
            addressSummary = "18 DB Road, RS Puram",
            addressLat = 11.0145,
            addressLng = 76.9562,
            distanceKm = 1.1,
            subtotalPaise = 49900,
            deliveryChargePaise = 4000,
            taxPaise = 0,
            totalPaise = 53900,
            paymentMethod = PaymentMethod.COD,
            paymentStatus = PaymentStatus.PENDING,
            deliveryPartner = DeliveryPartner("dp1", "Karthik", "+919842290011", 4.8),
            customerNote = null,
            cancelReason = null,
            cancelledBy = null,
            estimatedMinutes = 25,
            placedAtEpochMillis = System.currentTimeMillis(),
            outForDeliveryAtEpochMillis = System.currentTimeMillis(),
            deliveredAtEpochMillis = null,
            items = listOf(
                OrderItem("oi1", "p1", "Red Rose Bouquet", "12 Roses", "red_rose_bouquet", 49900, 1, 49900)
            ),
            history = emptyList()
        )

        composeTestRule.setContent {
            SunFlowersTheme {
                OrderDetailsScreen(
                    order = order,
                    uiState = SunFlowersUiState(),
                    showBackArrow = true,
                    onBack = {},
                    onCancelOrder = { _, _ -> },
                    onReorder = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Order Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Karthik").assertIsDisplayed()
        composeTestRule.onNodeWithText("Order Summary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("contact_support_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Taxes & Fees").assertDoesNotExist()
    }

    @Test
    fun `tamil language switches UI strings cleanly`() {
        val tamilState = SunFlowersUiState(
            language = AppLanguage.TA,
            strings = StringsProvider.forLanguage(AppLanguage.TA)
        )
        composeTestRule.setContent {
            SunFlowersTheme(isTamil = true) {
                SplashScreen(uiState = tamilState)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("சன் ஃபிளவர்ஸ்").assertIsDisplayed()
    }

    @Test
    fun `login screen displays Google Sign-In only without mobile OTP input`() {
        composeTestRule.setContent {
            SunFlowersTheme {
                LoginScreen(
                    uiState = SunFlowersUiState(),
                    onAttemptSilentSignIn = {},
                    onGoogleSignIn = {},
                    onEnterPreview = {},
                    onToggleLanguage = {},
                    onOpenLegal = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_signin_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send OTP").assertDoesNotExist()
        composeTestRule.onNodeWithText("Mobile Number").assertDoesNotExist()
    }
}
