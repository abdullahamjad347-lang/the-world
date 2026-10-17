package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.components.UrduRtlProvider
import com.example.ui.components.UrduTopAppBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    currentUser: User,
    allUsers: List<User>,
    onUpdateUser: (targetUserId: String, newGroup: String, newRole: String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("ALL") }

    var editingUser by remember { mutableStateOf<User?>(null) }
    var editRole by remember { mutableStateOf("worker") }
    var editGroup by remember { mutableStateOf("A") }

    val filteredStaff = remember(allUsers, searchQuery, selectedGroupFilter) {
        allUsers.filter { u ->
            val matchesSearch = u.displayName.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true)
            val matchesGroup = selectedGroupFilter == "ALL" || u.assignedGroup == selectedGroupFilter
            matchesSearch && matchesGroup
        }
    }

    UrduRtlProvider {
        Scaffold(
            topBar = {
                UrduTopAppBar(
                    title = "کارکنان اور اکاؤنٹس مینجمنٹ",
                    subtitle = "مجاز عملہ اور گروپس اسائنمنٹ",
                    showBackButton = true,
                    onBackClick = onBackClick
                )
            },
            containerColor = MainBackground
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("نام یا ای میل سے تلاش کریں") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Group filter chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("گروپ:", style = MaterialTheme.typography.bodySmall)
                        listOf("ALL", "A", "B", "C").forEach { g ->
                            FilterChip(
                                selected = selectedGroupFilter == g,
                                onClick = { selectedGroupFilter = g },
                                label = { Text(if (g == "ALL") "تمام کارکنان" else "گروپ $g") }
                            )
                        }
                    }
                }

                if (filteredStaff.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        ) {
                            Text(
                                text = "کوئی کارکن دستیاب نہیں ملا۔",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(filteredStaff, key = { it.userId }) { user ->
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (user.isSuperAdmin) AccentLime else LightMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (user.isSuperAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PrimaryDarkGreen
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MainText
                                    )
                                    Text(
                                        text = "${user.email.ifBlank { "کوئی ای میل درج نہیں" }}  |  گروپ: ${user.assignedGroup}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryText
                                    )
                                    Surface(
                                        color = when (user.role) {
                                            "super_admin" -> Color(0xFFDDF5EA)
                                            "group_admin" -> Color(0xFFFFF3CD)
                                            else -> Color(0xFFE8F0EC)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when (user.role) {
                                                "super_admin" -> "سپر ایڈمنسٹریٹر"
                                                "group_admin" -> "گروپ ایڈمن"
                                                else -> "کارکن"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrimaryDarkGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingUser = user
                                        editRole = user.role
                                        editGroup = user.assignedGroup
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "ترمیم کریں", tint = SecondaryGreen)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        // Edit User Dialog
        if (editingUser != null) {
            val user = editingUser!!
            AlertDialog(
                onDismissRequest = { editingUser = null },
                title = {
                    Text(
                        text = "کارکن کی تفصیلات میں ترمیم",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryDarkGreen
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "نام: ${user.displayName}", fontWeight = FontWeight.Bold)

                        Text("ڈیوٹی گروپ تفویض کریں:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("A", "B", "C", "ALL").forEach { g ->
                                FilterChip(
                                    selected = editGroup == g,
                                    onClick = { editGroup = g },
                                    label = { Text(if (g == "ALL") "تمام گروپس" else "گروپ $g") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("کردار (Role):", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = editRole == "worker",
                                onClick = { editRole = "worker" },
                                label = { Text("کارکن") }
                            )
                            FilterChip(
                                selected = editRole == "group_admin",
                                onClick = { editRole = "group_admin" },
                                label = { Text("گروپ ایڈمن") }
                            )
                            FilterChip(
                                selected = editRole == "super_admin",
                                onClick = { editRole = "super_admin" },
                                label = { Text("سپر ایڈمن") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateUser(user.userId, editGroup, editRole)
                            editingUser = null
                            Toast.makeText(context, "کارکن کے اختیارات کامیابی سے اپ ڈیٹ ہو گئے!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                    ) {
                        Text("محفوظ کریں")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingUser = null }) {
                        Text("منسوخ")
                    }
                }
            )
        }
    }
}
