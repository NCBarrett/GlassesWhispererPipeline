package glasseswhisperer.pipeline

import kotlinx.serialization.Serializable

/**
 * Represents a simplified venue entity.
 * Populates your drop-down menus directly from your persistent t_venues database table.
 */
data class VenueLookup(
    val id: Int,
    val name: String
)

/**
 * A flattened data model optimized for your main pipeline table rows.
 * Binds your relational SQL inner joins straight into your visible dashboard.
 */
data class PipelineItem(
    val instanceId: Int,
    val eventName: String,
    val venueName: String,
    val calendarDate: String,
    val appDeadline: String,
    val feeAmount: Double,
    val anticipatedRevenue: Double,
    val status: String
)

/**
 * An aggregate data class specifically optimized to feed your monthly timeline graph.
 * Summarizes seasonal volume trends and cash footprints natively inside SQLite.
 */
data class MonthlyEventCount(
    val monthString: String, // Format: YYYY-MM
    val totalEvents: Int,
    val totalFees: Double
)

/**
 * The data transfer object used by your network engine.
 * Maps incoming JSON data streams from large enterprise venue APIs safely.
 */
@Serializable
data class OccEventRecord(
    val title: String,
    val start_date: String,
    val end_date: String
)