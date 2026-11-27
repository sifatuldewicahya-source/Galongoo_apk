package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.ComplaintStatus
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        StoreEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ReviewEntity::class,
        NotificationEntity::class,
        ComplaintEntity::class,
        AnnouncementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GalonDekatDatabase : RoomDatabase() {
    abstract fun dao(): GalonDekatDao

    companion object {
        @Volatile
        private var INSTANCE: GalonDekatDatabase? = null

        fun getInstance(context: Context): GalonDekatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GalonDekatDatabase::class.java,
                    "galondekat.db",
                ).build().also { INSTANCE = it }
            }
        }

        fun hashPassword(raw: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        suspend fun seedIfEmpty(dao: GalonDekatDao) {
            if (dao.countUsers() > 0) return

            val demoPass = hashPassword("123456")
            val now = System.currentTimeMillis()
            val hour = 3_600_000L

            // Users
            val buyerId = dao.insertUser(
                UserEntity(
                    name = "Siti Rahma",
                    phoneOrEmail = "081234567890",
                    passwordHash = demoPass,
                    role = UserRole.BUYER,
                    primaryAddress = "Jl. Melati Indah No. 14, Kel. Sukamaju",
                    altAddress = "Kantor Kelurahan Sukamaju, Jl. Kenanga No. 3",
                    distanceFromCenterKm = 0.8,
                ),
            )
            val seller1Id = dao.insertUser(
                UserEntity(
                    name = "Budi Santoso",
                    phoneOrEmail = "penjual@galondekat.id",
                    passwordHash = demoPass,
                    role = UserRole.SELLER,
                    primaryAddress = "Jl. Merdeka Raya No. 22, Sukamaju",
                ),
            )
            val seller2Id = dao.insertUser(
                UserEntity(
                    name = "Hendra Wijaya",
                    phoneOrEmail = "081398765432",
                    passwordHash = demoPass,
                    role = UserRole.SELLER,
                    primaryAddress = "Jl. Diponegoro No. 88, Harapan Jaya",
                ),
            )
            val seller3Id = dao.insertUser(
                UserEntity(
                    name = "Dewi Lestari",
                    phoneOrEmail = "085711223344",
                    passwordHash = demoPass,
                    role = UserRole.SELLER,
                    primaryAddress = "Jl. Flamboyan Blok C2, Taman Asri",
                ),
            )
            val courier1Id = dao.insertUser(
                UserEntity(
                    name = "Riko Pratama",
                    phoneOrEmail = "kurir@galondekat.id",
                    passwordHash = demoPass,
                    role = UserRole.COURIER,
                    primaryAddress = "Pos Kurir Sukamaju",
                ),
            )
            dao.insertUser(
                UserEntity(
                    name = "Damar Putra",
                    phoneOrEmail = "081900112233",
                    passwordHash = demoPass,
                    role = UserRole.COURIER,
                    primaryAddress = "Pos Kurir Harapan Jaya",
                ),
            )
            dao.insertUser(
                UserEntity(
                    name = "Admin GalonDekat",
                    phoneOrEmail = "admin@galondekat.id",
                    passwordHash = demoPass,
                    role = UserRole.ADMIN,
                    primaryAddress = "Pusat Operasional GalonDekat",
                ),
            )

            // Stores
            val store1Id = dao.insertStore(
                StoreEntity(
                    sellerId = seller1Id,
                    name = "Depot Tirta Segar",
                    address = "Jl. Merdeka Raya No. 22, Sukamaju",
                    distanceKm = 0.6,
                    openHours = "07:00 - 21:00",
                    maxServiceRadiusKm = 5.0,
                    deliveryFee = 2000,
                    estimatedMinutes = 15,
                    isOpen = true,
                    isPaused = false,
                    maxActiveOrders = 12,
                    isVerified = true,
                    rating = 4.9,
                    reviewCount = 2,
                    description = "Depot air RO higienis & agen resmi galon bermerek. Antar cepat maksimal 20 menit.",
                ),
            )
            val store2Id = dao.insertStore(
                StoreEntity(
                    sellerId = seller2Id,
                    name = "Sumber Bening Jaya",
                    address = "Jl. Diponegoro No. 88, Harapan Jaya",
                    distanceKm = 1.4,
                    openHours = "06:30 - 20:30",
                    maxServiceRadiusKm = 4.0,
                    deliveryFee = 3000,
                    estimatedMinutes = 25,
                    isOpen = true,
                    isPaused = false,
                    maxActiveOrders = 10,
                    isVerified = true,
                    rating = 4.7,
                    reviewCount = 1,
                    description = "Menyediakan air pegunungan bermerek, isi ulang galon pribadi, dan pompa galon.",
                ),
            )
            val store3Id = dao.insertStore(
                StoreEntity(
                    sellerId = seller3Id,
                    name = "Depot Embun Pagi",
                    address = "Jl. Flamboyan Blok C2, Taman Asri",
                    distanceKm = 2.8,
                    openHours = "08:00 - 19:00",
                    maxServiceRadiusKm = 2.0,
                    deliveryFee = 4000,
                    estimatedMinutes = 35,
                    isOpen = true,
                    isPaused = false,
                    maxActiveOrders = 8,
                    isVerified = false,
                    rating = 4.5,
                    reviewCount = 0,
                    description = "Depot air minum isi ulang bio-mineral (menunggu verifikasi admin).",
                ),
            )

            // Products Store 1
            val p1 = dao.insertProduct(
                ProductEntity(
                    storeId = store1Id,
                    name = "Aqua Galon 19L (Isi Ulang)",
                    category = ProductCategory.BRANDED_GALLON,
                    description = "Galon asli Aqua 19 liter tersegel pabrik, tukar galon kosong.",
                    price = 21000,
                    stock = 32,
                    minStockThreshold = 8,
                    brandColorHex = 0xFF1565C0,
                ),
            )
            val p2 = dao.insertProduct(
                ProductEntity(
                    storeId = store1Id,
                    name = "Le Minerale 15L (Sekali Pakai)",
                    category = ProductCategory.BRANDED_GALLON,
                    description = "Air mineral pegunungan galon 15 liter higienis tanpa tukar galon.",
                    price = 22500,
                    stock = 18,
                    minStockThreshold = 6,
                    brandColorHex = 0xFF0284C7,
                ),
            )
            val p3 = dao.insertProduct(
                ProductEntity(
                    storeId = store1Id,
                    name = "Air Isi Ulang RO Higienis 19L",
                    category = ProductCategory.REFILL_DEPOT,
                    description = "Filtrasi Reverse Osmosis 8 tahap + sterilisasi UV, segar dan bebas bau.",
                    price = 6000,
                    stock = 45,
                    minStockThreshold = 10,
                    brandColorHex = 0xFF0EA5E9,
                ),
            )
            val p4 = dao.insertProduct(
                ProductEntity(
                    storeId = store1Id,
                    name = "Layanan Isi Ulang Galon Pribadi",
                    category = ProductCategory.OWN_GALLON_REFILL,
                    description = "Isi ulang menggunakan galon milik pembeli (cuci & bilas ozon gratis).",
                    price = 5000,
                    stock = 4, // intentionally low stock to trigger low-stock alert for seller!
                    minStockThreshold = 5,
                    brandColorHex = 0xFF0891B2,
                ),
            )
            dao.insertProduct(
                ProductEntity(
                    storeId = store1Id,
                    name = "Galon Kosong Baru 19L + Tutup",
                    category = ProductCategory.NEW_GALLON_ACCESSORY,
                    description = "Galon polikarbonat food-grade baru termasuk isi air RO pertama.",
                    price = 48000,
                    stock = 9,
                    minStockThreshold = 3,
                    brandColorHex = 0xFF2563EB,
                ),
            )

            // Products Store 2
            val p6 = dao.insertProduct(
                ProductEntity(
                    storeId = store2Id,
                    name = "Vit Galon 19L",
                    category = ProductCategory.BRANDED_GALLON,
                    description = "Air minum Vit 19L segel resmi distributor, tukar galon kosong.",
                    price = 18500,
                    stock = 25,
                    minStockThreshold = 6,
                    brandColorHex = 0xFF1D4ED8,
                ),
            )
            dao.insertProduct(
                ProductEntity(
                    storeId = store2Id,
                    name = "Air Isi Ulang Bio Mineral 19L",
                    category = ProductCategory.REFILL_DEPOT,
                    description = "Air minum isi ulang pH seimbang dengan filter keramik bio.",
                    price = 7000,
                    stock = 40,
                    minStockThreshold = 10,
                    brandColorHex = 0xFF0284C7,
                ),
            )
            dao.insertProduct(
                ProductEntity(
                    storeId = store2Id,
                    name = "Pompa Galon Elektrik USB",
                    category = ProductCategory.NEW_GALLON_ACCESSORY,
                    description = "Dispenser pompa otomatis isi ulang daya USB, praktis untuk meja.",
                    price = 35000,
                    stock = 7,
                    minStockThreshold = 2,
                    brandColorHex = 0xFFF59E0B,
                ),
            )

            // Products Store 3 (unverified store)
            dao.insertProduct(
                ProductEntity(
                    storeId = store3Id,
                    name = "Air Isi Ulang Embun 19L",
                    category = ProductCategory.REFILL_DEPOT,
                    description = "Air minum isi ulang higienis.",
                    price = 5500,
                    stock = 30,
                    minStockThreshold = 5,
                    brandColorHex = 0xFF0EA5E9,
                ),
            )

            // Sample Orders
            val o1 = dao.insertOrder(
                OrderEntity(
                    orderCode = "GD-2601",
                    buyerId = buyerId,
                    buyerName = "Siti Rahma",
                    buyerPhone = "081234567890",
                    storeId = store1Id,
                    storeName = "Depot Tirta Segar",
                    courierId = null,
                    courierName = null,
                    address = "Jl. Melati Indah No. 14, Kel. Sukamaju",
                    notes = "Taruh di teras depan kalau belum dibukakan pintu.",
                    deliverySchedule = "Segera (15-20 menit)",
                    paymentMethod = PaymentMethod.COD,
                    paymentStatus = "Menunggu Pembayaran",
                    status = OrderStatus.PENDING_CONFIRMATION,
                    subtotal = 42000,
                    deliveryFee = 2000,
                    totalAmount = 44000,
                    createdAt = now - (hour / 4),
                    updatedAt = now - (hour / 4),
                ),
            )
            dao.insertOrderItems(
                listOf(
                    OrderItemEntity(
                        orderId = o1,
                        productId = p1,
                        productName = "Aqua Galon 19L (Isi Ulang)",
                        categoryLabel = ProductCategory.BRANDED_GALLON.label,
                        quantity = 2,
                        priceAtTransaction = 21000,
                    ),
                ),
            )

            val o2 = dao.insertOrder(
                OrderEntity(
                    orderCode = "GD-2602",
                    buyerId = buyerId,
                    buyerName = "Siti Rahma",
                    buyerPhone = "081234567890",
                    storeId = store1Id,
                    storeName = "Depot Tirta Segar",
                    courierId = courier1Id,
                    courierName = "Riko Pratama",
                    address = "Jl. Melati Indah No. 14, Kel. Sukamaju",
                    notes = "Galon kosong sudah disiapkan di pagar.",
                    deliverySchedule = "Segera (15-20 menit)",
                    paymentMethod = PaymentMethod.COD,
                    paymentStatus = "Menunggu Pembayaran",
                    status = OrderStatus.DELIVERING,
                    subtotal = 28500,
                    deliveryFee = 2000,
                    totalAmount = 30500,
                    createdAt = now - hour,
                    updatedAt = now - (hour / 2),
                ),
            )
            dao.insertOrderItems(
                listOf(
                    OrderItemEntity(
                        orderId = o2,
                        productId = p2,
                        productName = "Le Minerale 15L (Sekali Pakai)",
                        categoryLabel = ProductCategory.BRANDED_GALLON.label,
                        quantity = 1,
                        priceAtTransaction = 22500,
                    ),
                    OrderItemEntity(
                        orderId = o2,
                        productId = p3,
                        productName = "Air Isi Ulang RO Higienis 19L",
                        categoryLabel = ProductCategory.REFILL_DEPOT.label,
                        quantity = 1,
                        priceAtTransaction = 6000,
                    ),
                ),
            )

            val o3 = dao.insertOrder(
                OrderEntity(
                    orderCode = "GD-2598",
                    buyerId = buyerId,
                    buyerName = "Siti Rahma",
                    buyerPhone = "081234567890",
                    storeId = store1Id,
                    storeName = "Depot Tirta Segar",
                    courierId = courier1Id,
                    courierName = "Riko Pratama",
                    address = "Jl. Melati Indah No. 14, Kel. Sukamaju",
                    notes = "",
                    deliverySchedule = "Pagi (08:00 - 11:00)",
                    paymentMethod = PaymentMethod.COD,
                    paymentStatus = "Lunas",
                    status = OrderStatus.COMPLETED,
                    deliveryProofNote = "Diterima langsung oleh Ibu Siti di rumah.",
                    subtotal = 33000,
                    deliveryFee = 2000,
                    totalAmount = 35000,
                    createdAt = now - 26 * hour,
                    updatedAt = now - 25 * hour,
                    hasReview = true,
                ),
            )
            dao.insertOrderItems(
                listOf(
                    OrderItemEntity(
                        orderId = o3,
                        productId = p1,
                        productName = "Aqua Galon 19L (Isi Ulang)",
                        categoryLabel = ProductCategory.BRANDED_GALLON.label,
                        quantity = 1,
                        priceAtTransaction = 21000,
                    ),
                    OrderItemEntity(
                        orderId = o3,
                        productId = p3,
                        productName = "Air Isi Ulang RO Higienis 19L",
                        categoryLabel = ProductCategory.REFILL_DEPOT.label,
                        quantity = 2,
                        priceAtTransaction = 6000,
                    ),
                ),
            )

            val o4 = dao.insertOrder(
                OrderEntity(
                    orderCode = "GD-2595",
                    buyerId = buyerId,
                    buyerName = "Siti Rahma",
                    buyerPhone = "081234567890",
                    storeId = store2Id,
                    storeName = "Sumber Bening Jaya",
                    courierId = courier1Id,
                    courierName = "Riko Pratama",
                    address = "Kantor Kelurahan Sukamaju, Jl. Kenanga No. 3",
                    notes = "Antar ke meja resepsionis.",
                    deliverySchedule = "Siang (13:00 - 16:00)",
                    paymentMethod = PaymentMethod.QRIS,
                    paymentStatus = "Lunas",
                    status = OrderStatus.COMPLETED,
                    deliveryProofNote = "Diterima staf resepsionis.",
                    subtotal = 37000,
                    deliveryFee = 3000,
                    totalAmount = 40000,
                    createdAt = now - 50 * hour,
                    updatedAt = now - 49 * hour,
                    hasReview = false,
                ),
            )
            dao.insertOrderItems(
                listOf(
                    OrderItemEntity(
                        orderId = o4,
                        productId = p6,
                        productName = "Vit Galon 19L",
                        categoryLabel = ProductCategory.BRANDED_GALLON.label,
                        quantity = 2,
                        priceAtTransaction = 18500,
                    ),
                ),
            )

            // Reviews
            dao.insertReview(
                ReviewEntity(
                    orderId = o3,
                    buyerId = buyerId,
                    buyerName = "Siti Rahma",
                    storeId = store1Id,
                    rating = 5,
                    comment = "Pengiriman sangat cepat, kurir ramah dan galon bersih tersegel.",
                    createdAt = now - 24 * hour,
                ),
            )
            dao.insertReview(
                ReviewEntity(
                    orderId = o3,
                    buyerId = buyerId,
                    buyerName = "Andi Kusuma",
                    storeId = store1Id,
                    rating = 5,
                    comment = "Air RO segar dan selalu ready setiap pagi.",
                    createdAt = now - 72 * hour,
                ),
            )
            dao.insertReview(
                ReviewEntity(
                    orderId = o4,
                    buyerId = buyerId,
                    buyerName = "Rina Amelia",
                    storeId = store2Id,
                    rating = 5,
                    comment = "Harga bersahabat, pelayanan rapi.",
                    createdAt = now - 96 * hour,
                ),
            )

            // Notifications
            dao.insertNotification(
                NotificationEntity(
                    userId = buyerId,
                    title = "Pesanan GD-2602 Sedang Dikirim",
                    message = "Kurir Riko Pratama sedang menuju alamat Anda dari Depot Tirta Segar.",
                    orderId = o2,
                    isRead = false,
                    createdAt = now - (hour / 2),
                ),
            )
            dao.insertNotification(
                NotificationEntity(
                    userId = seller1Id,
                    title = "Pesanan Baru GD-2601",
                    message = "Siti Rahma memesan 2x Aqua Galon 19L (COD). Segera konfirmasi!",
                    orderId = o1,
                    isRead = false,
                    createdAt = now - (hour / 4),
                ),
            )
            dao.insertNotification(
                NotificationEntity(
                    userId = courier1Id,
                    title = "Tugas Pengantaran GD-2602",
                    message = "Ambil pesanan di Depot Tirta Segar untuk Jl. Melati Indah No. 14.",
                    orderId = o2,
                    isRead = false,
                    createdAt = now - (hour / 2),
                ),
            )

            // Announcements
            dao.insertAnnouncement(
                AnnouncementEntity(
                    title = "Antar Cepat Galon Terdekat",
                    subtitle = "Pesan dari depot terverifikasi mulai Rp5.000 dengan pelacakan status langsung.",
                    badgeText = "PROMO LOKAL",
                    isActive = true,
                ),
            )
            dao.insertAnnouncement(
                AnnouncementEntity(
                    title = "Jaga Kebersihan Galon Anda",
                    subtitle = "Pilih layanan Isi Ulang Galon Sendiri untuk bilas ozon gratis di depot mitra.",
                    badgeText = "INFO SEHAT",
                    isActive = true,
                ),
            )

            // Sample Complaint
            dao.insertComplaint(
                ComplaintEntity(
                    reporterId = buyerId,
                    reporterName = "Siti Rahma",
                    reporterRole = UserRole.BUYER.label,
                    title = "Estimasi waktu pengiriman saat hujan",
                    description = "Mohon tambahkan informasi jika depot sedang menunda pengiriman saat hujan deras.",
                    status = ComplaintStatus.BARU,
                    createdAt = now - 5 * hour,
                ),
            )
        }
    }
}
