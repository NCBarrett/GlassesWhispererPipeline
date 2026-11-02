package glasseswhisperer.pipeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    val windowState = rememberWindowState(width = 1300.dp, height = 780.dp)

    // Connect directly to your persistent SQLite data file
    val dbManager = remember { EventDatabaseManager() }

    // Seed initial regional assets and venues on application boot
    LaunchedEffect(Unit) {
        dbManager.seedInitialPipeline()
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "The Glass Whisperer - Tri-County Event Engine"
    ) {
        // State Hoisting: Reactive UI containers tracking data changes dynamically
        var pipelineState by remember { mutableStateOf(dbManager.fetchUpcomingPipeline()) }
        var graphState by remember { mutableStateOf(dbManager.fetchMonthlyDistribution()) }
        val venuesList = remember { dbManager.fetchVenuesLookup() }

        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {

                // Left Panel Layout: Your expanded operational CRM form
                AddEventInstanceForm(
                    venues = venuesList,
                    onSaveClick = { venueId, name, date, deadline, fee, layout, revenue, freq, vouchers, startMo, endMo, day, sTime, endTime, pitch, email ->

                        // Execute transaction block via your database manager layer
                        val commitSuccess = dbManager.insertManualEventInstance(
                            venueId, name, date, deadline, fee, layout, revenue, freq, vouchers, startMo, endMo, day, sTime, endTime, pitch, email
                        )

                        if (commitSuccess) {
                            // State Hoisting Trigger Loop: Instantly rerenders your panels on-screen!
                            pipelineState = dbManager.fetchUpcomingPipeline()
                            graphState = dbManager.fetchMonthlyDistribution()
                        }
                    },
                    modifier = Modifier.weight(1.3f)
                )

                // Right Panel Layout: Monitoring dashboard grids & monthly volume graph
                Column(modifier = Modifier.weight(1.7f)) {
                    MonthlyTimelineGraph(data = graphState, modifier = Modifier.weight(1f))
                    PipelineDashboard(items = pipelineState, modifier = Modifier.weight(1.4f))
                }
            }
        }
    }
}
