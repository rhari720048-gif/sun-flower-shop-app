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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BannerItem
import com.example.domain.Category
import com.example.domain.MoneyFormatter
import com.example.domain.Product
import com.example.ui.SunFlowersUiState
import com.example.ui.components.AnimatedAddOrStepperButton
import com.example.ui.components.AnimatedWishlistHeart
import com.example.ui.components.EmptyFloralState
import com.example.ui.components.FlowerPhoto
import com.example.ui.components.PrimaryPillButton
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.ShopClosedStatusBanner
import com.example.ui.components.SunFlowersEmblem
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
import com.example.ui.theme.PlayfairDisplayFontFamily
import kotlinx.coroutines.delay

/**
 * Home Screen matching Phone 2 of the mockup:
 * Top bar (hamburger, rose-in-diamond logo + SUN FLOWERS AND DECORATION, cart badge),
 * Shop Closed StatusBanner slot, Hero Banner Carousel ("Fresh Flowers Delivered with Love ♥"),
 * 3x2 Category Grid (+ More), and "Our Special Picks" 2-column product cards.
 */
@Composable
fun HomeScreen(
    uiState: SunFlowersUiState,
    onOpenDrawer: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategory: (String?) -> Unit,
    onOpenProduct: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onIncrementCart: (Product) -> Unit,
    onDecrementCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeScale by animateFloatAsState(
        targetValue = if (uiState.totalCartCount > 0) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cart_badge_bounce"
    )

    val specialPicks = remember(uiState.activeProducts) {
        uiState.activeProducts.filter { it.isFeatured }.ifEmpty { uiState.activeProducts.take(4) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Brand Header Bar matching Phone 2
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("home_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open menu",
                        tint = BrandNavy,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onOpenSearch)
                ) {
                    SunFlowersEmblem(size = 38.dp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.strings.brandName,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.8.sp,
                        color = BrandNavy
                    )
                    Text(
                        text = uiState.strings.brandSubtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            letterSpacing = 2.0.sp
                        ),
                        color = BrandPrimary
                    )
                }

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onOpenCart,
                        modifier = Modifier.testTag("home_cart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = "Shopping cart",
                            tint = BrandNavy,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    if (uiState.totalCartCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(20.dp)
                                .scale(badgeScale)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                                .testTag("home_cart_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.totalCartCount.coerceAtMost(99).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BrandSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Shop Closed Status Banner (shown dynamically when Admin closes shop)
        if (!uiState.effectiveShopStatus.effectiveOpen) {
            item(key = "closed_banner") {
                ShopClosedStatusBanner(
                    status = uiState.effectiveShopStatus,
                    language = uiState.language,
                    strings = uiState.strings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 3. Hero Banner Carousel matching Phone 2 ("Fresh Flowers Delivered with Love ♥")
        item(key = "hero_carousel") {
            HeroBannerCarousel(
                banners = uiState.banners,
                language = uiState.language,
                onBannerClick = { banner -> onOpenCategory(banner.targetCategoryId) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 4. 3x2 Category Grid matching Phone 2 (Bouquets, Arrangements, Garlands, Flower Baskets, Decorations, More)
        item(key = "category_grid") {
            val topFive = uiState.categories.take(5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topFive.take(3).forEach { cat ->
                        CategoryGridTile(
                            category = cat,
                            language = uiState.language,
                            onClick = { onOpenCategory(cat.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topFive.drop(3).take(2).forEach { cat ->
                        CategoryGridTile(
                            category = cat,
                            language = uiState.language,
                            onClick = { onOpenCategory(cat.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 6th Tile: "More" with 3 pink dots (exact Phone 2 match)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandPrimarySoft.copy(alpha = 0.65f)),
                        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(114.dp)
                            .pinkCardShadow(elevation = 4.dp, cornerRadius = 16.dp)
                            .clickable { onOpenCategory(null) }
                            .testTag("category_tile_more")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreHoriz,
                                contentDescription = uiState.strings.moreCategory,
                                tint = BrandPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.strings.moreCategory,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. "Our Special Picks" + "View All →"
        item(key = "special_picks_header") {
            SectionHeaderRow(
                title = uiState.strings.ourSpecialPicks,
                actionLabel = uiState.strings.viewAll,
                onActionClick = { onOpenCategory(null) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 6. 2-Column Special Picks Cards (Red Rose Bouquet ₹499, Jasmine Bunch ₹299, ...)
        val rows = specialPicks.chunked(2)
        items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { rowProducts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowProducts.forEach { product ->
                    ProductGridCard(
                        product = product,
                        language = uiState.language,
                        quantityInCart = uiState.cartQuantities[product.id] ?: 0,
                        isWishlisted = uiState.wishlistIds.contains(product.id),
                        shopOpen = uiState.effectiveShopStatus.effectiveOpen,
                        onOpenProduct = { onOpenProduct(product.id) },
                        onToggleWishlist = { onToggleWishlist(product.id) },
                        onIncrement = { onIncrementCart(product) },
                        onDecrement = { onDecrementCart(product.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowProducts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroBannerCarousel(
    banners: List<BannerItem>,
    language: com.example.domain.AppLanguage,
    onBannerClick: (BannerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(pagerState.settledPage) {
        delay(4200L)
        val next = (pagerState.currentPage + 1) % banners.size
        pagerState.animateScrollToPage(next)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        ) { page ->
            val banner = banners[page]
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BrandPrimarySoft),
                modifier = Modifier
                    .fillMaxSize()
                    .pinkCardShadow(elevation = 8.dp, cornerRadius = 18.dp)
                    .clickable { onBannerClick(banner) }
                    .testTag("hero_banner_$page")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFCD8E8),
                                    Color(0xFFFDE6F0),
                                    Color(0xFFF7BDD9)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.48f)
                                .fillMaxHeight()
                        ) {
                            FlowerPhoto(
                                imageKey = banner.imageKey,
                                contentDescription = banner.localizedTitle(language),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Soft pink fade into text area
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(34.dp)
                                    .align(Alignment.CenterEnd)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color(0xFFFDE6F0))
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.52f)
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = banner.localizedTitle(language),
                                fontFamily = PlayfairDisplayFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                lineHeight = 25.sp,
                                color = BrandNavy,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (language == com.example.domain.AppLanguage.TA) banner.subtitleTa else banner.subtitleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimaryDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Carousel Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(banners.size) { idx ->
                val active = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .size(if (active) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (active) BrandPrimary else BrandBorder)
                )
            }
        }
    }
}

@Composable
private fun CategoryGridTile(
    category: Category,
    language: com.example.domain.AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(0.8.dp, BrandBorder.copy(alpha = 0.7f)),
        modifier = modifier
            .height(114.dp)
            .pinkCardShadow(elevation = 4.dp, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .testTag("category_tile_${category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FlowerPhoto(
                imageKey = category.imageKey,
                contentDescription = category.localizedName(language),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Text(
                text = category.localizedName(language),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = BrandNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    language: com.example.domain.AppLanguage,
    quantityInCart: Int,
    isWishlisted: Boolean,
    shopOpen: Boolean,
    onOpenProduct: () -> Unit,
    onToggleWishlist: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discount = MoneyFormatter.discountPercent(product.pricePaise, product.mrpPaise)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        border = BorderStroke(0.8.dp, BrandBorder.copy(alpha = 0.75f)),
        modifier = modifier
            .pinkCardShadow(elevation = 6.dp, cornerRadius = 18.dp)
            .clickable(onClick = onOpenProduct)
            .testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.12f)
                    .clip(RoundedCornerShape(13.dp))
            ) {
                FlowerPhoto(
                    imageKey = product.imageKey,
                    contentDescription = product.localizedName(language),
                    modifier = Modifier.fillMaxSize()
                )
                AnimatedWishlistHeart(
                    isWishlisted = isWishlisted,
                    onClick = onToggleWishlist,
                    testTag = "wishlist_btn_${product.id}",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
                if (!product.inStock) {
                    Surface(
                        color = BrandNavy.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Out of Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (discount != null) {
                    Surface(
                        color = BrandPrimary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "$discount% OFF",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = BrandSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.localizedName(language),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = BrandNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = product.unitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BrandTextMuted,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = MoneyFormatter.formatPaise(product.pricePaise),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandPrimary
                    )
                    if (product.mrpPaise != null && product.mrpPaise > product.pricePaise) {
                        Text(
                            text = MoneyFormatter.formatPaise(product.mrpPaise),
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = BrandTextMuted
                        )
                    }
                }

                AnimatedAddOrStepperButton(
                    quantity = quantityInCart,
                    enabled = shopOpen && product.inStock,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    testTagPrefix = product.id
                )
            }
        }
    }
}

/**
 * Phase 4 Catalog & Search Screen with category filter chips, in-stock toggle, and price sorting.
 */
@Composable
fun CatalogScreen(
    uiState: SunFlowersUiState,
    initialCategoryId: String?,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onIncrementCart: (Product) -> Unit,
    onDecrementCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryId by remember(initialCategoryId) { mutableStateOf(initialCategoryId) }
    var searchQuery by remember { mutableStateOf("") }
    var inStockOnly by remember { mutableStateOf(false) }
    var sortLowToHigh by remember { mutableStateOf<Boolean?>(null) }

    val filteredProducts = remember(uiState.activeProducts, selectedCategoryId, searchQuery, inStockOnly, sortLowToHigh) {
        val q = searchQuery.trim()
        uiState.activeProducts
            .filter { p -> selectedCategoryId == null || p.categoryId == selectedCategoryId }
            .filter { p -> !inStockOnly || p.inStock }
            .filter { p ->
                q.length < 2 ||
                    p.nameEn.contains(q, ignoreCase = true) ||
                    p.nameTa.contains(q, ignoreCase = true) ||
                    p.descriptionEn.contains(q, ignoreCase = true)
            }
            .let { list ->
                when (sortLowToHigh) {
                    true -> list.sortedBy { it.pricePaise }
                    false -> list.sortedByDescending { it.pricePaise }
                    null -> list
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("catalog_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("catalog_back_btn")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(uiState.strings.searchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = BrandPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = BrandTextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BrandSurface,
                    unfocusedContainerColor = BrandSurface,
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = BrandBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .testTag("catalog_search_input")
            )
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandPrimary,
                        selectedLabelColor = BrandSurface
                    )
                )
            }
            items(uiState.categories, key = { it.id }) { cat ->
                FilterChip(
                    selected = selectedCategoryId == cat.id,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.localizedName(uiState.language)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandPrimary,
                        selectedLabelColor = BrandSurface
                    )
                )
            }
        }

        // Sort & In-Stock Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = inStockOnly,
                    onClick = { inStockOnly = !inStockOnly },
                    label = { Text(uiState.strings.inStock) }
                )
            }
            item {
                FilterChip(
                    selected = sortLowToHigh == true,
                    onClick = { sortLowToHigh = if (sortLowToHigh == true) null else true },
                    label = { Text("Price: Low → High") }
                )
            }
            item {
                FilterChip(
                    selected = sortLowToHigh == false,
                    onClick = { sortLowToHigh = if (sortLowToHigh == false) null else false },
                    label = { Text("Price: High → Low") }
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            EmptyFloralState(
                title = uiState.strings.noFlowersFound,
                subtitle = "Try clearing filters or searching for roses, jasmine, or garlands.",
                ctaText = "Show All Flowers",
                onCtaClick = {
                    selectedCategoryId = null
                    searchQuery = ""
                    inStockOnly = false
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        language = uiState.language,
                        quantityInCart = uiState.cartQuantities[product.id] ?: 0,
                        isWishlisted = uiState.wishlistIds.contains(product.id),
                        shopOpen = uiState.effectiveShopStatus.effectiveOpen,
                        onOpenProduct = { onOpenProduct(product.id) },
                        onToggleWishlist = { onToggleWishlist(product.id) },
                        onIncrement = { onIncrementCart(product) },
                        onDecrement = { onDecrementCart(product.id) }
                    )
                }
            }
        }
    }
}

/**
 * Phase 4 Product Detail Screen with full description, stock state, shop-closed awareness, and sticky bottom bar.
 */
@Composable
fun ProductDetailScreen(
    uiState: SunFlowersUiState,
    productId: String,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    onToggleWishlist: (String) -> Unit,
    onIncrementCart: (Product) -> Unit,
    onDecrementCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val product = uiState.activeProducts.firstOrNull { it.id == productId }
    if (product == null) {
        EmptyFloralState(
            title = "Product unavailable",
            subtitle = "This floral item is no longer active.",
            ctaText = "Go Back",
            onCtaClick = onBack
        )
        return
    }

    val qty = uiState.cartQuantities[product.id] ?: 0
    val discount = MoneyFormatter.discountPercent(product.pricePaise, product.mrpPaise)
    val shopOpen = uiState.effectiveShopStatus.effectiveOpen

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("product_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 104.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
            ) {
                FlowerPhoto(
                    imageKey = product.imageKey,
                    contentDescription = product.localizedName(uiState.language),
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(BrandSurface.copy(alpha = 0.9f))
                        .testTag("product_detail_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BrandNavy)
                }
                AnimatedWishlistHeart(
                    isWishlisted = uiState.wishlistIds.contains(product.id),
                    onClick = { onToggleWishlist(product.id) },
                    testTag = "product_detail_wishlist_btn",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                AnimatedVisibility(visible = !shopOpen) {
                    Column {
                        ShopClosedStatusBanner(
                            status = uiState.effectiveShopStatus,
                            language = uiState.language,
                            strings = uiState.strings
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (product.inStock) BrandSuccessSoft else BrandDangerSoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (product.inStock) uiState.strings.inStock else uiState.strings.outOfStock,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (product.inStock) BrandSuccess else BrandDanger,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = BrandPrimarySoft,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = product.unitLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandPrimaryDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = product.localizedName(uiState.language),
                    style = MaterialTheme.typography.headlineLarge,
                    color = BrandNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = MoneyFormatter.formatPaise(product.pricePaise),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandPrimary
                    )
                    if (product.mrpPaise != null && product.mrpPaise > product.pricePaise) {
                        Text(
                            text = MoneyFormatter.formatPaise(product.mrpPaise),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                            color = BrandTextMuted
                        )
                    }
                    if (discount != null) {
                        Surface(color = BrandPrimary, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "SAVE $discount%",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = product.localizedDescription(uiState.language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandNavy.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = BrandSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Freshness & Local Delivery Promise",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandNavy
                        )
                        Text(
                            text = "• Delivered within ${uiState.shopSettings.estimatedDeliveryMinutes} min inside ${uiState.shopSettings.deliveryRadiusKm} km radius.\n• Hand-tied fresh right before dispatch from Sun Flowers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTextMuted
                        )
                    }
                }
            }
        }

        // Sticky Bottom Action Bar
        Surface(
            color = BrandSurface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (qty > 0 && shopOpen) {
                    AnimatedAddOrStepperButton(
                        quantity = qty,
                        enabled = product.inStock,
                        onIncrement = { onIncrementCart(product) },
                        onDecrement = { onDecrementCart(product.id) },
                        testTagPrefix = "detail_${product.id}"
                    )
                    PrimaryPillButton(
                        text = "${uiState.strings.cartTitle} (${uiState.totalCartCount})",
                        onClick = onOpenCart,
                        modifier = Modifier.weight(1f),
                        testTag = "detail_go_to_cart_btn"
                    )
                } else {
                    PrimaryPillButton(
                        text = when {
                            !shopOpen -> uiState.strings.shopClosedShort
                            !product.inStock -> uiState.strings.outOfStock
                            else -> "${uiState.strings.addToCart} • ${MoneyFormatter.formatPaise(product.pricePaise)}"
                        },
                        enabled = shopOpen && product.inStock,
                        onClick = { onIncrementCart(product) },
                        modifier = Modifier.weight(1f),
                        testTag = "detail_add_to_cart_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(
    uiState: SunFlowersUiState,
    onOpenProduct: (String) -> Unit,
    onBrowseHome: () -> Unit,
    onToggleWishlist: (String) -> Unit,
    onIncrementCart: (Product) -> Unit,
    onDecrementCart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wishlistedProducts = remember(uiState.activeProducts, uiState.wishlistIds) {
        uiState.activeProducts.filter { uiState.wishlistIds.contains(it.id) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBackground)
            .statusBarsPadding()
            .testTag("wishlist_screen")
    ) {
        Text(
            text = uiState.strings.navWishlist,
            style = MaterialTheme.typography.headlineLarge,
            color = BrandNavy,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (wishlistedProducts.isEmpty()) {
            EmptyFloralState(
                title = uiState.strings.emptyWishlistTitle,
                subtitle = uiState.strings.emptyWishlistSubtitle,
                ctaText = uiState.strings.browseFlowers,
                onCtaClick = onBrowseHome
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(wishlistedProducts, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        language = uiState.language,
                        quantityInCart = uiState.cartQuantities[product.id] ?: 0,
                        isWishlisted = true,
                        shopOpen = uiState.effectiveShopStatus.effectiveOpen,
                        onOpenProduct = { onOpenProduct(product.id) },
                        onToggleWishlist = { onToggleWishlist(product.id) },
                        onIncrement = { onIncrementCart(product) },
                        onDecrement = { onDecrementCart(product.id) }
                    )
                }
            }
        }
    }
}
