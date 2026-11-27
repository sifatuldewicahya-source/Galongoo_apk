package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.NotificationEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StoreEntity
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.example.data.model.formatRupiah
import com.example.ui.theme.StatusCancelledBg
import com.example.ui.theme.StatusCancelledFg
import com.example.ui.theme.StatusDoneBg
import com.example.ui.theme.StatusDoneFg
import com.example.ui.theme.StatusProcessBg
import com.example.ui.theme.StatusProcessFg
import com.example.ui.theme.StatusShippingBg
import com.example.ui.theme.StatusShippingFg
import com.example.ui.theme.StatusWaitingBg
import com.example.ui.theme.StatusWaitingFg

@Composable
fun GalonLogoImage(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 14.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.logo_galondekat),
        contentDescription = stringResource(id = R.string.logo_content_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    )
}

@Composable
fun ResponsiveCenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            content = content,
        )
    }
}

@Composable
fun AppBrandTopBar(
    title: String,
    subtitle: String? = null,
    unreadNotifications: Int = 0,
    onNotificationClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                GalonLogoImage(size = 40.dp, cornerRadius = 11.dp)
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (trailingAction != null) {
                trailingAction()
            }

            if (onNotificationClick != null) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("notification_button"),
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifications > 0) {
                                Badge {
                                    Text(unreadNotifications.coerceAtMost(99).toString())
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING_CONFIRMATION -> StatusWaitingBg to StatusWaitingFg
        OrderStatus.CONFIRMED, OrderStatus.PREPARING -> StatusProcessBg to StatusProcessFg
        OrderStatus.DELIVERING -> StatusShippingBg to StatusShippingFg
        OrderStatus.COMPLETED -> StatusDoneBg to StatusDoneFg
        OrderStatus.REJECTED_CANCELLED -> StatusCancelledBg to StatusCancelledFg
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = status.label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun OrderStatusTimeline(status: OrderStatus, modifier: Modifier = Modifier) {
    if (status == OrderStatus.REJECTED_CANCELLED) {
        Surface(
            color = StatusCancelledBg,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dibatalkan",
                    tint = StatusCancelledFg,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pesanan Ditolak / Dibatalkan",
                    style = MaterialTheme.typography.labelLarge,
                    color = StatusCancelledFg,
                )
            }
        }
        return
    }

    val steps = listOf(
        OrderStatus.PENDING_CONFIRMATION to "Menunggu",
        OrderStatus.CONFIRMED to "Dikonfirmasi",
        OrderStatus.PREPARING to "Disiapkan",
        OrderStatus.DELIVERING to "Dikirim",
        OrderStatus.COMPLETED to "Selesai",
    )
    val activeIdx = steps.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { idx, (_, shortLabel) ->
            val reached = idx <= activeIdx
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (idx == 0) Color.Transparent
                                else if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (reached) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp),
                            )
                        } else {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (idx == steps.lastIndex) Color.Transparent
                                else if (idx < activeIdx) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun GallonProductGraphic(
    category: ProductCategory,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    val bgBrush = Brush.linearGradient(
        colors = listOf(tintColor.copy(alpha = 0.16f), tintColor.copy(alpha = 0.05f)),
    )
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(1.dp, tintColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            val w = size.width
            val h = size.height
            when (category) {
                ProductCategory.NEW_GALLON_ACCESSORY -> {
                    // Dispenser / pump silhouette
                    drawRoundRect(
                        color = tintColor,
                        topLeft = Offset(w * 0.26f, h * 0.30f),
                        size = Size(w * 0.48f, h * 0.58f),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                    drawRoundRect(
                        color = tintColor.copy(alpha = 0.7f),
                        topLeft = Offset(w * 0.36f, h * 0.12f),
                        size = Size(w * 0.28f, h * 0.20f),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                    drawLine(
                        color = tintColor,
                        start = Offset(w * 0.65f, h * 0.20f),
                        end = Offset(w * 0.85f, h * 0.32f),
                        strokeWidth = 6f,
                    )
                }
                else -> {
                    // Classic 19L Water Gallon silhouette with ribs & droplet shine
                    drawRoundRect(
                        color = tintColor.copy(alpha = 0.9f),
                        topLeft = Offset(w * 0.38f, h * 0.08f),
                        size = Size(w * 0.24f, h * 0.12f),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                    val shoulder = Path().apply {
                        moveTo(w * 0.38f, h * 0.20f)
                        lineTo(w * 0.62f, h * 0.20f)
                        lineTo(w * 0.78f, h * 0.34f)
                        lineTo(w * 0.22f, h * 0.34f)
                        close()
                    }
                    drawPath(shoulder, color = tintColor.copy(alpha = 0.75f))
                    drawRoundRect(
                        color = tintColor,
                        topLeft = Offset(w * 0.20f, h * 0.32f),
                        size = Size(w * 0.60f, h * 0.58f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                    // Gallon ribs
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f),
                        start = Offset(w * 0.24f, h * 0.49f),
                        end = Offset(w * 0.76f, h * 0.49f),
                        strokeWidth = 3f,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.55f),
                        start = Offset(w * 0.24f, h * 0.66f),
                        end = Offset(w * 0.76f, h * 0.66f),
                        strokeWidth = 3f,
                    )
                    // Droplet highlight for refill categories
                    if (category == ProductCategory.REFILL_DEPOT || category == ProductCategory.OWN_GALLON_REFILL) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = w * 0.09f,
                            center = Offset(w * 0.50f, h * 0.58f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoreCard(
    store: StoreEntity,
    activeOrderCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFull = activeOrderCount >= store.maxActiveOrders
    val canAccept = store.isOpen && !store.isPaused && !isFull && store.isVerified
    val statusLabel = when {
        !store.isVerified -> "Menunggu Verifikasi"
        !store.isOpen -> "Tutup"
        store.isPaused || isFull -> "Antrean Penuh"
        else -> "Buka • ${store.estimatedMinutes} mnt"
    }
    val statusBg = if (canAccept) StatusDoneBg else StatusWaitingBg
    val statusFg = if (canAccept) StatusDoneFg else StatusWaitingFg

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("store_card_${store.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (store.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Terverifikasi",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${store.distanceKm} km • Jam ${store.openHours}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            if (store.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = store.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${store.rating} (${store.reviewCount} ulasan)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = "Ongkir ${formatRupiah(store.deliveryFee)} • Maks ${store.maxServiceRadiusKm} km",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NotificationListOverlay(
    notifications: List<NotificationEntity>,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppBrandTopBar(
            title = "Notifikasi",
            subtitle = "Pembaruan pesanan dan aktivitas akun",
            onBackClick = onBack,
        )
        ResponsiveCenteredColumn(modifier = Modifier.padding(16.dp)) {
            if (notifications.isEmpty()) {
                EmptyStateCard(
                    title = "Belum ada notifikasi",
                    subtitle = "Pemberitahuan pesanan baru atau perubahan status akan muncul di sini.",
                )
            } else {
                notifications.forEach { notif ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = notif.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun OrderEntity.isActiveStatus(): Boolean = status in listOf(
    OrderStatus.PENDING_CONFIRMATION,
    OrderStatus.CONFIRMED,
    OrderStatus.PREPARING,
    OrderStatus.DELIVERING,
)

fun ProductEntity.isLowStock(): Boolean = stock <= minStockThreshold
