package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AnnouncementEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.StoreEntity
import com.example.data.local.UserEntity
import com.example.data.model.CartItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.formatRupiah
import com.example.data.model.formatTimestamp
import com.example.ui.components.AppBrandTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GallonProductGraphic
import com.example.ui.components.GalonLogoImage
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.OrderStatusTimeline
import com.example.ui.components.ResponsiveCenteredColumn
import com.example.ui.components.StoreCard
import com.example.ui.components.isActiveStatus
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaPrimaryBright

@Composable
fun BuyerHomeScreen(
    user: UserEntity,
    usePrimaryAddress: Boolean,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    stores: List<StoreEntity>,
    products: List<ProductEntity>,
    orders: List<OrderWithItems>,
    announcements: List<AnnouncementEntity>,
    unreadNotifications: Int,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (ProductCategory?) -> Unit,
    onToggleAddress: (Boolean) -> Unit,
    onOpenStore: (Long) -> Unit,
    onAddToCart: (ProductEntity, StoreEntity) -> Unit,
    onOpenNotifications: () -> Unit,
    onGoToStoresTab: () -> Unit,
) {
    val verifiedStores = remember(stores) { stores.filter { it.isVerified } }
    val activeAddress = if (usePrimaryAddress || user.altAddress.isBlank()) {
        user.primaryAddress
    } else {
        user.altAddress
    }
    val filteredStores = remember(verifiedStores, searchQuery, selectedCategory, products) {
        verifiedStores.filter { store ->
            val storeProducts = products.filter { it.storeId == store.id }
            val matchesQuery = searchQuery.isBlank() ||
                store.name.contains(searchQuery, ignoreCase = true) ||
                storeProducts.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = selectedCategory == null ||
                storeProducts.any { it.category == selectedCategory && it.isAvailable && it.stock > 0 }
            matchesQuery && matchesCategory
        }.sortedBy { it.distanceKm }
    }

    val recommendedProducts = remember(products, verifiedStores, searchQuery, selectedCategory) {
        val storeMap = verifiedStores.associateBy { it.id }
        products.filter { prod ->
            val st = storeMap[prod.storeId]
            st != null &&
                prod.isAvailable &&
                prod.stock > 0 &&
                (selectedCategory == null || prod.category == selectedCategory) &&
                (searchQuery.isBlank() || prod.name.contains(searchQuery, ignoreCase = true) ||
                    st.name.contains(searchQuery, ignoreCase = true))
        }.take(6)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "GalonDekat",
            subtitle = "Halo, ${user.name}",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Address selector card
            item {
                ResponsiveCenteredColumn {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Lokasi pengiriman",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alamat Pengiriman Aktif",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeAddress,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            if (user.altAddress.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = usePrimaryAddress,
                                        onClick = { onToggleAddress(true) },
                                        label = { Text("Alamat Utama") },
                                    )
                                    FilterChip(
                                        selected = !usePrimaryAddress,
                                        onClick = { onToggleAddress(false) },
                                        label = { Text("Alamat Alternatif") },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search bar
            item {
                ResponsiveCenteredColumn {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Cari depot, Aqua, Le Minerale, isi ulang...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buyer_search_input"),
                    )
                }
            }

            // Promo Banners
            val activeBanners = announcements.filter { it.isActive }
            if (activeBanners.isNotEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            activeBanners.forEach { banner ->
                                Box(
                                    modifier = Modifier
                                        .width(290.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(AquaPrimary, AquaPrimaryBright),
                                            ),
                                        )
                                        .padding(16.dp),
                                ) {
                                    Column {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = banner.badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = banner.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = banner.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories
            item {
                ResponsiveCenteredColumn {
                    Text(
                        text = "Kategori Layanan",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelect(null) },
                            label = { Text("Semua") },
                        )
                        ProductCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    onCategorySelect(if (selectedCategory == category) null else category)
                                },
                                label = { Text(category.shortLabel) },
                            )
                        }
                    }
                }
            }

            // Nearest Stores Header
            item {
                ResponsiveCenteredColumn {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Toko & Depot Terdekat",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = onGoToStoresTab) {
                            Text("Lihat Semua")
                        }
                    }
                }
            }

            if (filteredStores.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = "Toko tidak ditemukan",
                            subtitle = "Ubah kata kunci pencarian atau filter kategori Anda.",
                        )
                    }
                }
            } else {
                items(filteredStores, key = { it.id }) { store ->
                    val activeCount = orders.count {
                        it.order.storeId == store.id && it.order.isActiveStatus()
                    }
                    ResponsiveCenteredColumn {
                        StoreCard(
                            store = store,
                            activeOrderCount = activeCount,
                            onClick = { onOpenStore(store.id) },
                        )
                    }
                }
            }

            // Recommended Products
            if (recommendedProducts.isNotEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Produk Populer Siap Antar",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                items(recommendedProducts, key = { "rec_${it.id}" }) { product ->
                    val store = verifiedStores.firstOrNull { it.id == product.storeId }
                    if (store != null) {
                        ResponsiveCenteredColumn {
                            ProductRowCard(
                                product = product,
                                storeName = store.name,
                                onOpenStore = { onOpenStore(store.id) },
                                onAddToCart = { onAddToCart(product, store) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerStoresScreen(
    stores: List<StoreEntity>,
    products: List<ProductEntity>,
    orders: List<OrderWithItems>,
    searchQuery: String,
    onlyOpen: Boolean,
    selectedCategory: ProductCategory?,
    unreadNotifications: Int,
    onSearchChange: (String) -> Unit,
    onToggleOnlyOpen: () -> Unit,
    onCategorySelect: (ProductCategory?) -> Unit,
    onOpenStore: (Long) -> Unit,
    onOpenNotifications: () -> Unit,
) {
    var maxDistanceKm by remember { mutableIntStateOf(10) }

    val filtered = remember(stores, products, searchQuery, onlyOpen, selectedCategory, maxDistanceKm) {
        stores.filter { store ->
            val storeProds = products.filter { it.storeId == store.id }
            val matchQuery = searchQuery.isBlank() ||
                store.name.contains(searchQuery, ignoreCase = true) ||
                store.address.contains(searchQuery, ignoreCase = true) ||
                storeProds.any { it.name.contains(searchQuery, ignoreCase = true) }
            val matchOpen = !onlyOpen || (store.isOpen && !store.isPaused)
            val matchCat = selectedCategory == null || storeProds.any { it.category == selectedCategory }
            val matchDist = store.distanceKm <= maxDistanceKm
            store.isVerified && matchQuery && matchOpen && matchCat && matchDist
        }.sortedBy { it.distanceKm }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Toko Terdekat",
            subtitle = "${filtered.size} depot tersedia di area Anda",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ResponsiveCenteredColumn {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Cari nama toko atau merek galon...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = onlyOpen,
                            onClick = onToggleOnlyOpen,
                            label = { Text("Hanya yang Buka") },
                        )
                        listOf(2, 5, 10).forEach { km ->
                            FilterChip(
                                selected = maxDistanceKm == km,
                                onClick = { maxDistanceKm = km },
                                label = { Text("≤ $km km") },
                            )
                        }
                        ProductCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    onCategorySelect(if (selectedCategory == cat) null else cat)
                                },
                                label = { Text(cat.shortLabel) },
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = "Tidak ada toko yang sesuai filter",
                            subtitle = "Coba perluas jarak atau matikan filter status buka.",
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { store ->
                    val activeCount = orders.count {
                        it.order.storeId == store.id && it.order.isActiveStatus()
                    }
                    ResponsiveCenteredColumn {
                        StoreCard(
                            store = store,
                            activeOrderCount = activeCount,
                            onClick = { onOpenStore(store.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerStoreDetailScreen(
    store: StoreEntity,
    products: List<ProductEntity>,
    reviews: List<ReviewEntity>,
    activeOrderCount: Int,
    cartCount: Int,
    onBack: () -> Unit,
    onAddToCart: (ProductEntity, StoreEntity) -> Unit,
    onGoToCart: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val outOfRange = store.distanceKm > store.maxServiceRadiusKm
    val isFull = activeOrderCount >= store.maxActiveOrders

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = store.name,
            subtitle = "${store.distanceKm} km • Ongkir ${formatRupiah(store.deliveryFee)}",
            onBackClick = onBack,
            trailingAction = {
                if (cartCount > 0) {
                    TextButton(onClick = onGoToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Keranjang")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("($cartCount)")
                    }
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ResponsiveCenteredColumn {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${store.rating} (${store.reviewCount})",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = store.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Jam Operasional: ${store.openHours} • Estimasi ${store.estimatedMinutes} menit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Jangkauan kirim maks ${store.maxServiceRadiusKm} km • Kapasitas antrean $activeOrderCount/${store.maxActiveOrders}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (outOfRange || !store.isOpen || store.isPaused || isFull) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = when {
                                            !store.isOpen -> "Toko sedang tutup saat ini."
                                            store.isPaused -> "Toko sedang menjeda penerimaan pesanan."
                                            isFull -> "Antrean pesanan toko sedang penuh."
                                            else -> "Lokasi Anda di luar jangkauan kirim toko ini."
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ResponsiveCenteredColumn {
                    Text(
                        text = "Katalog Produk (${products.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            items(products, key = { it.id }) { product ->
                ResponsiveCenteredColumn {
                    ProductRowCard(
                        product = product,
                        storeName = null,
                        onOpenStore = {},
                        onAddToCart = { onAddToCart(product, store) },
                    )
                }
            }

            item {
                ResponsiveCenteredColumn {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ulasan Pembeli (${reviews.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (reviews.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        Text(
                            text = "Belum ada ulasan untuk toko ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(reviews, key = { it.id }) { rev ->
                    ResponsiveCenteredColumn {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(rev.buyerName, style = MaterialTheme.typography.titleSmall)
                                    Text("★ ${rev.rating}/5", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductRowCard(
    product: ProductEntity,
    storeName: String?,
    onOpenStore: () -> Unit,
    onAddToCart: () -> Unit,
) {
    val canBuy = product.isAvailable && product.stock > 0
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GallonProductGraphic(
                category = product.category,
                tintColor = Color(product.brandColorHex),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!storeName.isNullOrBlank()) {
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onOpenStore),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = formatRupiah(product.price),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (canBuy) "Stok: ${product.stock}" else "Stok Habis",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (canBuy) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = onAddToCart,
                        enabled = canBuy,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_cart_${product.id}"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah")
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerCartCheckoutScreen(
    user: UserEntity,
    usePrimaryAddress: Boolean,
    cartItems: List<CartItem>,
    stores: List<StoreEntity>,
    isBusy: Boolean,
    unreadNotifications: Int,
    onUpdateQuantity: (Long, Int) -> Unit,
    onClearCart: () -> Unit,
    onPlaceOrder: (String, String, String, PaymentMethod) -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val store = remember(cartItems, stores) {
        val sid = cartItems.firstOrNull()?.storeId
        stores.firstOrNull { it.id == sid }
    }
    var addressText by remember(user, usePrimaryAddress) {
        mutableStateOf(
            if (usePrimaryAddress || user.altAddress.isBlank()) user.primaryAddress else user.altAddress,
        )
    }
    var notes by remember { mutableStateOf("") }
    val schedules = listOf("Segera (Antar Sekarang)", "Pagi (08:00 - 11:00)", "Siang (13:00 - 16:00)", "Sore (17:00 - 20:00)")
    var selectedSchedule by remember { mutableStateOf(schedules.first()) }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.COD) }

    val subtotal = cartItems.sumOf { it.subtotal }
    val deliveryFee = store?.deliveryFee ?: 0
    val total = subtotal + deliveryFee

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Keranjang & Checkout",
            subtitle = store?.let { "Dari ${it.name}" } ?: "Belum ada produk dipilih",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
            trailingAction = {
                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Kosongkan keranjang")
                    }
                }
            },
        )

        if (cartItems.isEmpty()) {
            ResponsiveCenteredColumn(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    title = "Keranjang belanja kosong",
                    subtitle = "Pilih toko galon atau depot air isi ulang di Beranda untuk mulai memesan.",
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResponsiveCenteredColumn {
                    // Items card
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Daftar Produk", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            cartItems.forEachIndexed { idx, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${formatRupiah(item.unitPrice)} / unit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.productId, -1) },
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                                        }
                                        Text(
                                            text = "${item.quantity}",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        )
                                        IconButton(
                                            onClick = { onUpdateQuantity(item.productId, 1) },
                                            modifier = Modifier.minimumInteractiveComponentSize(),
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                                        }
                                    }
                                }
                                if (idx < cartItems.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address & notes
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Alamat & Catatan Pengiriman", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (user.altAddress.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = addressText == user.primaryAddress,
                                        onClick = { addressText = user.primaryAddress },
                                        label = { Text("Alamat Utama") },
                                    )
                                    FilterChip(
                                        selected = addressText == user.altAddress,
                                        onClick = { addressText = user.altAddress },
                                        label = { Text("Alamat Alternatif") },
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            OutlinedTextField(
                                value = addressText,
                                onValueChange = { addressText = it },
                                label = { Text("Alamat Pengiriman") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Catatan untuk penjual/kurir (opsional)") },
                                placeholder = { Text("Contoh: Galon kosong diletakkan di teras") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Schedule
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jadwal Pengiriman", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                schedules.forEach { sched ->
                                    FilterChip(
                                        selected = selectedSchedule == sched,
                                        onClick = { selectedSchedule = sched },
                                        label = { Text(sched) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment method
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            PaymentMethod.entries.forEach { method ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedPayment = method }
                                        .padding(vertical = 6.dp),
                                ) {
                                    RadioButton(
                                        selected = selectedPayment == method,
                                        onClick = { selectedPayment = method },
                                    )
                                    Column {
                                        Text(method.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            method.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary & Submit
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rincian Biaya", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            SummaryRow("Subtotal Produk", formatRupiah(subtotal))
                            SummaryRow("Biaya Pengiriman", formatRupiah(deliveryFee))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Total Pembayaran", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = formatRupiah(total),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onPlaceOrder(addressText, notes, selectedSchedule, selectedPayment)
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("checkout_submit_button"),
                            ) {
                                Text("Buat Pesanan Sekarang", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BuyerOrdersScreen(
    user: UserEntity,
    orders: List<OrderWithItems>,
    unreadNotifications: Int,
    onCancelOrder: (OrderEntity, String) -> Unit,
    onReorder: (OrderWithItems) -> Unit,
    onSubmitReview: (OrderEntity, Int, String) -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val myOrders = remember(orders, user.id) { orders.filter { it.order.buyerId == user.id } }
    var showActiveOnly by remember { mutableStateOf(true) }
    val displayed = remember(myOrders, showActiveOnly) {
        if (showActiveOnly) myOrders.filter { it.order.isActiveStatus() }
        else myOrders
    }

    var reviewTargetOrder by remember { mutableStateOf<OrderEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Pesanan Saya",
            subtitle = "${myOrders.count { it.order.isActiveStatus() }} pesanan sedang diproses",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            ResponsiveCenteredColumn {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showActiveOnly,
                        onClick = { showActiveOnly = true },
                        label = { Text("Sedang Berjalan (${myOrders.count { it.order.isActiveStatus() }})") },
                    )
                    FilterChip(
                        selected = !showActiveOnly,
                        onClick = { showActiveOnly = false },
                        label = { Text("Semua Riwayat (${myOrders.size})") },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (displayed.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = if (showActiveOnly) "Tidak ada pesanan aktif" else "Belum ada riwayat pesanan",
                            subtitle = "Pesanan yang Anda buat akan muncul di sini lengkap dengan pelacakan status.",
                        )
                    }
                }
            } else {
                items(displayed, key = { it.order.id }) { item ->
                    val order = item.order
                    ResponsiveCenteredColumn {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(order.orderCode, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${order.storeName} • ${formatTimestamp(order.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    OrderStatusBadge(order.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OrderStatusTimeline(order.status)
                                Spacer(modifier = Modifier.height(12.dp))

                                item.items.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "${line.quantity}x ${line.productName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = formatRupiah(line.priceAtTransaction * line.quantity),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${order.paymentMethod.label} • ${order.deliverySchedule}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Total ${formatRupiah(order.totalAmount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                if (!order.courierName.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Kurir: ${order.courierName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                                if (!order.deliveryProofNote.isNullOrBlank()) {
                                    Text(
                                        text = "Serah terima: ${order.deliveryProofNote}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!order.rejectionReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Alasan: ${order.rejectionReason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    if (order.status == OrderStatus.PENDING_CONFIRMATION) {
                                        OutlinedButton(
                                            onClick = { onCancelOrder(order, "Dibatalkan oleh pembeli") },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error,
                                            ),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("Batalkan")
                                        }
                                    }
                                    if (order.status == OrderStatus.COMPLETED || order.status == OrderStatus.REJECTED_CANCELLED) {
                                        OutlinedButton(
                                            onClick = { onReorder(item) },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pesan Ulang")
                                        }
                                    }
                                    if (order.status == OrderStatus.COMPLETED && !order.hasReview) {
                                        Button(
                                            onClick = { reviewTargetOrder = order },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Beri Ulasan")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reviewTargetOrder?.let { target ->
        ReviewDialog(
            storeName = target.storeName,
            onDismiss = { reviewTargetOrder = null },
            onSubmit = { rating, comment ->
                onSubmitReview(target, rating, comment)
                reviewTargetOrder = null
            },
        )
    }
}

@Composable
private fun ReviewDialog(
    storeName: String,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ulasan untuk $storeName") },
        text = {
            Column {
                Text("Pilih penilaian layanan & kualitas air:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$star bintang",
                                tint = Color(0xFFF59E0B),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar Anda") },
                    placeholder = { Text("Contoh: Pengantaran cepat dan galon bersih") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }) {
                Text("Kirim Ulasan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
fun BuyerProfileScreen(
    user: UserEntity,
    unreadNotifications: Int,
    onUpdateProfile: (String, String, String, String) -> Unit,
    onSubmitComplaint: (String, String) -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
) {
    var name by remember(user) { mutableStateOf(user.name) }
    var contact by remember(user) { mutableStateOf(user.phoneOrEmail) }
    var primaryAddr by remember(user) { mutableStateOf(user.primaryAddress) }
    var altAddr by remember(user) { mutableStateOf(user.altAddress) }
    var showComplaintDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Profil & Alamat",
            subtitle = user.role.label,
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ResponsiveCenteredColumn {
                // Brand Card with App Logo
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GalonLogoImage(size = 56.dp, cornerRadius = 14.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${user.role.label} • ${user.phoneOrEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Informasi Akun & Alamat", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Lengkap") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Nomor Kontak / Email") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = primaryAddr,
                            onValueChange = { primaryAddr = it },
                            label = { Text("Alamat Utama") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = altAddr,
                            onValueChange = { altAddr = it },
                            label = { Text("Alamat Alternatif (Kantor/Kos)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { onUpdateProfile(name, contact, primaryAddr, altAddr) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Perubahan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showComplaintDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Pengaduan / Bantuan ke Admin")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar / Ganti Akun")
                }
            }
        }
    }

    if (showComplaintDialog) {
        ComplaintSubmitDialog(
            onDismiss = { showComplaintDialog = false },
            onSubmit = { title, desc ->
                onSubmitComplaint(title, desc)
                showComplaintDialog = false
            },
        )
    }
}

@Composable
fun ComplaintSubmitDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaduan & Bantuan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Kendala") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Jelaskan detail masalah") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(title, description) }) {
                Text("Kirim Laporan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
