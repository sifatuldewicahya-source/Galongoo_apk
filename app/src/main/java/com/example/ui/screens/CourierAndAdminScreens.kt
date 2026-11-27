package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AnnouncementEntity
import com.example.data.local.ComplaintEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.StoreEntity
import com.example.data.local.UserEntity
import com.example.data.model.ComplaintStatus
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.example.data.model.formatRupiah
import com.example.data.model.formatTimestamp
import com.example.ui.components.AppBrandTopBar
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GalonLogoImage
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ResponsiveCenteredColumn
import com.example.ui.theme.StatusDoneBg
import com.example.ui.theme.StatusDoneFg
import com.example.ui.theme.StatusWaitingBg
import com.example.ui.theme.StatusWaitingFg

// ==================== COURIER SCREENS ====================

@Composable
fun CourierTasksScreen(
    user: UserEntity,
    orders: List<OrderWithItems>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onUpdateOrderStatus: (OrderEntity, OrderStatus, UserEntity?, String?) -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tugas Aktif, 1 = Riwayat, 2 = Profil
    var completeTarget by remember { mutableStateOf<OrderEntity?>(null) }

    val assignedOrders = remember(orders, user.id) {
        orders.filter {
            it.order.courierId == user.id ||
                (it.order.courierId == null && it.order.status == OrderStatus.PREPARING)
        }
    }
    val activeTasks = assignedOrders.filter {
        it.order.status == OrderStatus.PREPARING || it.order.status == OrderStatus.DELIVERING
    }
    val completedTasks = assignedOrders.filter {
        it.order.status == OrderStatus.COMPLETED && it.order.courierId == user.id
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Kurir GalonDekat",
            subtitle = "${user.name} • ${activeTasks.size} tugas aktif",
            unreadNotifications = unreadNotifications,
            onNotificationClick = onOpenNotifications,
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Tugas Aktif (${activeTasks.size})") },
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Selesai (${completedTasks.size})") },
            )
            FilterChip(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                label = { Text("Profil") },
            )
        }

        if (selectedTab == 2) {
            ResponsiveCenteredColumn(modifier = Modifier.padding(16.dp)) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GalonLogoImage(size = 54.dp, cornerRadius = 14.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(user.name, style = MaterialTheme.typography.titleMedium)
                                Text("${user.role.label} • ${user.phoneOrEmail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Total pengantaran selesai: ${completedTasks.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
            return
        }

        val displayed = if (selectedTab == 0) activeTasks else completedTasks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (displayed.isEmpty()) {
                item {
                    ResponsiveCenteredColumn {
                        EmptyStateCard(
                            title = if (selectedTab == 0) "Belum ada tugas pengiriman" else "Belum ada riwayat pengantaran",
                            subtitle = "Tugas pengiriman yang ditugaskan kepada Anda akan muncul di sini.",
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
                                ) {
                                    Column {
                                        Text("${order.orderCode} • ${order.storeName}", style = MaterialTheme.typography.titleMedium)
                                        Text(formatTimestamp(order.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OrderStatusBadge(order.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pembeli: ${order.buyerName}", style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(order.buyerPhone, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("Alamat: ${order.address}", style = MaterialTheme.typography.bodyMedium)
                                if (order.notes.isNotBlank()) {
                                    Text("Catatan: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                item.items.forEach { line ->
                                    Text("• ${line.quantity}x ${line.productName}", style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Tagihan ${order.paymentMethod.label}", style = MaterialTheme.typography.bodySmall)
                                    Text(formatRupiah(order.totalAmount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (!order.deliveryProofNote.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Bukti: ${order.deliveryProofNote}", style = MaterialTheme.typography.bodySmall, color = StatusDoneFg)
                                }

                                if (selectedTab == 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(order.address)}")
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                runCatching { context.startActivity(intent) }
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Petunjuk Arah")
                                        }
                                        if (order.status == OrderStatus.PREPARING) {
                                            Button(
                                                onClick = {
                                                    onUpdateOrderStatus(order, OrderStatus.DELIVERING, user, null)
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ambil & Antar")
                                            }
                                        } else if (order.status == OrderStatus.DELIVERING) {
                                            Button(
                                                onClick = { completeTarget = order },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Konfirmasi Tiba")
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
    }

    completeTarget?.let { target ->
        DeliveryConfirmationDialog(
            orderCode = target.orderCode,
            buyerName = target.buyerName,
            onDismiss = { completeTarget = null },
            onConfirm = { proof ->
                onUpdateOrderStatus(target, OrderStatus.COMPLETED, user, proof)
                completeTarget = null
            },
        )
    }
}

@Composable
private fun DeliveryConfirmationDialog(
    orderCode: String,
    buyerName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var proofNote by remember { mutableStateOf("Diterima langsung oleh $buyerName.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfirmasi Serah Terima $orderCode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Catat bukti serah terima atau nama penerima barang:", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = { Text("Catatan serah terima") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(proofNote.ifBlank { "Pesanan telah diterima pembeli." }) }) {
                Text("Selesaikan Pengantaran")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ==================== ADMIN SCREENS ====================

@Composable
fun AdminDashboardScreen(
    user: UserEntity,
    users: List<UserEntity>,
    stores: List<StoreEntity>,
    orders: List<OrderWithItems>,
    complaints: List<ComplaintEntity>,
    announcements: List<AnnouncementEntity>,
    onUpdateStore: (StoreEntity) -> Unit,
    onToggleUserActive: (UserEntity) -> Unit,
    onUpdateComplaint: (ComplaintEntity, ComplaintStatus, String) -> Unit,
    onAddAnnouncement: (String, String, String) -> Unit,
    onToggleAnnouncement: (AnnouncementEntity) -> Unit,
    onDeleteAnnouncement: (Long) -> Unit,
    onLogout: () -> Unit,
) {
    val tabs = listOf("Toko", "Akun", "Transaksi", "Pengaduan", "Konten")
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddBanner by remember { mutableStateOf(false) }

    val totalCompletedRevenue = orders.filter { it.order.status == OrderStatus.COMPLETED }.sumOf { it.order.totalAmount }

    Column(modifier = Modifier.fillMaxSize()) {
        AppBrandTopBar(
            title = "Panel Admin GalonDekat",
            subtitle = "${user.name} • ${stores.count { !it.isVerified }} toko tunggu verifikasi",
            trailingAction = {
                IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_button")) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Keluar")
                }
            },
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
            when (selectedTab) {
                0 -> {
                    // Verifikasi Toko
                    items(stores, key = { it.id }) { store ->
                        ResponsiveCenteredColumn {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(store.name, style = MaterialTheme.typography.titleMedium)
                                            if (store.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = "Terverifikasi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Surface(
                                            color = if (store.isVerified) StatusDoneBg else StatusWaitingBg,
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                text = if (store.isVerified) "Terverifikasi" else "Menunggu",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (store.isVerified) StatusDoneFg else StatusWaitingFg,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(store.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Jam ${store.openHours} • Radius ${store.maxServiceRadiusKm} km", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onUpdateStore(store.copy(isVerified = !store.isVerified)) },
                                        colors = if (store.isVerified) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors(),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (store.isVerified) "Cabut Verifikasi Sementara" else "Setujui & Verifikasi Toko")
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Manajemen Akun & Kategori
                    item {
                        ResponsiveCenteredColumn {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Kategori Produk Terdaftar", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        ProductCategory.entries.joinToString(" • ") { it.label },
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    items(users, key = { it.id }) { account ->
                        ResponsiveCenteredColumn {
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${account.name} (${account.role.label})", style = MaterialTheme.typography.titleSmall)
                                        Text(account.phoneOrEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = account.isActive,
                                        enabled = account.id != user.id,
                                        onCheckedChange = { onToggleUserActive(account) },
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Pemantauan Transaksi
                    item {
                        ResponsiveCenteredColumn {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Ringkasan Transaksi Platform", style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatRupiah(totalCompletedRevenue), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text("${orders.size} total pesanan tercatat di sistem", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    items(orders, key = { it.order.id }) { item ->
                        val o = item.order
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
                                        Text("${o.orderCode} • ${o.storeName}", style = MaterialTheme.typography.titleSmall)
                                        Text("Pembeli: ${o.buyerName} • ${o.paymentMethod.label}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatRupiah(o.totalAmount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        OrderStatusBadge(o.status)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Pengaduan
                    if (complaints.isEmpty()) {
                        item {
                            ResponsiveCenteredColumn {
                                EmptyStateCard("Belum ada pengaduan", "Laporan kendala dari pembeli atau penjual akan muncul di sini.")
                            }
                        }
                    } else {
                        items(complaints, key = { it.id }) { comp ->
                            ResponsiveCenteredColumn {
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(comp.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            Surface(
                                                color = if (comp.status == ComplaintStatus.SELESAI) StatusDoneBg else StatusWaitingBg,
                                                shape = RoundedCornerShape(50),
                                            ) {
                                                Text(
                                                    comp.status.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                        Text("${comp.reporterName} (${comp.reporterRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(comp.description, style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (comp.status != ComplaintStatus.SELESAI) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (comp.status == ComplaintStatus.BARU) {
                                                    OutlinedButton(
                                                        onClick = { onUpdateComplaint(comp, ComplaintStatus.DITINDAKLANJUTI, "Sedang ditinjau tim Admin") },
                                                        modifier = Modifier.weight(1f),
                                                    ) { Text("Tindak Lanjuti") }
                                                }
                                                Button(
                                                    onClick = { onUpdateComplaint(comp, ComplaintStatus.SELESAI, "Diselesaikan oleh Admin") },
                                                    modifier = Modifier.weight(1f),
                                                ) { Text("Tandai Selesai") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Pengelolaan Konten / Banner
                    item {
                        ResponsiveCenteredColumn {
                            Button(
                                onClick = { showAddBanner = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Banner Promo / Pengumuman")
                            }
                        }
                    }
                    items(announcements, key = { it.id }) { ann ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("[${ann.badgeText}] ${ann.title}", style = MaterialTheme.typography.titleSmall)
                                        Text(ann.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = ann.isActive,
                                        onCheckedChange = { onToggleAnnouncement(ann) },
                                    )
                                    IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus banner", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBanner) {
        AddAnnouncementDialog(
            onDismiss = { showAddBanner = false },
            onSave = { title, sub, badge ->
                onAddAnnouncement(title, sub, badge)
                showAddBanner = false
            },
        )
    }
}

@Composable
private fun AddAnnouncementDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var badge by remember { mutableStateOf("PROMO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Banner Promo / Pengumuman") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("Label Badge") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, subtitle, badge) }) { Text("Terbitkan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
