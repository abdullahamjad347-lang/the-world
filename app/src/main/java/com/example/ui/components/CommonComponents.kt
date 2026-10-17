package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun UrduRtlProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrduTopAppBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = LightMint
                        )
                    )
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward, // RTL back arrow is forward pointing
                        contentDescription = "واپس جائیں",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryDarkGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (status.lowercase()) {
        "pending" -> Triple(Color(0xFFFFF3CD), WarningAmber, "زیر التواء")
        "accepted" -> Triple(LightMint, SecondaryGreen, "قبول شدہ")
        "declined" -> Triple(Color(0xFFFFDAD6), ErrorRed, "مسترد شدہ")
        "recorded" -> Triple(LightMint, SecondaryGreen, "مکمل حاضری")
        "incomplete" -> Triple(Color(0xFFFFF3CD), WarningAmber, "نامکمل")
        "needs_review" -> Triple(Color(0xFFFFDAD6), ErrorRed, "جائزہ درکار")
        else -> Triple(Color(0xFFEEEEEE), SecondaryText, "درج نہیں")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TimePickerDialog24h(
    initialTime: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedHour by remember {
        mutableStateOf(
            if (initialTime != "-" && initialTime.contains(":")) {
                initialTime.split(":")[0].toIntOrNull() ?: 8
            } else 8
        )
    }

    var selectedMinute by remember {
        mutableStateOf(
            if (initialTime != "-" && initialTime.contains(":")) {
                initialTime.split(":")[1].toIntOrNull() ?: 0
            } else 0
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryDarkGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display
                Text(
                    text = String.format("%02d:%02d", selectedHour, selectedMinute),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryGreen
                )
                Text(
                    text = "(24 گھنٹے فارمیٹ)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Hours Picker
                Text(
                    text = "گھنٹہ منتخب کریں (00 - 23):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کم کریں")
                    }
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedHour = if (selectedHour < 23) selectedHour + 1 else 0 }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "بڑھائیں")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minutes Picker
                Text(
                    text = "منٹ منتخب کریں (00 - 55):",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55 }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "کم کریں")
                    }
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = (it.toInt() / 5) * 5 },
                        valueRange = 0f..55f,
                        steps = 10,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedMinute = if (selectedMinute <= 50) selectedMinute + 5 else 0 }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "بڑھائیں")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { onConfirm("-") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("خالی کریں (-)")
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("منسوخ")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm(String.format("%02d:%02d", selectedHour, selectedMinute))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                        ) {
                            Text("محفوظ کریں")
                        }
                    }
                }
            }
        }
    }
}
