package com.example.data.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UserRole(val label: String, val subtitle: String) {
    BUYER("Pembeli", "Cari toko galon & pesan cepat"),
    SELLER("Penjual", "Kelola toko, stok & antrean pesanan"),
    COURIER("Kurir", "Antar pesanan & perbarui pengantaran"),
    ADMIN("Admin", "Verifikasi toko, akun & pengaduan"),
}

enum class ProductCategory(val label: String, val shortLabel: String) {
    BRANDED_GALLON("Galon Bermerek", "Bermerek"),
    REFILL_DEPOT("Air Isi Ulang Depot", "Isi Ulang"),
    OWN_GALLON_REFILL("Isi Ulang Galon Sendiri", "Galon Sendiri"),
    NEW_GALLON_ACCESSORY("Galon Baru & Aksesoris", "Galon Baru"),
}

enum class OrderStatus(val label: String) {
    PENDING_CONFIRMATION("Menunggu Konfirmasi"),
    CONFIRMED("Dikonfirmasi"),
    PREPARING("Disiapkan"),
    DELIVERING("Dalam Pengiriman"),
    COMPLETED("Selesai"),
    REJECTED_CANCELLED("Ditolak / Dibatalkan"),
}

enum class PaymentMethod(val label: String, val description: String) {
    COD("COD / Tunai", "Bayar tunai saat galon diterima"),
    BANK_TRANSFER("Transfer Bank", "Konfirmasi transfer melalui kurir/penjual"),
    QRIS("QRIS", "Pindai QRIS saat serah terima"),
}

enum class ComplaintStatus(val label: String) {
    BARU("Baru"),
    DITINDAKLANJUTI("Ditindaklanjuti"),
    SELESAI("Selesai"),
}

enum class SalesPeriod(val label: String) {
    TODAY("Hari Ini"),
    WEEK("7 Hari"),
    MONTH("30 Hari"),
    ALL("Semua"),
}

data class CartItem(
    val productId: Long,
    val storeId: Long,
    val storeName: String,
    val productName: String,
    val categoryLabel: String,
    val unitPrice: Int,
    val quantity: Int,
    val maxStock: Int,
    val brandColorHex: Long,
) {
    val subtotal: Int get() = unitPrice * quantity
}

fun formatRupiah(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp${formatter.format(amount)}"
}

fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(millis))
}

fun formatShortTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("id", "ID"))
    return sdf.format(Date(millis))
}
