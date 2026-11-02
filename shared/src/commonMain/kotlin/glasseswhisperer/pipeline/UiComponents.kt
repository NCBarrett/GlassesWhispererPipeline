package glasseswhisperer.pipeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PipelineDashboard(items: List<PipelineItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Active Tri-County Event Pipeline", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Event / Venue", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("Deadline", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Fee", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Bold)
            Text("Status", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(2f)) {
                            Text(item.eventName, fontWeight = FontWeight.SemiBold)
                            Text(item.venueName, style = MaterialTheme.typography.caption, color = Color.Gray)
                        }
                        Text(item.appDeadline, modifier = Modifier.weight(1f))
                        Text("$${String.format("%.2f", item.feeAmount)}", modifier = Modifier.weight(0.6f))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (item.status == "Pipeline") Color(0xFFFFF3CD) else Color(0xFFD1E7DD))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.status, style = MaterialTheme.typography.caption, color = if (item.status == "Pipeline") Color(0xFF664D03) else Color(0xFF0F5132))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTimelineGraph(data: List<MonthlyEventCount>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monthly Event Volume & Fee Footprint", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No upcoming timeline data to graph.", color = Color.Gray)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(130.dp).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxEvents = data.maxOf { it.totalEvents }.coerceAtLeast(1)
                    data.forEach { monthMetric ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${monthMetric.totalEvents} Fairs", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("$${monthMetric.totalFees.toInt()}", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((80 * monthMetric.totalEvents / maxEvents).dp)
                                    .background(MaterialTheme.colors.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(monthMetric.monthString, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEventInstanceForm(
    venues: List<VenueLookup>,
    onSaveClick: (
        venueId: Int, name: String, date: String, deadline: String, fee: Double,
        layout: String, revenue: Double, frequency: String, acceptsVouchers: Boolean,
        seasonStart: String, seasonEnd: String, defaultDay: String, startTime: String, endTime: String, pitchText: String, email: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var eventName by remember { mutableStateOf("") }
    var selectedVenue by remember { mutableStateOf<VenueLookup?>(null) }
    var calendarDate by remember { mutableStateOf("") }
    var appDeadline by remember { mutableStateOf("") }
    var feeAmount by remember { mutableStateOf("") }
    var anticipatedRevenue by remember { mutableStateOf("") }
    var organizerEmail by remember { mutableStateOf("") }

    var seasonStart by remember { mutableStateOf("") }
    var seasonEnd by remember { mutableStateOf("") }
    var defaultDay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var savedPitchText by remember { mutableStateOf("") }

    var eventFrequency by remember { mutableStateOf("One-Time") }
    var acceptsVouchers by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Operational CRM & Lead Form", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)

        Column {
            Text("Linked Database Venue", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).clickable { dropdownExpanded = true }.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedVenue?.name ?: "Select venue relation...")
                    Icon(Icons.Default.ArrowDropDown, "")
                }
                DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    venues.forEach { venue ->
                        DropdownMenuItem(onClick = { selectedVenue = venue; dropdownExpanded = false }) { Text(venue.name) }
                    }
                }
            }
        }

        OutlinedTextField(value = eventName, onValueChange = { eventName = it }, label = { Text("Event Circuit Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = organizerEmail, onValueChange = { organizerEmail = it }, label = { Text("Inquiry Contact Email") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = calendarDate, onValueChange = { calendarDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = appDeadline, onValueChange = { appDeadline = it }, label = { Text("App Deadline") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = seasonStart, onValueChange = { seasonStart = it }, label = { Text("Season Start (e.g. May)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = seasonEnd, onValueChange = { seasonEnd = it }, label = { Text("Season End") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start Time of Day") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End Time of Day") }, modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = defaultDay, onValueChange = { defaultDay = it }, label = { Text("Default Day of Week") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = feeAmount, onValueChange = { feeAmount = it }, label = { Text("Booth Fee Cost ($)") }, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(value = anticipatedRevenue, onValueChange = { anticipatedRevenue = it }, label = { Text("Anticipated Metric Revenue ($)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = savedPitchText, onValueChange = { savedPitchText = it }, label = { Text("Saved Vendor Application Verbiage") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = acceptsVouchers, onCheckedChange = { acceptsVouchers = it })
            Text("Accepts Community Vouchers (e.g. Jade Dollars)", fontSize = 12.sp)
        }
        Button(
            onClick = {
                val vId = selectedVenue?.id ?: return@Button
                val parsedFee = feeAmount.toDoubleOrNull() ?: 0.0
                val parsedRev = anticipatedRevenue.toDoubleOrNull() ?: 0.0
                onSaveClick(vId, eventName, calendarDate, appDeadline, parsedFee, "Dual 24x16 Side-by-Side", parsedRev, eventFrequency, acceptsVouchers, seasonStart, seasonEnd, defaultDay, startTime, endTime, savedPitchText, organizerEmail)
                eventName = ""; selectedVenue = null; calendarDate = ""; appDeadline = ""; feeAmount = ""; anticipatedRevenue = ""; seasonStart = ""; seasonEnd = ""; startTime = ""; endTime = ""; defaultDay = ""; savedPitchText = ""; organizerEmail = ""
            },
            modifier = Modifier.align(Alignment.End).height(45.dp),
            enabled = selectedVenue != null && eventName.isNotBlank() && calendarDate.isNotBlank()
        ) {
            Text("Commit Data Record", fontWeight = FontWeight.Bold)
        }
    }
}