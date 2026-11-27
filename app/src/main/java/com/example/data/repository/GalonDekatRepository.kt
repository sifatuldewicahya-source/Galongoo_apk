package com.example.data.repository

import android.content.Context
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ComplaintEntity
import com.example.data.local.GalonDekatDao
import com.example.data.local.GalonDekatDatabase
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
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
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class GalonDekatRepository(
    private val dao: GalonDekatDao,
    context: Context,
) {
    private val prefs = context.getSharedPreferences("galondekat_prefs", Context.MODE_PRIVATE)

    val allUsers: Flow<List<UserEntity>> = dao.observeAllUsers()
    val allStores: Flow<List<StoreEntity>> = dao.observeAllStores()
    val allProducts: Flow<List<ProductEntity>> = dao.observeAllProducts()
    val allOrdersWithItems: Flow<List<OrderWithItems>> = dao.observeAllOrdersWithItems()
    val allReviews: Flow<List<ReviewEntity>> = dao.observeAllReviews()
    val allComplaints: Flow<List<ComplaintEntity>> = dao.observeAllComplaints()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = dao.observeAllAnnouncements()
    val activeCouriers: Flow<List<UserEntity>> = dao.observeUsersByRole(UserRole.COURIER)

    fun observeNotifications(userId: Long): Flow<List<NotificationEntity>> =
        dao.observeNotificationsForUser(userId)

    suspend fun ensureSeeded() {
        GalonDekatDatabase.seedIfEmpty(dao)
    }

    fun getSavedUserId(): Long {
        return prefs.getLong("active_user_id", -1L)
    }

    fun saveSessionUserId(userId: Long) {
        prefs.edit().putLong("active_user_id", userId).apply()
    }

    fun clearSession() {
        prefs.edit().remove("active_user_id").apply()
    }

    suspend fun getUserById(userId: Long): UserEntity? = dao.getUserById(userId)

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val cleanId = identifier.trim()
        if (cleanId.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Nomor telepon/email dan kata sandi wajib diisi."))
        }
        val user = dao.getUserByIdentifier(cleanId)
            ?: return Result.failure(IllegalArgumentException("Akun tidak ditemukan."))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("Akun ini dinonaktifkan oleh Admin."))
        }
        val hash = GalonDekatDatabase.hashPassword(password)
        if (user.passwordHash != hash) {
            return Result.failure(IllegalArgumentException("Kata sandi tidak sesuai."))
        }
        saveSessionUserId(user.id)
        return Result.success(user)
    }

    suspend fun loginDemoRole(role: UserRole): Result<UserEntity> {
        val identifier = when (role) {
            UserRole.BUYER -> "081234567890"
            UserRole.SELLER -> "penjual@galondekat.id"
            UserRole.COURIER -> "kurir@galondekat.id"
            UserRole.ADMIN -> "admin@galondekat.id"
        }
        val user = dao.getUserByIdentifier(identifier)
            ?: return Result.failure(IllegalStateException("Akun demo belum tersedia."))
        if (!user.isActive) {
            return Result.failure(IllegalStateException("Akun demo sedang dinonaktifkan oleh Admin."))
        }
        saveSessionUserId(user.id)
        return Result.success(user)
    }

    suspend fun register(
        name: String,
        identifier: String,
        password: String,
        role: UserRole,
        address: String,
        storeName: String,
    ): Result<UserEntity> {
        val cleanName = name.trim()
        val cleanId = identifier.trim()
        val cleanAddr = address.trim()
        if (cleanName.isBlank() || cleanId.isBlank() || password.length < 4) {
            return Result.failure(IllegalArgumentException("Nama, nomor/email, dan kata sandi (min. 4 karakter) wajib diisi."))
        }
        if (dao.getUserByIdentifier(cleanId) != null) {
            return Result.failure(IllegalArgumentException("Nomor telepon atau email sudah terdaftar."))
        }
        val newUser = UserEntity(
            name = cleanName,
            phoneOrEmail = cleanId,
            passwordHash = GalonDekatDatabase.hashPassword(password),
            role = role,
            primaryAddress = cleanAddr.ifBlank { "Jl. Nusantara No. 10, Pusat Kota" },
            altAddress = "",
            distanceFromCenterKm = 1.0,
            isActive = true,
        )
        val userId = dao.insertUser(newUser)
        val savedUser = newUser.copy(id = userId)
        if (role == UserRole.SELLER) {
            dao.insertStore(
                StoreEntity(
                    sellerId = userId,
                    name = storeName.trim().ifBlank { "Depot $cleanName" },
                    address = savedUser.primaryAddress,
                    distanceKm = 1.1,
                    openHours = "07:00 - 20:00",
                    maxServiceRadiusKm = 5.0,
                    deliveryFee = 2500,
                    estimatedMinutes = 20,
                    isOpen = true,
                    isPaused = false,
                    maxActiveOrders = 10,
                    isVerified = false,
                    rating = 5.0,
                    reviewCount = 0,
                    description = "Toko baru di GalonDekat, siap melayani kebutuhan air minum Anda.",
                ),
            )
        }
        saveSessionUserId(userId)
        return Result.success(savedUser)
    }

    suspend fun updateUserProfile(
        user: UserEntity,
        name: String,
        phoneOrEmail: String,
        primaryAddress: String,
        altAddress: String,
    ): Result<UserEntity> {
        if (name.isBlank() || phoneOrEmail.isBlank() || primaryAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama, kontak, dan alamat utama tidak boleh kosong."))
        }
        val updated = user.copy(
            name = name.trim(),
            phoneOrEmail = phoneOrEmail.trim(),
            primaryAddress = primaryAddress.trim(),
            altAddress = altAddress.trim(),
        )
        dao.updateUser(updated)
        return Result.success(updated)
    }

    suspend fun placeOrder(
        buyer: UserEntity,
        store: StoreEntity,
        cartItems: List<CartItem>,
        activeOrderCountForStore: Int,
        address: String,
        notes: String,
        deliverySchedule: String,
        paymentMethod: PaymentMethod,
    ): Result<OrderEntity> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Keranjang belanja masih kosong."))
        }
        if (address.isBlank()) {
            return Result.failure(IllegalArgumentException("Alamat pengiriman wajib diisi."))
        }
        if (!store.isOpen) {
            return Result.failure(IllegalStateException("Maaf, ${store.name} sedang tutup."))
        }
        if (store.isPaused) {
            return Result.failure(IllegalStateException("${store.name} sedang menjeda penerimaan pesanan."))
        }
        if (activeOrderCountForStore >= store.maxActiveOrders) {
            return Result.failure(IllegalStateException("Kapasitas antrean ${store.name} sedang penuh."))
        }
        if (store.distanceKm > store.maxServiceRadiusKm) {
            return Result.failure(IllegalStateException("Lokasi Anda (${store.distanceKm} km) di luar jangkauan kirim toko (${store.maxServiceRadiusKm} km)."))
        }

        // Validate and deduct stock
        val productsToUpdate = mutableListOf<ProductEntity>()
        for (item in cartItems) {
            val product = dao.getProductById(item.productId)
                ?: return Result.failure(IllegalStateException("Produk ${item.productName} tidak ditemukan."))
            if (!product.isAvailable || product.stock < item.quantity) {
                return Result.failure(IllegalStateException("Stok ${product.name} tidak mencukupi (tersisa ${product.stock})."))
            }
            val remaining = product.stock - item.quantity
            productsToUpdate.add(
                product.copy(
                    stock = remaining,
                    isAvailable = remaining > 0 && product.isAvailable,
                ),
            )
        }
        productsToUpdate.forEach { dao.updateProduct(it) }

        val subtotal = cartItems.sumOf { it.subtotal }
        val total = subtotal + store.deliveryFee
        val now = System.currentTimeMillis()
        val code = "GD-${(1000 + (now % 8999)).toInt()}"

        val newOrder = OrderEntity(
            orderCode = code,
            buyerId = buyer.id,
            buyerName = buyer.name,
            buyerPhone = buyer.phoneOrEmail,
            storeId = store.id,
            storeName = store.name,
            address = address.trim(),
            notes = notes.trim(),
            deliverySchedule = deliverySchedule,
            paymentMethod = paymentMethod,
            paymentStatus = if (paymentMethod == PaymentMethod.COD) "Bayar Tunai saat Terima" else "Menunggu Konfirmasi Pembayaran",
            status = OrderStatus.PENDING_CONFIRMATION,
            subtotal = subtotal,
            deliveryFee = store.deliveryFee,
            totalAmount = total,
            createdAt = now,
            updatedAt = now,
        )
        val orderId = dao.insertOrder(newOrder)
        dao.insertOrderItems(
            cartItems.map {
                OrderItemEntity(
                    orderId = orderId,
                    productId = it.productId,
                    productName = it.productName,
                    categoryLabel = it.categoryLabel,
                    quantity = it.quantity,
                    priceAtTransaction = it.unitPrice,
                )
            },
        )

        dao.insertNotification(
            NotificationEntity(
                userId = store.sellerId,
                title = "Pesanan Baru $code",
                message = "${buyer.name} memesan ${cartItems.sumOf { it.quantity }} item (${paymentMethod.label}).",
                orderId = orderId,
                createdAt = now,
            ),
        )
        dao.insertNotification(
            NotificationEntity(
                userId = buyer.id,
                title = "Pesanan $code Dibuat",
                message = "Pesanan Anda ke ${store.name} menunggu konfirmasi penjual.",
                orderId = orderId,
                createdAt = now,
            ),
        )

        return Result.success(newOrder.copy(id = orderId))
    }

    suspend fun updateOrderStatus(
        order: OrderEntity,
        newStatus: OrderStatus,
        reason: String? = null,
        courier: UserEntity? = null,
        proofNote: String? = null,
    ) {
        val now = System.currentTimeMillis()
        if (newStatus == OrderStatus.REJECTED_CANCELLED && order.status != OrderStatus.REJECTED_CANCELLED) {
            // Restore stock when order is rejected/cancelled
            val items = dao.getOrderItems(order.id)
            for (item in items) {
                val product = dao.getProductById(item.productId)
                if (product != null) {
                    val restored = product.stock + item.quantity
                    dao.updateProduct(product.copy(stock = restored, isAvailable = true))
                }
            }
        }

        val updated = order.copy(
            status = newStatus,
            rejectionReason = reason ?: order.rejectionReason,
            courierId = courier?.id ?: order.courierId,
            courierName = courier?.name ?: order.courierName,
            deliveryProofNote = proofNote ?: order.deliveryProofNote,
            paymentStatus = if (newStatus == OrderStatus.COMPLETED) "Lunas" else order.paymentStatus,
            updatedAt = now,
        )
        dao.updateOrder(updated)

        val buyerMsg = when (newStatus) {
            OrderStatus.CONFIRMED -> "Pesanan ${order.orderCode} telah dikonfirmasi oleh ${order.storeName}."
            OrderStatus.PREPARING -> "Pesanan ${order.orderCode} sedang disiapkan."
            OrderStatus.DELIVERING -> "Pesanan ${order.orderCode} sedang dikirim${courier?.let { " oleh ${it.name}" } ?: ""}."
            OrderStatus.COMPLETED -> "Pesanan ${order.orderCode} telah selesai diterima. Terima kasih!"
            OrderStatus.REJECTED_CANCELLED -> "Pesanan ${order.orderCode} dibatalkan: ${reason ?: "Tanpa keterangan"}."
            else -> "Status pesanan ${order.orderCode} diperbarui."
        }
        dao.insertNotification(
            NotificationEntity(
                userId = order.buyerId,
                title = "Update ${order.orderCode}: ${newStatus.label}",
                message = buyerMsg,
                orderId = order.id,
                createdAt = now,
            ),
        )

        if (courier != null && newStatus == OrderStatus.DELIVERING) {
            dao.insertNotification(
                NotificationEntity(
                    userId = courier.id,
                    title = "Tugas Pengantaran ${order.orderCode}",
                    message = "Antar ke ${order.buyerName} (${order.address}).",
                    orderId = order.id,
                    createdAt = now,
                ),
            )
        }
    }

    suspend fun submitReview(
        order: OrderEntity,
        buyer: UserEntity,
        rating: Int,
        comment: String,
    ) {
        val cleanRating = rating.coerceIn(1, 5)
        dao.insertReview(
            ReviewEntity(
                orderId = order.id,
                buyerId = buyer.id,
                buyerName = buyer.name,
                storeId = order.storeId,
                rating = cleanRating,
                comment = comment.trim().ifBlank { "Pelayanan baik dan cepat." },
            ),
        )
        dao.updateOrder(order.copy(hasReview = true))
        val store = dao.getStoreById(order.storeId) ?: return
        val reviews = dao.getReviewsForStore(store.id)
        val avg = if (reviews.isEmpty()) 5.0 else reviews.map { it.rating }.average()
        val rounded = (avg * 10.0).roundToInt() / 10.0
        dao.updateStore(store.copy(rating = rounded, reviewCount = reviews.size))
    }

    suspend fun saveProduct(product: ProductEntity) {
        if (product.id == 0L) {
            dao.insertProduct(product)
        } else {
            dao.updateProduct(product)
        }
    }

    suspend fun deleteProduct(productId: Long) {
        dao.deleteProductById(productId)
    }

    suspend fun updateStore(store: StoreEntity) {
        dao.updateStore(store)
    }

    suspend fun toggleUserActive(user: UserEntity) {
        dao.updateUser(user.copy(isActive = !user.isActive))
    }

    suspend fun markNotificationsRead(userId: Long) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun submitComplaint(user: UserEntity, title: String, description: String) {
        dao.insertComplaint(
            ComplaintEntity(
                reporterId = user.id,
                reporterName = user.name,
                reporterRole = user.role.label,
                title = title.trim(),
                description = description.trim(),
            ),
        )
    }

    suspend fun updateComplaint(complaint: ComplaintEntity, status: ComplaintStatus, adminNote: String) {
        dao.updateComplaint(complaint.copy(status = status, adminNote = adminNote.trim()))
    }

    suspend fun addAnnouncement(title: String, subtitle: String, badgeText: String) {
        dao.insertAnnouncement(
            AnnouncementEntity(
                title = title.trim(),
                subtitle = subtitle.trim(),
                badgeText = badgeText.trim().ifBlank { "INFO" },
                isActive = true,
            ),
        )
    }

    suspend fun toggleAnnouncement(announcement: AnnouncementEntity) {
        dao.updateAnnouncement(announcement.copy(isActive = !announcement.isActive))
    }

    suspend fun deleteAnnouncement(id: Long) {
        dao.deleteAnnouncementById(id)
    }
}
