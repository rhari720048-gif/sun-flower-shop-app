package com.example.data.neon

import com.example.BuildConfig
import com.example.domain.NeonConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

data class NeonQueryResult(
    val rows: List<Map<String, Any?>>,
    val rowCount: Int
)

/**
 * Parameterized HTTPS SQL client for Neon Serverless PostgreSQL (/sql endpoint).
 * Reads connection string strictly from BuildConfig.NEON_DATABASE_URL (injected via AI Studio Secrets).
 * Never logs credentials or connection strings.
 */
class NeonSqlClient(
    private val rawConnectionString: String = BuildConfig.NEON_DATABASE_URL
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val parsedHost: String? by lazy { extractNeonHost(rawConnectionString) }

    val isConfigured: Boolean
        get() = parsedHost != null &&
            rawConnectionString.isNotBlank() &&
            !rawConnectionString.contains("PLACEHOLDER", ignoreCase = true)

    fun maskedHost(): String = parsedHost ?: "Not configured"

    /**
     * Executes a parameterized PostgreSQL statement over HTTPS against Neon's SQL endpoint.
     * All untrusted values MUST be passed in [params] (never interpolated into [sql]).
     */
    suspend fun execute(sql: String, params: List<Any?> = emptyList()): Result<NeonQueryResult> =
        withContext(Dispatchers.IO) {
            val host = parsedHost
            if (!isConfigured || host == null) {
                return@withContext Result.failure(
                    IllegalStateException("NEON_DATABASE_URL is not configured in Secrets.")
                )
            }

            try {
                val payload = JSONObject().apply {
                    put("query", sql)
                    put("params", JSONArray().apply {
                        params.forEach { value ->
                            when (value) {
                                null -> put(JSONObject.NULL)
                                else -> put(value)
                            }
                        }
                    })
                }

                val request = Request.Builder()
                    .url("https://$host/sql")
                    .addHeader("Neon-Connection-String", rawConnectionString)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Neon HTTP ${response.code}")
                        )
                    }
                    val parsed = parseNeonResponse(bodyText)
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun pingAndEnsureSchema(): NeonConnectionStatus {
        if (!isConfigured) {
            return NeonConnectionStatus(
                isConfigured = false,
                isConnected = false,
                hostMasked = "Not configured",
                lastSyncMessage = "Add NEON_DATABASE_URL in AI Studio Secrets"
            )
        }

        val ping = execute("SELECT 1 AS ok")
        if (ping.isFailure) {
            return NeonConnectionStatus(
                isConfigured = true,
                isConnected = false,
                hostMasked = maskedHost(),
                lastSyncMessage = "Offline fallback active (Neon unreachable)"
            )
        }

        // Apply idempotent core schema statements matching 02-database-schema.md
        for (ddl in NeonSchemaStatements.ddlStatements) {
            execute(ddl)
        }

        return NeonConnectionStatus(
            isConfigured = true,
            isConnected = true,
            hostMasked = maskedHost(),
            lastSyncMessage = "Synced with Neon PostgreSQL"
        )
    }

    private fun parseNeonResponse(bodyText: String): NeonQueryResult {
        if (bodyText.isBlank()) return NeonQueryResult(emptyList(), 0)
        val root = JSONObject(bodyText)
        val rowsArray = root.optJSONArray("rows") ?: JSONArray()
        val parsedRows = buildList {
            for (i in 0 until rowsArray.length()) {
                val obj = rowsArray.optJSONObject(i) ?: continue
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val v = obj.opt(key)
                    map[key] = if (v == JSONObject.NULL) null else v
                }
                add(map)
            }
        }
        val rowCount = root.optInt("rowCount", parsedRows.size)
        return NeonQueryResult(rows = parsedRows, rowCount = rowCount)
    }

    companion object {
        fun extractNeonHost(connectionString: String): String? {
            if (connectionString.isBlank() || connectionString.contains("PLACEHOLDER", ignoreCase = true)) {
                return null
            }
            return try {
                val normalized = connectionString.trim()
                if (!normalized.startsWith("postgres://") && !normalized.startsWith("postgresql://")) {
                    return null
                }
                val uri = URI(normalized)
                val host = uri.host?.lowercase() ?: return null
                // Strict host allowlist: only permit valid Neon HTTPS hosts (blocks SSRF/private IPs)
                if (host.endsWith(".neon.tech")) host else null
            } catch (_: Exception) {
                null
            }
        }
    }
}

object NeonSchemaStatements {
    val ddlStatements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS users (
            id text PRIMARY KEY,
            firebase_uid text NOT NULL UNIQUE,
            name text,
            phone text,
            email text,
            role text NOT NULL DEFAULT 'customer',
            language text NOT NULL DEFAULT 'en',
            is_deleted boolean NOT NULL DEFAULT false,
            deleted_at timestamptz,
            created_at timestamptz NOT NULL DEFAULT now(),
            updated_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS shop_settings (
            id smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
            is_open boolean NOT NULL DEFAULT true,
            closed_message text,
            reopen_at timestamptz,
            shop_name text NOT NULL DEFAULT 'Sun Flowers and Decoration',
            shop_lat double precision NOT NULL DEFAULT 11.0081,
            shop_lng double precision NOT NULL DEFAULT 76.9510,
            delivery_radius_km numeric(4,1) NOT NULL DEFAULT 6.0,
            delivery_charge_paise int NOT NULL DEFAULT 4000,
            free_delivery_above_paise int DEFAULT 99900,
            min_order_paise int NOT NULL DEFAULT 19900,
            tax_percent numeric(4,2) NOT NULL DEFAULT 0,
            estimated_delivery_minutes int NOT NULL DEFAULT 25,
            support_phone text,
            support_whatsapp text,
            cod_enabled boolean NOT NULL DEFAULT true,
            online_payment_enabled boolean NOT NULL DEFAULT true,
            updated_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS categories (
            id text PRIMARY KEY,
            name_en text NOT NULL,
            name_ta text NOT NULL,
            image_key text,
            sort_order int NOT NULL DEFAULT 0,
            is_active boolean NOT NULL DEFAULT true,
            created_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS products (
            id text PRIMARY KEY,
            category_id text NOT NULL REFERENCES categories(id),
            name_en text NOT NULL,
            name_ta text NOT NULL,
            description_en text,
            description_ta text,
            price_paise int NOT NULL CHECK (price_paise >= 0),
            mrp_paise int,
            image_key text,
            unit_label text,
            in_stock boolean NOT NULL DEFAULT true,
            is_featured boolean NOT NULL DEFAULT false,
            is_active boolean NOT NULL DEFAULT true,
            created_at timestamptz NOT NULL DEFAULT now(),
            updated_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS addresses (
            id text PRIMARY KEY,
            user_id text NOT NULL,
            label text NOT NULL,
            recipient_name text NOT NULL,
            recipient_phone text NOT NULL,
            line1 text NOT NULL,
            line2 text,
            landmark text,
            pincode text NOT NULL,
            lat double precision NOT NULL,
            lng double precision NOT NULL,
            distance_km numeric(5,2) NOT NULL,
            is_default boolean NOT NULL DEFAULT false,
            created_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS delivery_partners (
            id text PRIMARY KEY,
            name text NOT NULL,
            phone text NOT NULL,
            rating numeric(3,1) NOT NULL DEFAULT 4.8,
            is_active boolean NOT NULL DEFAULT true,
            created_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS orders (
            id text PRIMARY KEY,
            order_number int NOT NULL UNIQUE,
            user_id text NOT NULL,
            idempotency_key text NOT NULL,
            customer_name text NOT NULL,
            customer_phone text NOT NULL,
            status text NOT NULL DEFAULT 'placed',
            address_snapshot text NOT NULL,
            address_lat double precision NOT NULL,
            address_lng double precision NOT NULL,
            distance_km numeric(5,2) NOT NULL,
            subtotal_paise int NOT NULL,
            delivery_charge_paise int NOT NULL,
            tax_paise int NOT NULL,
            total_paise int NOT NULL,
            payment_method text NOT NULL,
            payment_status text NOT NULL DEFAULT 'pending',
            delivery_partner_id text,
            customer_note text,
            cancel_reason text,
            cancelled_by text,
            placed_at timestamptz NOT NULL DEFAULT now(),
            updated_at timestamptz NOT NULL DEFAULT now(),
            UNIQUE (user_id, idempotency_key)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS admin_audit_log (
            id text PRIMARY KEY,
            admin_name text NOT NULL,
            action text NOT NULL,
            entity text NOT NULL,
            summary text NOT NULL,
            created_at timestamptz NOT NULL DEFAULT now()
        )
        """.trimIndent()
    )
}
