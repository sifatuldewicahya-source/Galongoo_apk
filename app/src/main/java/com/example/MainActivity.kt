package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.components.NotificationListOverlay
import com.example.ui.components.isActiveStatus
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuyerCartCheckoutScreen
import com.example.ui.screens.BuyerHomeScreen
import com.example.ui.screens.BuyerOrdersScreen
import com.example.ui.screens.BuyerProfileScreen
import com.example.ui.screens.BuyerStoreDetailScreen
import com.example.ui.screens.BuyerStoresScreen
import com.example.ui.screens.CourierTasksScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.SellerOrdersScreen
import com.example.ui.screens.SellerProductsScreen
import com.example.ui.screens.SellerReportsScreen
import com.example.ui.screens.SellerStoreSettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.GalonDekatTheme
import com.example.ui.viewmodel.GalonDekatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalonDekatTheme {
                GalonDekatApp()
            }
        }
    }
}

private data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val tag: String,
)

@Composable
fun GalonDekatApp(
    viewModel: GalonDekatViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.ordersWithItems.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val activeCouriers by viewModel.activeCouriers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (uiState.isSplashVisible) {
        SplashScreen()
        return
    }

    val currentUser = uiState.currentUser
    if (currentUser == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AuthScreen(
                    isBusy = uiState.isBusy,
                    onLogin = viewModel::login,
                    onRegister = viewModel::register,
                    onDemoLogin = viewModel::loginDemo,
                )
            }
        }
        return
    }

    val unreadNotifications = notifications.count { !it.isRead }
    var buyerTab by rememberSaveable(currentUser.id) { mutableIntStateOf(0) }
    var sellerTab by rememberSaveable(currentUser.id) { mutableIntStateOf(0) }

    // Back handling for overlays
    if (uiState.showNotificationsScreen) {
        BackHandler { viewModel.closeNotifications() }
    } else if (currentUser.role == UserRole.BUYER && uiState.selectedStoreId == null && buyerTab != 0) {
        BackHandler { buyerTab = 0 }
    } else if (currentUser.role == UserRole.SELLER && sellerTab != 0) {
        BackHandler { sellerTab = 0 }
    }

    val sellerStore = remember(stores, currentUser) {
        stores.firstOrNull { it.sellerId == currentUser.id }
    }

    val navItems: List<NavTabItem> = when (currentUser.role) {
        UserRole.BUYER -> listOf(
            NavTabItem("Beranda", Icons.Filled.Home, Icons.Outlined.Home, tag = "nav_buyer_home"),
            NavTabItem("Toko", Icons.Filled.Storefront, Icons.Outlined.Storefront, tag = "nav_buyer_stores"),
            NavTabItem(
                "Keranjang",
                Icons.Filled.ShoppingCart,
                Icons.Outlined.ShoppingCart,
                badgeCount = uiState.cartItems.sumOf { it.quantity },
                tag = "nav_buyer_cart",
            ),
            NavTabItem(
                "Pesanan",
                Icons.Filled.ReceiptLong,
                Icons.Outlined.ReceiptLong,
                badgeCount = orders.count { it.order.buyerId == currentUser.id && it.order.isActiveStatus() },
                tag = "nav_buyer_orders",
            ),
            NavTabItem("Profil", Icons.Filled.Person, Icons.Outlined.Person, tag = "nav_buyer_profile"),
        )
        UserRole.SELLER -> {
            val newOrderBadge = sellerStore?.let { st ->
                orders.count {
                    it.order.storeId == st.id && it.order.status == com.example.data.model.OrderStatus.PENDING_CONFIRMATION
                }
            } ?: 0
            listOf(
                NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, tag = "nav_seller_dashboard"),
                NavTabItem("Pesanan", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, badgeCount = newOrderBadge, tag = "nav_seller_orders"),
                NavTabItem("Produk", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, tag = "nav_seller_products"),
                NavTabItem("Laporan", Icons.Filled.BarChart, Icons.Outlined.BarChart, tag = "nav_seller_reports"),
                NavTabItem("Toko", Icons.Filled.Storefront, Icons.Outlined.Storefront, tag = "nav_seller_store"),
            )
        }
        UserRole.COURIER, UserRole.ADMIN -> emptyList()
    }

    val showNavigation = navItems.isNotEmpty() && !uiState.showNotificationsScreen && uiState.selectedStoreId == null
    val activeTabIndex = if (currentUser.role == UserRole.BUYER) buyerTab else sellerTab

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp && showNavigation

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showNavigation && !useNavigationRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        navItems.forEachIndexed { idx, item ->
                            NavigationBarItem(
                                selected = activeTabIndex == idx,
                                onClick = {
                                    if (currentUser.role == UserRole.BUYER) buyerTab = idx else sellerTab = idx
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (activeTabIndex == idx) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { idx, item ->
                            NavigationRailItem(
                                selected = activeTabIndex == idx,
                                onClick = {
                                    if (currentUser.role == UserRole.BUYER) buyerTab = idx else sellerTab = idx
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = if (activeTabIndex == idx) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    }
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (uiState.showNotificationsScreen) {
                        NotificationListOverlay(
                            notifications = notifications,
                            onBack = viewModel::closeNotifications,
                        )
                    } else {
                        when (currentUser.role) {
                            UserRole.BUYER -> {
                                val selectedStore = uiState.selectedStoreId?.let { sid ->
                                    stores.firstOrNull { it.id == sid }
                                }
                                if (selectedStore != null) {
                                    BuyerStoreDetailScreen(
                                        store = selectedStore,
                                        products = products.filter { it.storeId == selectedStore.id },
                                        reviews = reviews.filter { it.storeId == selectedStore.id },
                                        activeOrderCount = orders.count {
                                            it.order.storeId == selectedStore.id && it.order.isActiveStatus()
                                        },
                                        cartCount = uiState.cartItems.sumOf { it.quantity },
                                        onBack = viewModel::closeStoreDetail,
                                        onAddToCart = viewModel::addToCart,
                                        onGoToCart = {
                                            viewModel.closeStoreDetail()
                                            buyerTab = 2
                                        },
                                    )
                                } else {
                                    AnimatedContent(targetState = buyerTab, label = "buyer_tab") { tab ->
                                        when (tab) {
                                            0 -> BuyerHomeScreen(
                                                user = currentUser,
                                                usePrimaryAddress = uiState.usePrimaryAddress,
                                                searchQuery = uiState.searchQuery,
                                                selectedCategory = uiState.selectedCategory,
                                                stores = stores,
                                                products = products,
                                                orders = orders,
                                                announcements = announcements,
                                                unreadNotifications = unreadNotifications,
                                                onSearchChange = viewModel::setSearchQuery,
                                                onCategorySelect = viewModel::setSelectedCategory,
                                                onToggleAddress = viewModel::selectAddressType,
                                                onOpenStore = viewModel::openStoreDetail,
                                                onAddToCart = viewModel::addToCart,
                                                onOpenNotifications = viewModel::openNotifications,
                                                onGoToStoresTab = { buyerTab = 1 },
                                            )
                                            1 -> BuyerStoresScreen(
                                                stores = stores,
                                                products = products,
                                                orders = orders,
                                                searchQuery = uiState.searchQuery,
                                                onlyOpen = uiState.onlyOpenStores,
                                                selectedCategory = uiState.selectedCategory,
                                                unreadNotifications = unreadNotifications,
                                                onSearchChange = viewModel::setSearchQuery,
                                                onToggleOnlyOpen = viewModel::toggleOnlyOpenStores,
                                                onCategorySelect = viewModel::setSelectedCategory,
                                                onOpenStore = viewModel::openStoreDetail,
                                                onOpenNotifications = viewModel::openNotifications,
                                            )
                                            2 -> BuyerCartCheckoutScreen(
                                                user = currentUser,
                                                usePrimaryAddress = uiState.usePrimaryAddress,
                                                cartItems = uiState.cartItems,
                                                stores = stores,
                                                isBusy = uiState.isBusy,
                                                unreadNotifications = unreadNotifications,
                                                onUpdateQuantity = viewModel::updateCartQuantity,
                                                onClearCart = viewModel::clearCart,
                                                onPlaceOrder = { addr, notes, sched, method ->
                                                    viewModel.placeOrder(addr, notes, sched, method) {
                                                        buyerTab = 3
                                                    }
                                                },
                                                onOpenNotifications = viewModel::openNotifications,
                                            )
                                            3 -> BuyerOrdersScreen(
                                                user = currentUser,
                                                orders = orders,
                                                unreadNotifications = unreadNotifications,
                                                onCancelOrder = { order, reason ->
                                                    viewModel.updateOrderStatus(
                                                        order = order,
                                                        newStatus = com.example.data.model.OrderStatus.REJECTED_CANCELLED,
                                                        reason = reason,
                                                    )
                                                },
                                                onReorder = {
                                                    viewModel.reorder(it)
                                                    buyerTab = 2
                                                },
                                                onSubmitReview = viewModel::submitReview,
                                                onOpenNotifications = viewModel::openNotifications,
                                            )
                                            else -> BuyerProfileScreen(
                                                user = currentUser,
                                                unreadNotifications = unreadNotifications,
                                                onUpdateProfile = viewModel::updateProfile,
                                                onSubmitComplaint = viewModel::submitComplaint,
                                                onOpenNotifications = viewModel::openNotifications,
                                                onLogout = viewModel::logout,
                                            )
                                        }
                                    }
                                }
                            }

                            UserRole.SELLER -> {
                                AnimatedContent(targetState = sellerTab, label = "seller_tab") { tab ->
                                    when (tab) {
                                        0 -> SellerDashboardScreen(
                                            user = currentUser,
                                            store = sellerStore,
                                            products = products,
                                            orders = orders,
                                            unreadNotifications = unreadNotifications,
                                            onOpenNotifications = viewModel::openNotifications,
                                            onUpdateOrderStatus = { o, s, r, c ->
                                                viewModel.updateOrderStatus(o, s, r, c)
                                            },
                                            onGoToOrdersTab = { sellerTab = 1 },
                                            onGoToProductsTab = { sellerTab = 2 },
                                        )
                                        1 -> SellerOrdersScreen(
                                            store = sellerStore,
                                            orders = orders,
                                            couriers = activeCouriers,
                                            unreadNotifications = unreadNotifications,
                                            onOpenNotifications = viewModel::openNotifications,
                                            onUpdateOrderStatus = { o, s, r, c ->
                                                viewModel.updateOrderStatus(o, s, r, c)
                                            },
                                        )
                                        2 -> SellerProductsScreen(
                                            store = sellerStore,
                                            products = products,
                                            unreadNotifications = unreadNotifications,
                                            onOpenNotifications = viewModel::openNotifications,
                                            onSaveProduct = viewModel::saveProduct,
                                            onDeleteProduct = viewModel::deleteProduct,
                                        )
                                        3 -> SellerReportsScreen(
                                            store = sellerStore,
                                            orders = orders,
                                            unreadNotifications = unreadNotifications,
                                            onOpenNotifications = viewModel::openNotifications,
                                        )
                                        else -> SellerStoreSettingsScreen(
                                            store = sellerStore,
                                            unreadNotifications = unreadNotifications,
                                            onOpenNotifications = viewModel::openNotifications,
                                            onSaveStore = viewModel::updateStore,
                                            onSubmitComplaint = viewModel::submitComplaint,
                                            onLogout = viewModel::logout,
                                        )
                                    }
                                }
                            }

                            UserRole.COURIER -> {
                                CourierTasksScreen(
                                    user = currentUser,
                                    orders = orders,
                                    unreadNotifications = unreadNotifications,
                                    onOpenNotifications = viewModel::openNotifications,
                                    onUpdateOrderStatus = { o, s, c, proof ->
                                        viewModel.updateOrderStatus(o, s, null, c, proof)
                                    },
                                    onLogout = viewModel::logout,
                                )
                            }

                            UserRole.ADMIN -> {
                                AdminDashboardScreen(
                                    user = currentUser,
                                    users = users,
                                    stores = stores,
                                    orders = orders,
                                    complaints = complaints,
                                    announcements = announcements,
                                    onUpdateStore = viewModel::updateStore,
                                    onToggleUserActive = viewModel::toggleUserActive,
                                    onUpdateComplaint = viewModel::updateComplaint,
                                    onAddAnnouncement = viewModel::addAnnouncement,
                                    onToggleAnnouncement = viewModel::toggleAnnouncement,
                                    onDeleteAnnouncement = viewModel::deleteAnnouncement,
                                    onLogout = viewModel::logout,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
