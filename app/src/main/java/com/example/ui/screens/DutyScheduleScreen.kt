package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PredefinedSchedules
import com.example.data.model.User
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*

@Composable
fun DutyScheduleScreen(
    currentUser: User,
    initialGroupId: String = "A",
    onBackClick: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf(initialGroupId) }
    var selectedDayOfWeek by remember { mutableStateOf(1) } // 1 = Monday (پیر)

    val staffList = remember(selectedDayOfWeek) {
        PredefinedSchedules.getStaffForDay(selectedDayOfWeek)
    }

    val daysList = listOf(
        Pair(1, "پیر"),
        Pair(2, "منگل"),
        Pair(3, "بدھ"),
        Pair(4, "جمعرات"),
        Pair(5, "جمعہ"),
        Pair(6, "ہفتہ"),
        Pair(7, "اتوار")
    )

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "ہفتہ وار ڈیوٹی شیڈول",
                    subtitle = "پیشگی طے شدہ کارکنان کی لسٹ",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = MainBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Group selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("A", "B", "C").forEach { g ->
                        FilterChip(
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = g },
                            label = { Text("گروپ $g", fontWeight = if (selectedGroup == g) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Weekday Scrollable Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedDayOfWeek - 1,
                    containerColor = CardBackground,
                    contentColor = PrimaryDarkGreen,
                    edgePadding = 12.dp
                ) {
                    daysList.forEach { (dayNum, dayName) ->
                        Tab(
                            selected = selectedDayOfWeek == dayNum,
                            onClick = { selectedDayOfWeek = dayNum },
                            text = {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (selectedDayOfWeek == dayNum) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightMint),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${PredefinedSchedules.getDayNameUrdu(selectedDayOfWeek)} — گروپ $selectedGroup",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryDarkGreen
                                    )
                                    Text(
                                        text = "کل مقررہ کارکنان: ${staffList.size} افراد",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryGreen
                                    )
                                }
                                Surface(
                                    color = PrimaryDarkGreen,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "پیشگی شیڈول",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(staffList) { index, staff ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (staff.isCoordinator) AccentLime else LightMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryDarkGreen
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = staff.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MainText
                                    )
                                    Text(
                                        text = "سیکیورٹی ڈیوٹی ممبر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText
                                    )
                                }

                                if (staff.isCoordinator) {
                                    Surface(
                                        color = PrimaryDarkGreen,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = AccentLime,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "کوآرڈینیٹر",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
