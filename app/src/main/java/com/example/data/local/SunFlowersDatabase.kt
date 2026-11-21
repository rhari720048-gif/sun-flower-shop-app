package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameTa: String,
    val imageKey: String,
    val sortOrder: Int,
    val isActive: Boolean
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val nameEn: String,
    val nameTa: String,
    val descriptionEn: String,
    val descriptionTa: String,
    val pricePaise: Int,
    val mrpPaise: Int?,
    val imageKey: String,
    val unitLabel: String,
    val inStock: Boolean,
    val isFeatured: Boolean,
    val isActive: Boolean
)

@Entity(tableName = "wishlist_items")
data class WishlistEntity(
    @PrimaryKey val productId: String,
    val userId: String,
    val createdAt: Long
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val label: String,
    val recipientName: String,
    val recipientPhone: String,
    val line1: String,
    val line2: String,
    val landmark: String,
    val pincode: String,
    val lat: Double,
    val lng: Double,
    val distanceKm: Double,
    val isDefault: Boolean
)

@Entity(tableName = "delivery_partners")
data class DeliveryPartnerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val rating: Double,
    val isActive: Boolean
)

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isOpen: Boolean,
    val closedMessage: String,
    val reopenAtEpochMillis: Long?,
    val shopName: String,
    val shopAddress: String,
    val shopLat: Double,
    val shopLng: Double,
    val deliveryRadiusKm: Double,
    val deliveryChargePaise: Int,
    val freeDeliveryAbovePaise: Int?,
    val minOrderPaise: Int,
    val taxPercent: Double,
    val estimatedDeliveryMinutes: Int,
    val supportPhone: String,
    val supportWhatsapp: String,
    val codEnabled: Boolean,
    val onlinePaymentEnabled: Boolean,
    val updatedByName: String,
    val updatedAtEpochMillis: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: Int,
    val userId: String,
    val idempotencyKey: String,
    val customerName: String,
    val customerPhone: String,
    val status: String,
    val addressSummary: String,
    val addressLat: Double,
    val addressLng: Double,
    val distanceKm: Double,
    val subtotalPaise: Int,
    val deliveryChargePaise: Int,
    val taxPaise: Int,
    val totalPaise: Int,
    val paymentMethod: String,
    val paymentStatus: String,
    val deliveryPartnerId: String?,
    val customerNote: String?,
    val cancelReason: String?,
    val cancelledBy: String?,
    val estimatedMinutes: Int,
    val placedAtEpochMillis: Long,
    val outForDeliveryAtEpochMillis: Long?,
    val deliveredAtEpochMillis: Long?
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val nameSnapshot: String,
    val unitLabelSnapshot: String,
    val imageKey: String,
    val unitPricePaise: Int,
    val quantity: Int,
    val lineTotalPaise: Int
)

@Entity(tableName = "order_status_history")
data class OrderStatusHistoryEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val status: String,
    val changedBy: String,
    val note: String?,
    val timestampEpochMillis: Long
)

@Entity(tableName = "admin_audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val adminName: String,
    val action: String,
    val entity: String,
    val summary: String,
    val timestampEpochMillis: Long
)

@Dao
interface SunFlowersDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(items: List<CategoryEntity>)

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY isFeatured DESC, nameEn ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY isFeatured DESC, nameEn ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(item: ProductEntity)

    @Query("SELECT productId FROM wishlist_items WHERE userId = :userId")
    fun observeWishlistIds(userId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND productId = :productId")
    suspend fun removeWishlist(userId: String, productId: String)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId")
    suspend fun clearWishlistForUser(userId: String)

    @Query("SELECT * FROM addresses WHERE userId = :userId ORDER BY isDefault DESC, id DESC")
    fun observeAddresses(userId: String): Flow<List<AddressEntity>>

    @Query("SELECT COUNT(*) FROM addresses WHERE userId = :userId")
    suspend fun countAddresses(userId: String): Int

    @Query("UPDATE addresses SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaultAddresses(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddress(address: AddressEntity)

    @Query("DELETE FROM addresses WHERE id = :id AND userId = :userId")
    suspend fun deleteAddress(id: String, userId: String)

    @Query("DELETE FROM addresses WHERE userId = :userId")
    suspend fun deleteAllAddressesForUser(userId: String)

    @Query("SELECT * FROM delivery_partners WHERE isActive = 1 ORDER BY name ASC")
    fun observeDeliveryPartners(): Flow<List<DeliveryPartnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeliveryPartners(items: List<DeliveryPartnerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeliveryPartner(item: DeliveryPartnerEntity)

    @Query("SELECT * FROM shop_settings WHERE id = 1")
    fun observeShopSettings(): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE id = 1")
    suspend fun getShopSettingsOnce(): ShopSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShopSettings(settings: ShopSettingsEntity)

    @Query("SELECT * FROM orders ORDER BY placedAtEpochMillis DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId AND idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun findOrderByIdempotencyKey(userId: String, idempotencyKey: String): OrderEntity?

    @Query("SELECT COALESCE(MAX(orderNumber), 100) FROM orders")
    suspend fun getMaxOrderNumber(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Query("SELECT * FROM order_items")
    fun observeAllOrderItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_status_history ORDER BY timestampEpochMillis ASC")
    fun observeAllStatusHistory(): Flow<List<OrderStatusHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusHistory(entry: OrderStatusHistoryEntity)

    @Query("UPDATE orders SET customerName = 'Deleted User', customerPhone = '', addressSummary = 'Redacted Area' WHERE userId = :userId")
    suspend fun anonymizeOrdersForUser(userId: String)

    @Query("SELECT * FROM admin_audit_log ORDER BY timestampEpochMillis DESC LIMIT 100")
    fun observeAuditLog(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(entry: AuditLogEntity)
}

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        WishlistEntity::class,
        AddressEntity::class,
        DeliveryPartnerEntity::class,
        ShopSettingsEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderStatusHistoryEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SunFlowersDatabase : RoomDatabase() {
    abstract fun dao(): SunFlowersDao

    companion object {
        @Volatile
        private var INSTANCE: SunFlowersDatabase? = null

        fun getInstance(context: Context): SunFlowersDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SunFlowersDatabase::class.java,
                    "sun_flowers_cache.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
