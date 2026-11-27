package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.ProductEntity
import com.example.data.local.StoreEntity
import com.example.data.local.UserEntity
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.example.data.model.SalesPeriod
import com.example.data.model.formatRupiah
import com.example.data.model.formatShortTime
import com.example.data.model.formatTimestamp
import com.example.ui.components.AppBrandTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GallonProductGraphic
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ResponsiveCenteredColumn
import com.example.ui.components.isLowStock
import com.example.ui.theme.StatusDoneBg
import com.example.ui.theme.StatusDoneFg
import com.example.ui.theme.StatusProcessBg
import com.example.ui.theme.StatusProcessFg
import com.example.ui.theme.StatusWaitingBg
import com.example.ui.theme.StatusWaitingFg

@Composable
fun SellerDashboardScreen(
    user: UserEntity,
    store: StoreEntity?,
    products: List<ProductEntity>,
    orders: List<OrderWithItems>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onUpdateOrderStatus: (OrderEntity, OrderStatus, String?, UserEntity?) -> Unit,
    onGoToOrdersTab: () -> Unit,
    onGoToProductsTab: () -> Unit,
) {
    if (store == null) {
        EmptyStateCard("Toko belum terdaftar", "Silakan atur profil toko Anda.")
        return
    }

    val storeOrders = remember(orders, store.id) {
        orders.filter { it.order.storeId == store.id }
    }
    val newOrders = storeOrders.filter { it.order.status == OrderStatus.PENDING_CONFIRMATION }
    val processingCount = storeOrders.count {
        it.order.status == OrderStatus.CONFIRMED || it.order.status == OrderStatus.PREPARING
    }
    val deliveringCount = storeOrders.count { it.order.status == OrderStatus.DELIVERING }
    val completedOrders = storeOrders.filter { it.order.status == OrderStatus.COMPLETED }
    val todayCutoff = remember { System.currentTimeMillis() - 24 * 3_600_000L }
    val todaySales = completedOrders.filter { it.order.createdAt >= todayCutoff }.sumOf { it.order.totalAmount }
    val lowStockProducts = products.filter { it.storeId == store.id && it.isLowStock() }

    var rejectTarget by remember { mutableStateOf<OrderEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = store.name,
            subtitle = "Dashboard Penjual • ${if (store.isOpen) "Toko Buka" else "Toko Tutup"}",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!store.isVerified) {
                item {
                    ResponsiveCenteredColumn {
                        Surface(
                            color = StatusWaitingBg,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Toko Anda sedang menunggu verifikasi Admin sebelum tampil di daftar Pembeli.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusWaitingFg,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // Summary Grid
            item {
                ResponsiveCenteredColumn {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DashboardMetricCard(
                            title = "Pesanan Baru",
                            value = "${newOrders.size}",
                            bgColor = StatusWaitingBg,
                            fgColor = StatusWaitingFg,
                            onClick = onGoToOrdersTab,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardMetricCard(
                            title = "Diproses & Kirim",
                            value = "${processingCount + deliveringCount}",
                            bgColor = StatusProcessBg,
                            fgColor = StatusProcessFg,
                            onClick = onGoToOrdersTab,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DashboardMetricCard(
                            title = "Selesai",
                            value = "${completedOrders.size}",
                            bgColor = StatusDoneBg,
                            fgColor = StatusDoneFg,
                            onClick = onGoToOrdersTab,
                            modifier = Modifier.weight(1f),
                        )
                        DashboardMetricCard(
                            title = "Penjualan 24 Jam",
                            value = formatRupiah(todaySales),
                            bgColor = MaterialTheme.colorScheme.primaryContainer,
                            fgColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onGoToOrdersTab,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Low-stock warning
            if (lowStockProducts.isNotEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            onClick = onGoToProductsTab,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Peringatan stok",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Peringatan Stok Menipis (${lowStockProducts.size} Produk)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = lowStockProducts.joinToString { "${it.name} (${it.stock})" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Incoming Orders Queue
            item {
                ResponsiveCenteredColumn {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Antrean Pesanan Masuk", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onGoToOrdersTab) { Text("Kelola Semua") }
                    }
                }
            }

            if (newOrders.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = "Belum ada pesanan baru menunggu",
                            subtitle = "Pesanan masuk dari pembeli akan otomatis muncul di sini untuk dikonfirmasi.",
                        )
                    }
                }
            } else {
                // Sort oldest first as queue (FIFO)
                items(newOrders.sortedBy { it.order.createdAt }, key = { it.order.id }) { item ->
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
                                ) {
                                    Text(
                                        text = "${order.orderCode} • ${formatShortTime(order.createdAt)}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    OrderStatusBadge(order.status)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("${order.buyerName} (${order.buyerPhone})", style = MaterialTheme.typography.titleSmall)
                                Text(order.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                item.items.forEach { line ->
                                    Text("• ${line.quantity}x ${line.productName}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Total ${formatRupiah(order.totalAmount)} (${order.paymentMethod.label})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { rejectTarget = order },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tolak")
                                    }
                                    Button(
                                        onClick = { onUpdateOrderStatus(order, OrderStatus.CONFIRMED, null, null) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Terima")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejectTarget?.let { target ->
        RejectOrderDialog(
            orderCode = target.orderCode,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                onUpdateOrderStatus(target, OrderStatus.REJECTED_CANCELLED, reason, null)
                rejectTarget = null
            },
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = fgColor.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = fgColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SellerOrdersScreen(
    store: StoreEntity?,
    orders: List<OrderWithItems>,
    couriers: List<UserEntity>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onUpdateOrderStatus: (OrderEntity, OrderStatus, String?, UserEntity?) -> Unit,
) {
    if (store == null) return

    val tabs = listOf("Baru", "Diproses", "Dikirim", "Selesai", "Dibatalkan")
    var selectedTab by remember { mutableIntStateOf(0) }
    var rejectTarget by remember { mutableStateOf<OrderEntity?>(null) }
    var assignCourierTarget by remember { mutableStateOf<OrderEntity?>(null) }

    val storeOrders = remember(orders, store.id) {
        orders.filter { it.order.storeId == store.id }
    }
    val filtered = remember(storeOrders, selectedTab) {
        when (selectedTab) {
            0 -> storeOrders.filter { it.order.status == OrderStatus.PENDING_CONFIRMATION }.sortedBy { it.order.createdAt }
            1 -> storeOrders.filter { it.order.status == OrderStatus.CONFIRMED || it.order.status == OrderStatus.PREPARING }.sortedBy { it.order.createdAt }
            2 -> storeOrders.filter { it.order.status == OrderStatus.DELIVERING }
            3 -> storeOrders.filter { it.order.status == OrderStatus.COMPLETED }
            else -> storeOrders.filter { it.order.status == OrderStatus.REJECTED_CANCELLED }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Manajemen Pesanan",
            subtitle = "Antrean terurut dari waktu masuk terawal",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { idx, label ->
                FilterChip(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    label = { Text(label) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = "Tidak ada pesanan di tab ${tabs[selectedTab]}",
                            subtitle = "Pesanan dengan status tersebut akan tampil di daftar ini.",
                        )
                    }
                }
            } else {
                items(filtered, key = { it.order.id }) { item ->
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
                                ) {
                                    Column {
                                        Text(order.orderCode, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = formatTimestamp(order.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    OrderStatusBadge(order.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${order.buyerName} • ${order.buyerPhone}", style = MaterialTheme.typography.titleSmall)
                                Text(order.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (order.notes.isNotBlank()) {
                                    Text("Catatan: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                item.items.forEach { line ->
                                    Text("• ${line.quantity}x ${line.productName} (${formatRupiah(line.priceAtTransaction * line.quantity)})", style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${order.paymentMethod.label} • ${order.deliverySchedule}", style = MaterialTheme.typography.bodySmall)
                                    Text(formatRupiah(order.totalAmount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (!order.courierName.isNullOrBlank()) {
                                    Text("Kurir: ${order.courierName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                }
                                if (!order.rejectionReason.isNullOrBlank()) {
                                    Text("Alasan: ${order.rejectionReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                when (order.status) {
                                    OrderStatus.PENDING_CONFIRMATION -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { rejectTarget = order },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.weight(1f),
                                            ) { Text("Tolak") }
                                            Button(
                                                onClick = { onUpdateOrderStatus(order, OrderStatus.CONFIRMED, null, null) },
                                                modifier = Modifier.weight(1f),
                                            ) { Text("Konfirmasi") }
                                        }
                                    }
                                    OrderStatus.CONFIRMED -> {
                                        Button(
                                            onClick = { onUpdateOrderStatus(order, OrderStatus.PREPARING, null, null) },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text("Siapkan Galon") }
                                    }
                                    OrderStatus.PREPARING -> {
                                        Button(
                                            onClick = { assignCourierTarget = order },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tugaskan Kurir & Kirim")
                                        }
                                    }
                                    OrderStatus.DELIVERING -> {
                                        Button(
                                            onClick = { onUpdateOrderStatus(order, OrderStatus.COMPLETED, null, null) },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) { Text("Tandai Selesai Diterima") }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejectTarget?.let { target ->
        RejectOrderDialog(
            orderCode = target.orderCode,
            onDismiss = { rejectTarget = null },
            onConfirm = { reason ->
                onUpdateOrderStatus(target, OrderStatus.REJECTED_CANCELLED, reason, null)
                rejectTarget = null
            },
        )
    }

    assignCourierTarget?.let { target ->
        AssignCourierDialog(
            orderCode = target.orderCode,
            couriers = couriers,
            onDismiss = { assignCourierTarget = null },
            onConfirm = { selectedCourier ->
                onUpdateOrderStatus(target, OrderStatus.DELIVERING, null, selectedCourier)
                assignCourierTarget = null
            },
        )
    }
}

@Composable
private fun RejectOrderDialog(
    orderCode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val quickReasons = listOf("Stok produk habis", "Alamat di luar jangkauan kirim", "Kapasitas antrean sedang penuh")
    var reason by remember { mutableStateOf(quickReasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tolak Pesanan $orderCode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pilih atau tulis alasan penolakan:", style = MaterialTheme.typography.bodySmall)
                quickReasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reason = r },
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason.ifBlank { "Ditolak penjual" }) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Tolak Pesanan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AssignCourierDialog(
    orderCode: String,
    couriers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (UserEntity?) -> Unit,
) {
    var selectedId by remember { mutableStateOf(couriers.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kirim Pesanan $orderCode") },
        text = {
            Column {
                Text("Pilih kurir untuk mengantar pesanan ini:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                couriers.forEach { courier ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = courier.id },
                    ) {
                        RadioButton(selected = selectedId == courier.id, onClick = { selectedId = courier.id })
                        Column {
                            Text(courier.name, style = MaterialTheme.typography.titleSmall)
                            Text(courier.primaryAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = null },
                ) {
                    RadioButton(selected = selectedId == null, onClick = { selectedId = null })
                    Text("Antar Sendiri oleh Penjual", style = MaterialTheme.typography.titleSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(couriers.firstOrNull { it.id == selectedId }) }) {
                Text("Mulai Pengiriman")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun SellerProductsScreen(
    store: StoreEntity?,
    products: List<ProductEntity>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (Long) -> Unit,
) {
    if (store == null) return
    val storeProducts = remember(products, store.id) { products.filter { it.storeId == store.id } }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_product_fab"),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah produk")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AppBrandTopBar(
                title = "Produk & Stok",
                subtitle = "${storeProducts.size} produk • ${storeProducts.count { it.isLowStock() }} stok menipis",
                unreadNotifications = unreadNotifications,
                onNotificationClick = onOpenNotifications,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(storeProducts, key = { it.id }) { prod ->
                    ResponsiveCenteredColumn {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                GallonProductGraphic(
                                    category = prod.category,
                                    tintColor = Color(prod.brandColorHex),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall)
                                    Text(prod.category.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(formatRupiah(prod.price), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Stok: ${prod.stock} (min ${prod.minStockThreshold})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (prod.isLowStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (prod.isLowStock()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(50)) {
                                                Text(
                                                    text = "Stok Menipis",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Tersedia", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = prod.isAvailable && prod.stock > 0,
                                            onCheckedChange = { checked ->
                                                onSaveProduct(prod.copy(isAvailable = checked))
                                            },
                                        )
                                    }
                                }
                                Column {
                                    IconButton(onClick = {
                                        editingProduct = prod
                                        showEditor = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit produk")
                                    }
                                    IconButton(onClick = { onDeleteProduct(prod.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus produk", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            storeId = store.id,
            existing = editingProduct,
            onDismiss = { showEditor = false },
            onSave = { saved ->
                onSaveProduct(saved)
                showEditor = false
            },
        )
    }
}

@Composable
private fun ProductEditorDialog(
    storeId: Long,
    existing: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var priceText by remember { mutableStateOf(existing?.price?.toString() ?: "6000") }
    var stockText by remember { mutableStateOf(existing?.stock?.toString() ?: "20") }
    var minStockText by remember { mutableStateOf(existing?.minStockThreshold?.toString() ?: "5") }
    var category by remember { mutableStateOf(existing?.category ?: ProductCategory.REFILL_DEPOT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Produk Baru" else "Edit Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Kategori", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortLabel) },
                        )
                    }
                }
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit) },
                    label = { Text("Harga (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter(Char::isDigit) },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it.filter(Char::isDigit) },
                        label = { Text("Batas Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi singkat") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toIntOrNull() ?: 0
                    val s = stockText.toIntOrNull() ?: 0
                    val m = minStockText.toIntOrNull() ?: 5
                    if (name.isNotBlank() && p > 0) {
                        onSave(
                            (existing ?: ProductEntity(
                                storeId = storeId,
                                name = name.trim(),
                                category = category,
                                description = desc.trim(),
                                price = p,
                                stock = s,
                                minStockThreshold = m,
                            )).copy(
                                name = name.trim(),
                                category = category,
                                description = desc.trim(),
                                price = p,
                                stock = s,
                                minStockThreshold = m,
                                isAvailable = s > 0,
                            ),
                        )
                    }
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun SellerReportsScreen(
    store: StoreEntity?,
    orders: List<OrderWithItems>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
) {
    if (store == null) return
    var period by remember { mutableStateOf(SalesPeriod.ALL) }

    val now = remember { System.currentTimeMillis() }
    val cutoff = when (period) {
        SalesPeriod.TODAY -> now - 24 * 3_600_000L
        SalesPeriod.WEEK -> now - 7 * 24 * 3_600_000L
        SalesPeriod.MONTH -> now - 30 * 24 * 3_600_000L
        SalesPeriod.ALL -> 0L
    }

    val periodOrders = remember(orders, store.id, cutoff) {
        orders.filter { it.order.storeId == store.id && it.order.createdAt >= cutoff }
    }
    val completed = periodOrders.filter { it.order.status == OrderStatus.COMPLETED }
    val cancelled = periodOrders.filter { it.order.status == OrderStatus.REJECTED_CANCELLED }
    val totalRevenue = completed.sumOf { it.order.totalAmount }
    val totalGallons = completed.flatMap { it.items }.sumOf { it.quantity }

    val topProducts = remember(completed) {
        completed.flatMap { it.items }
            .groupBy { it.productName }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
            .entries
            .sortedByDescending { it.value }
    }
    val maxSold = (topProducts.firstOrNull()?.value ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Laporan Penjualan",
            subtitle = store.name,
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SalesPeriod.entries.forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { period = p },
                                label = { Text(p.label) },
                            )
                        }
                    }
                }
            }

            item {
                ResponsiveCenteredColumn {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DashboardMetricCard(
                            title = "Total Omzet",
                            value = formatRupiah(totalRevenue),
                            bgColor = MaterialTheme.colorScheme.primaryContainer,
                            fgColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        )
                        DashboardMetricCard(
                            title = "Transaksi Selesai",
                            value = "${completed.size} ($totalGallons unit)",
                            bgColor = StatusDoneBg,
                            fgColor = StatusDoneFg,
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                ResponsiveCenteredColumn {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Produk Terlaris", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(10.dp))
                            if (topProducts.isEmpty()) {
                                Text("Belum ada transaksi selesai pada periode ini.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                topProducts.forEach { (prodName, qty) ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(prodName, style = MaterialTheme.typography.bodyMedium)
                                            Text("$qty terjual", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { qty.toFloat() / maxSold.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                ResponsiveCenteredColumn {
                    Text(
                        text = "Riwayat Transaksi (${completed.size + cancelled.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            val history = (completed + cancelled).sortedByDescending { it.order.updatedAt }
            items(history, key = { it.order.id }) { item ->
                val order = item.order
                ResponsiveCenteredColumn {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${order.orderCode} • ${order.buyerName}", style = MaterialTheme.typography.titleSmall)
                                Text(formatTimestamp(order.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${order.paymentMethod.label} • ${order.paymentStatus}", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatRupiah(order.totalAmount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OrderStatusBadge(order.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerStoreSettingsScreen(
    store: StoreEntity?,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onSaveStore: (StoreEntity) -> Unit,
    onSubmitComplaint: (String, String) -> Unit,
    onLogout: () -> Unit,
) {
    if (store == null) return
    var name by remember(store) { mutableStateOf(store.name) }
    var address by remember(store) { mutableStateOf(store.address) }
    var openHours by remember(store) { mutableStateOf(store.openHours) }
    var deliveryFee by remember(store) { mutableStateOf(store.deliveryFee.toString()) }
    var radius by remember(store) { mutableStateOf(store.maxServiceRadiusKm.toString()) }
    var maxCapacity by remember(store) { mutableStateOf(store.maxActiveOrders.toString()) }
    var isOpen by remember(store) { mutableStateOf(store.isOpen) }
    var isPaused by remember(store) { mutableStateOf(store.isPaused) }
    var desc by remember(store) { mutableStateOf(store.description) }
    var showComplaint by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Pengaturan Toko",
            subtitle = if (store.isVerified) "Terverifikasi" else "Menunggu Verifikasi Admin",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResponsiveCenteredColumn {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status Toko Buka", style = MaterialTheme.typography.titleSmall)
                                Text("Nonaktifkan saat toko tutup di luar jam operasional", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isOpen, onCheckedChange = { isOpen = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Jeda Pesanan (Kapasitas Penuh)", style = MaterialTheme.typography.titleSmall)
                                Text("Tunda pesanan baru sementara waktu saat antrean padat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isPaused, onCheckedChange = { isPaused = it })
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Toko / Depot") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Toko") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = openHours,
                            onValueChange = { openHours = it },
                            label = { Text("Jam Operasional") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = deliveryFee,
                                onValueChange = { deliveryFee = it.filter(Char::isDigit) },
                                label = { Text("Biaya Kirim (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = radius,
                                onValueChange = { radius = it },
                                label = { Text("Jangkauan (km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        OutlinedTextField(
                            value = maxCapacity,
                            onValueChange = { maxCapacity = it.filter(Char::isDigit) },
                            label = { Text("Batas Maksimal Pesanan Aktif") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Deskripsi Usaha") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Button(
                            onClick = {
                                onSaveStore(
                                    store.copy(
                                        name = name.trim().ifBlank { store.name },
                                        address = address.trim().ifBlank { store.address },
                                        openHours = openHours.trim().ifBlank { store.openHours },
                                        deliveryFee = deliveryFee.toIntOrNull() ?: store.deliveryFee,
                                        maxServiceRadiusKm = radius.toDoubleOrNull() ?: store.maxServiceRadiusKm,
                                        maxActiveOrders = maxCapacity.toIntOrNull() ?: store.maxActiveOrders,
                                        isOpen = isOpen,
                                        isPaused = isPaused,
                                        description = desc.trim(),
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Simpan Pengaturan Toko") }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showComplaint = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Laporkan Masalah ke Admin")
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

    if (showComplaint) {
        ComplaintSubmitDialog(
            onDismiss = { showComplaint = false },
            onSubmit = { t, d ->
                onSubmitComplaint(t, d)
                showComplaint = false
            },
        )
    }
}
