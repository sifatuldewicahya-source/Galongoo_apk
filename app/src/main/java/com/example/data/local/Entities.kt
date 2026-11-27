package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.data.model.ComplaintStatus
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneOrEmail: String,
    val passwordHash: String,
    val role: UserRole,
    val primaryAddress: String,
    val altAddress: String = "",
    val distanceFromCenterKm: Double = 1.2,
    val isActive: Boolean = true,
)

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sellerId: Long,
    val name: String,
    val address: String,
    val distanceKm: Double,
    val openHours: String,
    val maxServiceRadiusKm: Double,
    val deliveryFee: Int,
    val estimatedMinutes: Int,
    val isOpen: Boolean = true,
    val isPaused: Boolean = false,
    val maxActiveOrders: Int = 10,
    val isVerified: Boolean = true,
    val rating: Double = 4.8,
    val reviewCount: Int = 0,
    val description: String = "",
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storeId: Long,
    val name: String,
    val category: ProductCategory,
    val description: String,
    val price: Int,
    val stock: Int,
    val minStockThreshold: Int = 5,
    val isAvailable: Boolean = true,
    val brandColorHex: Long = 0xFF1E88E5,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderCode: String,
    val buyerId: Long,
    val buyerName: String,
    val buyerPhone: String,
    val storeId: Long,
    val storeName: String,
    val courierId: Long? = null,
    val courierName: String? = null,
    val address: String,
    val notes: String,
    val deliverySchedule: String,
    val paymentMethod: PaymentMethod,
    val paymentStatus: String,
    val status: OrderStatus,
    val rejectionReason: String? = null,
    val deliveryProofNote: String? = null,
    val subtotal: Int,
    val deliveryFee: Int,
    val totalAmount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hasReview: Boolean = false,
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val categoryLabel: String,
    val quantity: Int,
    val priceAtTransaction: Int,
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
    )
    val items: List<OrderItemEntity>,
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val buyerId: Long,
    val buyerName: String,
    val storeId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val orderId: Long? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val reporterName: String,
    val reporterRole: String,
    val title: String,
    val description: String,
    val status: ComplaintStatus = ComplaintStatus.BARU,
    val adminNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val isActive: Boolean = true,
)

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromProductCategory(category: ProductCategory): String = category.name

    @TypeConverter
    fun toProductCategory(value: String): ProductCategory = ProductCategory.valueOf(value)

    @TypeConverter
    fun fromOrderStatus(status: OrderStatus): String = status.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromComplaintStatus(status: ComplaintStatus): String = status.name

    @TypeConverter
    fun toComplaintStatus(value: String): ComplaintStatus = ComplaintStatus.valueOf(value)
}
