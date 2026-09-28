package com.sahil.retailcore.ui.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.CustomerEntity
import com.sahil.retailcore.data.local.entity.PaymentEntity
import com.sahil.retailcore.data.local.entity.SaleEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    customerId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModelFactory(
            app.customerRepository,
            app.supplierRepository,
            app.saleRepository,
            app.purchaseRepository,
            app.paymentRepository
        )
    )

    val scope = rememberCoroutineScope()
    var customer by remember { mutableStateOf<CustomerEntity?>(null) }
    val payments by viewModel.getCustomerPayments(customerId).collectAsState(initial = emptyList())
    val sales by viewModel.getCustomerSales(customerId).collectAsState(initial = emptyList())

    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        customer = app.customerRepository.getCustomerById(customerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Ledger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = { showPaymentDialog = true }) {
                Text("Collect Payment")
            }
        }
    ) { padding ->
        customer?.let { c ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(c.phone)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LedgerStat("Total Sales", "₹${c.totalSales}")
                                LedgerStat("Received", "₹${c.totalReceived}")
                                LedgerStat("Outstanding", "₹${c.outstandingBalance}", MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item { Text("Transaction History", fontWeight = FontWeight.Bold) }

                val history = (sales.map { it to "SALE" } + payments.map { it to "PAYMENT" })
                    .sortedByDescending { 
                        if (it.first is SaleEntity) (it.first as SaleEntity).createdAt 
                        else (it.first as PaymentEntity).paymentDate 
                    }

                items(history) { item ->
                    if (item.second == "SALE") {
                        val sale = item.first as SaleEntity
                        TransactionRow("Sale #${sale.id.take(8).uppercase()}", sale.totalAmount, "DEBIT", sale.createdAt)
                    } else {
                        val payment = item.first as PaymentEntity
                        TransactionRow("Payment (${payment.paymentMode})", payment.amount, "CREDIT", payment.paymentDate)
                    }
                }

                if (c.openingBalance > 0) {
                    item {
                        TransactionRow(
                            title = "Opening Balance",
                            amount = c.openingBalance,
                            type = "DEBIT",
                            date = c.openingBalanceDate,
                            subtitle = "Previous Outstanding Before RetailCore"
                        )
                    }
                }
            }
        }

        if (showPaymentDialog) {
            PaymentDialog(
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, mode, notes ->
                    viewModel.collectPayment(customerId, amount, mode, notes) { success ->
                        if (success) {
                            showPaymentDialog = false
                            scope.launch {
                                customer = app.customerRepository.getCustomerById(customerId)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun LedgerStat(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TransactionRow(title: String, amount: Double, type: String, date: Long, subtitle: String? = null) {
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, fontWeight = FontWeight.Medium)
                Text(dateFormat.format(Date(date)), style = MaterialTheme.typography.bodySmall)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Text(
                if (type == "DEBIT") "+ ₹$amount" else "- ₹$amount",
                color = if (type == "DEBIT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentDialog(onDismiss: () -> Unit, onConfirm: (Double, String, String?) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Cash") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Cash", "UPI", "Card").forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, mode, notes) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
