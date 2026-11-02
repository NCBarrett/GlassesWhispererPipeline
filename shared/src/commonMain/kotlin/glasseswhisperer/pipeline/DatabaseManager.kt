package glasseswhisperer.pipeline

import java.sql.Connection
import java.sql.DriverManager

class EventDatabaseManager(private val dbPath: String = "jdbc:sqlite:whisperer_pipeline.db") {

    init {
        Class.forName("org.sqlite.JDBC")
        initializeDatabase()
    }

    fun getConnection(): Connection = DriverManager.getConnection(dbPath)

    private fun initializeDatabase() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA foreign_keys = ON;")

                // 1. Venues Table: Structural parameters & setting types
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS t_venues (
                        venue_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        venue_name TEXT NOT NULL UNIQUE,
                        street_address TEXT,
                        setting_type TEXT CHECK(setting_type IN ('Indoor', 'Outdoor', 'Hybrid')),
                        parking_fee_applies INTEGER CHECK(parking_fee_applies IN (0, 1)) DEFAULT 0
                    );
                """.trimIndent())

                // 2. Events Master Table: Expanded with seasonal windows, day rules, and saved application verbiage
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS t_events (
                        event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        venue_id INTEGER,
                        event_name TEXT NOT NULL UNIQUE,
                        organizer_email TEXT,
                        website_url TEXT,
                        event_frequency TEXT CHECK(event_frequency IN ('Annual', 'Monthly', 'One-Time')) DEFAULT 'One-Time',
                        accepts_vouchers INTEGER CHECK(accepts_vouchers IN (0, 1)) DEFAULT 0,
                        season_start TEXT,
                        season_end TEXT,
                        default_day_of_week TEXT,
                        daily_start_time TEXT,
                        daily_end_time TEXT,
                        saved_pitch_text TEXT,
                        FOREIGN KEY (venue_id) REFERENCES t_venues(venue_id) ON DELETE SET NULL
                    );
                """.trimIndent())

                // 3. Operational Instances Table: Tracking transaction metrics for specific calendar loops
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS t_event_instances (
                        instance_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_id INTEGER,
                        calendar_date TEXT NOT NULL,
                        app_deadline TEXT,
                        booth_size_text TEXT,
                        fee_amount REAL NOT NULL DEFAULT 0.0,
                        target_layout TEXT DEFAULT 'Dual 24x16 Side-by-Side',
                        allocated_layout TEXT,
                        anticipated_revenue REAL NOT NULL DEFAULT 0.0,
                        status_enum TEXT CHECK(status_enum IN ('Pipeline', 'Applied', 'Waitlisted', 'Accepted', 'Completed')) DEFAULT 'Pipeline',
                        FOREIGN KEY (event_id) REFERENCES t_events(event_id) ON DELETE CASCADE
                    );
                """.trimIndent())
            }
        }
    }

    fun seedInitialPipeline() {
        val insertVenue = "INSERT OR IGNORE INTO t_venues (venue_name, street_address, setting_type) VALUES (?, ?, ?);"
        val insertEvent = """
            INSERT OR IGNORE INTO t_events (
                venue_id, event_name, organizer_email, website_url, 
                event_frequency, accepts_vouchers, season_start, season_end, 
                default_day_of_week, daily_start_time, daily_end_time, saved_pitch_text
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val insertInstance = """
            INSERT INTO t_event_instances (event_id, calendar_date, app_deadline, booth_size_text, fee_amount, target_layout, anticipated_revenue, status_enum) 
            VALUES (?, ?, ?, ?, ?, ?, ?, 'Pipeline');
        """.trimIndent()

        getConnection().use { conn ->
            conn.prepareStatement(insertVenue).use { ps ->
                ps.setString(1, "Oaks Park")
                ps.setString(2, "7805 SE Oaks Park Way, Portland, OR")
                ps.setString(3, "Hybrid")
                ps.executeUpdate()
            }

            conn.prepareStatement(insertEvent, java.sql.Statement.RETURN_GENERATED_KEYS).use { ps ->
                ps.setInt(1, 1)
                ps.setString(2, "Multnomah County Fair")
                ps.setString(3, "info@multnomahcountyfair.org")
                ps.setString(4, "https://oakspark.com")
                ps.setString(5, "Annual")
                ps.setInt(6, 0)
                ps.setString(7, "May")
                ps.setString(8, "May")
                ps.setString(9, "Saturday")
                ps.setString(10, "10:00 AM")
                ps.setString(11, "07:00 PM")
                ps.setString(12, "Meticulous optical detailing and micro-hardware tuning service for eyewear frames.")
                ps.executeUpdate()

                val rs = ps.generatedKeys
                if (rs.next()) {
                    val eventId = rs.getInt(1)
                    conn.prepareStatement(insertInstance).use { instPs ->
                        instPs.setInt(1, eventId)
                        instPs.setString(2, "2027-05-22")
                        instPs.setString(3, "2027-02-15")
                        instPs.setString(4, "10x10 Covered Tent")
                        instPs.setDouble(5, 150.00)
                        instPs.setString(6, "Dual 24x16 Side-by-Side")
                        instPs.setDouble(7, 800.00)
                        instPs.executeUpdate()
                    }
                }
            }
        }
    }

    // Explicit type declaration constraints to clear your compiler logs
    fun fetchUpcomingPipeline(): kotlin.collections.List<PipelineItem> {
        val items = mutableListOf<PipelineItem>()
        val query = """
            SELECT i.instance_id, e.event_name, v.venue_name, i.calendar_date, i.app_deadline, i.fee_amount, i.anticipated_revenue, i.status_enum
            FROM t_event_instances i
            JOIN t_events e ON i.event_id = e.event_id
            JOIN t_venues v ON e.venue_id = v.venue_id
            WHERE i.status_enum != 'Completed'
            ORDER BY i.app_deadline ASC;
        """.trimIndent()

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(query)
                while (rs.next()) {
                    items.add(PipelineItem(
                        instanceId = rs.getInt("instance_id"),
                        eventName = rs.getString("event_name"),
                        venueName = rs.getString("venue_name"),
                        calendarDate = rs.getString("calendar_date"),
                        appDeadline = rs.getString("app_deadline") ?: "No Deadline",
                        feeAmount = rs.getDouble("fee_amount"),
                        anticipatedRevenue = rs.getDouble("anticipated_revenue"),
                        status = rs.getString("status_enum")
                    ))
                }
            }
        }
        return items
    }

    fun fetchMonthlyDistribution(): kotlin.collections.List<MonthlyEventCount> {
        val metrics = mutableListOf<MonthlyEventCount>()
        val query = """
            SELECT strftime('%Y-%m', calendar_date) AS event_month,
                   COUNT(instance_id) AS total_events,
                   SUM(fee_amount) AS total_fees
            FROM t_event_instances
            GROUP BY event_month
            ORDER BY event_month ASC;
        """.trimIndent()

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(query)
                while (rs.next()) {
                    val month = rs.getString("event_month") ?: "Unknown"
                    metrics.add(MonthlyEventCount(
                        monthString = month,
                        totalEvents = rs.getInt("total_events"),
                        totalFees = rs.getDouble("total_fees")
                    ))
                }
            }
        }
        return metrics
    }

    fun insertManualEventInstance(
        venueId: Int,
        name: String,
        date: String,
        deadline: String,
        fee: Double,
        targetLayout: String,
        anticipatedRevenue: Double,
        frequency: String,
        acceptsVouchers: Boolean,
        seasonStart: String,
        seasonEnd: String,
        defaultDay: String,
        startTime: String,
        endTime: String,
        pitchText: String,
        email: String
    ): Boolean {
        val insertEvent = """
            INSERT OR IGNORE INTO t_events (
                venue_id, event_name, event_frequency, accepts_vouchers, organizer_email,
                season_start, season_end, default_day_of_week, daily_start_time, daily_end_time, saved_pitch_text
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()

        val insertInstance = """
            INSERT INTO t_event_instances (event_id, calendar_date, app_deadline, fee_amount, target_layout, anticipated_revenue, status_enum)
            VALUES ((SELECT event_id FROM t_events WHERE event_name = ?), ?, ?, ?, ?, ?, 'Pipeline');
        """.trimIndent()

        var javaExecutionSuccess = false
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement(insertEvent).use { ps ->
                    ps.setInt(1, venueId)
                    ps.setString(2, name)
                    ps.setString(3, frequency)
                    ps.setInt(4, if (acceptsVouchers) 1 else 0)
                    ps.setString(5, email)
                    ps.setString(6, seasonStart)
                    ps.setString(7, seasonEnd)
                    ps.setString(8, defaultDay)
                    ps.setString(9, startTime)
                    ps.setString(10, endTime)
                    ps.setString(11, pitchText)
                    ps.executeUpdate()
                }
                conn.prepareStatement(insertInstance).use { ps ->
                    ps.setString(1, name)
                    ps.setString(2, date)
                    ps.setString(3, deadline)
                    ps.setDouble(4, fee)
                    ps.setString(5, targetLayout)
                    ps.setDouble(6, anticipatedRevenue)
                    ps.executeUpdate()
                }
                conn.commit()
                javaExecutionSuccess = true
            } catch (e: Exception) {
                conn.rollback()
                println("Insertion Failed: ${e.localizedMessage}")
            }
        }
        return javaExecutionSuccess
    }
    fun fetchVenuesLookup(): kotlin.collections.List<VenueLookup> {
        val list = mutableListOf<VenueLookup>()
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT venue_id, venue_name FROM t_venues;")
                while (rs.next()) {
                    list.add(VenueLookup(rs.getInt("venue_id"), rs.getString("venue_name")))
                }
            }
        }
        return list
    }
}