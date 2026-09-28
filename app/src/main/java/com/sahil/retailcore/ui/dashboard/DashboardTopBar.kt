package com.sahil.retailcore.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar() {
    val sdf = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
    val currentDate = sdf.format(Date())

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Welcome back, Sahil",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "RetailCore ERP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            Text(
                text = currentDate,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 16.dp),
                color = MaterialTheme.colorScheme.outline
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
