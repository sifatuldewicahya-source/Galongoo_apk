package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ComplaintEntity
import com.example.data.local.GalonDekatDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.ProductEntity
import com.example.data.local.ReviewEntity
import com.example.data.local.StoreEntity
import com.example.data.local.UserEntity
import com.example.data.model.CartItem
import com.example.data.model.ComplaintStatus
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole
import com.example.data.repository.GalonDekatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isSplashVisible: Boolean = true,
    val currentUser: UserEntity? = null,
    val selectedStoreId: Long? = null,
    val showNotificationsScreen: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val onlyOpenStores: Boolean = false,
    val usePrimaryAddress: Boolean = true,
    val cartItems: List<CartItem> = emptyList(),
    val snackbarMessage: String? = null,
    val isBusy: Boolean = false,
)

class GalonDekatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GalonDekatDatabase.getInstance(application)
    private val repository = GalonDekatRepository(db.dao(), application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stores: StateFlow<List<StoreEntity>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ordersWithItems: StateFlow<List<OrderWithItems>> = repository.allOrdersWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<ReviewEntity>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaints: StateFlow<List<ComplaintEntity>> = repository.allComplaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCouriers: StateFlow<List<UserEntity>> = repository.activeCouriers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<NotificationEntity>> = _uiState
        .flatMapLatest { state ->
            val uid = state.currentUser?.id
            if (uid == null) flowOf(emptyList()) else repository.observeNotifications(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val savedId = repository.getSavedUserId()
            val user = if (savedId > 0L) repository.getUserById(savedId) else null
            delay(1350L)
            _uiState.update {
                it.copy(
                    isSplashVisible = false,
                    currentUser = if (user?.isActive == true) user else null,
                )
            }
        }

        // Keep currentUser synced with DB updates
        viewModelScope.launch {
            combine(users, _uiState) { userList, state ->
                val activeId = state.currentUser?.id ?: return@combine null
                userList.firstOrNull { it.id == activeId }
            }.collect { freshUser ->
                if (freshUser != null && freshUser != _uiState.value.currentUser) {
                    _uiState.update { it.copy(currentUser = if (freshUser.isActive) freshUser else null) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.login(identifier, password)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        selectedStoreId = null,
                        showNotificationsScreen = false,
                        cartItems = emptyList(),
                        isBusy = false,
                        snackbarMessage = "Selamat datang, ${user.name}!",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        snackbarMessage = err.message ?: "Gagal masuk.",
                    )
                }
            }
        }
    }

    fun loginDemo(role: UserRole) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            repository.loginDemoRole(role).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        selectedStoreId = null,
                        showNotificationsScreen = false,
                        cartItems = emptyList(),
                        isBusy = false,
                        snackbarMessage = "Masuk sebagai ${role.label}: ${user.name}",
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isBusy = false, snackbarMessage = err.message ?: "Gagal masuk demo.")
                }
            }
        }
    }

    fun register(
        name: String,
        identifier: String,
        password: String,
        role: UserRole,
        address: String,
        storeName: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            repository.register(name, identifier, password, role, address, storeName)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            currentUser = user,
                            selectedStoreId = null,
                            showNotificationsScreen = false,
                            cartItems = emptyList(),
                            isBusy = false,
                            snackbarMessage = "Akun ${role.label} berhasil dibuat!",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isBusy = false, snackbarMessage = err.message ?: "Registrasi gagal.")
                    }
                }
        }
    }

    fun logout() {
        repository.clearSession()
        _uiState.update {
            it.copy(
                currentUser = null,
                selectedStoreId = null,
                showNotificationsScreen = false,
                cartItems = emptyList(),
                searchQuery = "",
                selectedCategory = null,
            )
        }
    }

    fun updateProfile(name: String, contact: String, primaryAddress: String, altAddress: String) {
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.updateUserProfile(current, name, contact, primaryAddress, altAddress)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            currentUser = updated,
                            snackbarMessage = "Profil dan alamat berhasil disimpan.",
                        )
                    }
                }
                .onFailure { err ->
                    showMessage(err.message ?: "Gagal memperbarui profil.")
                }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(category: ProductCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleOnlyOpenStores() {
        _uiState.update { it.copy(onlyOpenStores = !it.onlyOpenStores) }
    }

    fun selectAddressType(usePrimary: Boolean) {
        _uiState.update { it.copy(usePrimaryAddress = usePrimary) }
    }

    fun openStoreDetail(storeId: Long) {
        _uiState.update { it.copy(selectedStoreId = storeId) }
    }

    fun closeStoreDetail() {
        _uiState.update { it.copy(selectedStoreId = null) }
    }

    fun openNotifications() {
        val uid = _uiState.value.currentUser?.id
        _uiState.update { it.copy(showNotificationsScreen = true) }
        if (uid != null) {
            viewModelScope.launch { repository.markNotificationsRead(uid) }
        }
    }

    fun closeNotifications() {
        _uiState.update { it.copy(showNotificationsScreen = false) }
    }

    fun addToCart(product: ProductEntity, store: StoreEntity) {
        if (!store.isOpen) {
            showMessage("${store.name} sedang tutup.")
            return
        }
        if (store.isPaused) {
            showMessage("${store.name} sedang menjeda pesanan baru.")
            return
        }
        if (!product.isAvailable || product.stock <= 0) {
            showMessage("Stok ${product.name} sedang habis.")
            return
        }

        _uiState.update { state ->
            val existingCart = state.cartItems
            // Single-store cart rule for clean delivery fee calculation
            val sameStore = existingCart.isEmpty() || existingCart.first().storeId == store.id
            val baseList = if (sameStore) existingCart else emptyList()
            val idx = baseList.indexOfFirst { it.productId == product.id }
            val updatedList = if (idx >= 0) {
                val item = baseList[idx]
                if (item.quantity >= product.stock) {
                    return@update state.copy(snackbarMessage = "Maksimal stok tersedia: ${product.stock}.")
                }
                baseList.toMutableList().apply {
                    this[idx] = item.copy(quantity = item.quantity + 1, maxStock = product.stock)
                }
            } else {
                baseList + CartItem(
                    productId = product.id,
                    storeId = store.id,
                    storeName = store.name,
                    productName = product.name,
                    categoryLabel = product.category.label,
                    unitPrice = product.price,
                    quantity = 1,
                    maxStock = product.stock,
                    brandColorHex = product.brandColorHex,
                )
            }
            val msg = if (!sameStore) {
                "Keranjang diganti ke ${store.name} & ${product.name} ditambahkan."
            } else {
                "${product.name} ditambahkan ke keranjang."
            }
            state.copy(cartItems = updatedList, snackbarMessage = msg)
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _uiState.update { state ->
            val updated = state.cartItems.mapNotNull { item ->
                if (item.productId != productId) {
                    item
                } else {
                    val nextQty = item.quantity + delta
                    when {
                        nextQty <= 0 -> null
                        nextQty > item.maxStock -> item
                        else -> item.copy(quantity = nextQty)
                    }
                }
            }
            state.copy(cartItems = updated)
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    fun reorder(orderWithItems: OrderWithItems) {
        val store = stores.value.firstOrNull { it.id == orderWithItems.order.storeId }
        if (store == null) {
            showMessage("Toko tidak lagi tersedia.")
            return
        }
        val storeProducts = products.value.filter { it.storeId == store.id }
        val newItems = orderWithItems.items.mapNotNull { line ->
            val prod = storeProducts.firstOrNull { it.id == line.productId }
            if (prod != null && prod.isAvailable && prod.stock > 0) {
                CartItem(
                    productId = prod.id,
                    storeId = store.id,
                    storeName = store.name,
                    productName = prod.name,
                    categoryLabel = prod.category.label,
                    unitPrice = prod.price,
                    quantity = line.quantity.coerceAtMost(prod.stock),
                    maxStock = prod.stock,
                    brandColorHex = prod.brandColorHex,
                )
            } else {
                null
            }
        }
        if (newItems.isEmpty()) {
            showMessage("Produk pada pesanan ini sedang tidak tersedia.")
        } else {
            _uiState.update {
                it.copy(
                    cartItems = newItems,
                    snackbarMessage = "${newItems.size} produk dimasukkan kembali ke keranjang.",
                )
            }
        }
    }

    fun placeOrder(
        address: String,
        notes: String,
        deliverySchedule: String,
        paymentMethod: PaymentMethod,
        onOrderSuccess: () -> Unit,
    ) {
        val buyer = _uiState.value.currentUser ?: return
        val items = _uiState.value.cartItems
        val storeId = items.firstOrNull()?.storeId ?: return
        val store = stores.value.firstOrNull { it.id == storeId } ?: return
        val activeCount = ordersWithItems.value.count {
            it.order.storeId == store.id &&
                it.order.status in listOf(
                    OrderStatus.PENDING_CONFIRMATION,
                    OrderStatus.CONFIRMED,
                    OrderStatus.PREPARING,
                    OrderStatus.DELIVERING,
                )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            repository.placeOrder(
                buyer = buyer,
                store = store,
                cartItems = items,
                activeOrderCountForStore = activeCount,
                address = address,
                notes = notes,
                deliverySchedule = deliverySchedule,
                paymentMethod = paymentMethod,
            ).onSuccess { order ->
                _uiState.update {
                    it.copy(
                        cartItems = emptyList(),
                        isBusy = false,
                        snackbarMessage = "Pesanan ${order.orderCode} berhasil dikirim ke ${store.name}!",
                    )
                }
                onOrderSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        snackbarMessage = err.message ?: "Gagal membuat pesanan.",
                    )
                }
            }
        }
    }

    fun updateOrderStatus(
        order: OrderEntity,
        newStatus: OrderStatus,
        reason: String? = null,
        courier: UserEntity? = null,
        proofNote: String? = null,
    ) {
        viewModelScope.launch {
            repository.updateOrderStatus(order, newStatus, reason, courier, proofNote)
            showMessage("Pesanan ${order.orderCode} -> ${newStatus.label}")
        }
    }

    fun submitReview(order: OrderEntity, rating: Int, comment: String) {
        val buyer = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.submitReview(order, buyer, rating, comment)
            showMessage("Terima kasih atas ulasan Anda!")
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showMessage("Produk ${product.name} disimpan.")
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            showMessage("Produk dihapus.")
        }
    }

    fun updateStore(store: StoreEntity) {
        viewModelScope.launch {
            repository.updateStore(store)
            showMessage("Pengaturan toko diperbarui.")
        }
    }

    fun toggleUserActive(user: UserEntity) {
        viewModelScope.launch {
            repository.toggleUserActive(user)
            showMessage("Status akun ${user.name} diperbarui.")
        }
    }

    fun submitComplaint(title: String, description: String) {
        val user = _uiState.value.currentUser ?: return
        if (title.isBlank() || description.isBlank()) {
            showMessage("Judul dan deskripsi laporan wajib diisi.")
            return
        }
        viewModelScope.launch {
            repository.submitComplaint(user, title, description)
            showMessage("Laporan Anda telah dikirim ke Admin.")
        }
    }

    fun updateComplaint(complaint: ComplaintEntity, status: ComplaintStatus, adminNote: String) {
        viewModelScope.launch {
            repository.updateComplaint(complaint, status, adminNote)
            showMessage("Laporan diperbarui ke ${status.label}.")
        }
    }

    fun addAnnouncement(title: String, subtitle: String, badgeText: String) {
        if (title.isBlank() || subtitle.isBlank()) {
            showMessage("Judul dan keterangan promo wajib diisi.")
            return
        }
        viewModelScope.launch {
            repository.addAnnouncement(title, subtitle, badgeText)
            showMessage("Konten pengumuman ditambahkan.")
        }
    }

    fun toggleAnnouncement(announcement: AnnouncementEntity) {
        viewModelScope.launch {
            repository.toggleAnnouncement(announcement)
        }
    }

    fun deleteAnnouncement(id: Long) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
        }
    }
}
