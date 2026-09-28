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
import com.sahil.retailcore.data.local.entity.SupplierEntity
import com.sahil.retailcore.data.local.entity.PaymentEntity
import com.sahil.retailcore.data.local.entity.PurchaseEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierLedgerScreen(
    supplierId: String,
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
    var supplier by remember { mutableStateOf<SupplierEntity?>(null) }
    val payments by viewModel.getSupplierPayments(supplierId).collectAsState(initial = emptyList())
    val purchases by viewModel.getSupplierPurchases(supplierId).collectAsState(initial = emptyList())

    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(supplierId) {
        supplier = app.supplierRepository.getSupplierById(supplierId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Ledger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = { showPaymentDialog = true }) {
                Text("Record Payment")
            }
        }
    ) { padding ->
        supplier?.let { s ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(s.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(s.phone)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LedgerStat("Total Purchases", "₹${s.totalPurchases}")
                                LedgerStat("Total Paid", "₹${s.totalPaid}")
                                LedgerStat("Outstanding", "₹${s.outstandingBalance}", MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item { Text("Transaction History", fontWeight = FontWeight.Bold) }

                val history = (purchases.map { it to "PURCHASE" } + payments.map { it to "PAYMENT" })
                    .sortedByDescending { 
                        if (it.first is PurchaseEntity) (it.first as PurchaseEntity).createdAt 
                        else (it.first as PaymentEntity).paymentDate 
                    }

                items(history) { item ->
                    if (item.second == "PURCHASE") {
                        val purchase = item.first as PurchaseEntity
                        TransactionRow("Purchase #${purchase.invoiceNumber}", purchase.grandTotal, "DEBIT", purchase.createdAt)
                    } else {
                        val payment = item.first as PaymentEntity
                        TransactionRow("Payment (${payment.paymentMode})", payment.amount, "CREDIT", payment.paymentDate)
                    }
                }
            }
        }

        if (showPaymentDialog) {
            PaymentDialog(
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, mode, notes ->
                    viewModel.paySupplier(supplierId, amount, mode, notes) { success ->
                        if (success) {
                            showPaymentDialog = false
                            scope.launch {
                                supplier = app.supplierRepository.getSupplierById(supplierId)
                            }
                        }
                    }
                }
            )
        }
    }
}
