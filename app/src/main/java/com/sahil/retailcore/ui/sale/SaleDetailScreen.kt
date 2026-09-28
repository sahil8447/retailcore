package com.sahil.retailcore.ui.sale

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.PaymentEntity
import com.sahil.retailcore.data.local.entity.SaleEntity
import com.sahil.retailcore.data.local.entity.SaleItemEntity
import com.sahil.retailcore.data.local.entity.ProductEntity
import com.sahil.retailcore.ui.ledger.LedgerViewModel
import com.sahil.retailcore.ui.ledger.LedgerViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as RetailApp
    val viewModel: SaleViewModel = viewModel(
        factory = SaleViewModelFactory(
            app.saleRepository,
            app.customerRepository,
            app.productRepository,
            app.settingsRepository
        )
    )
    val ledgerViewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModelFactory(
            app.customerRepository,
            app.supplierRepository,
            app.saleRepository,
            app.purchaseRepository,
            app.paymentRepository
        )
    )

    var sale by remember { mutableStateOf<SaleEntity?>(null) }
    var items by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    var payments by remember { mutableStateOf<List<PaymentEntity>>(emptyList()) }
    var products by remember { mutableStateOf<Map<String, ProductEntity>>(emptyMap()) }
    var customer by remember { mutableStateOf<com.sahil.retailcore.data.local.entity.CustomerEntity?>(null) }

    LaunchedEffect(saleId) {
        val result = viewModel.getSaleWithItems(saleId)
        sale = result.first
        items = result.second
        payments = viewModel.getSalePayments(saleId)
        
        sale?.customerId?.let { cid ->
            customer = app.customerRepository.getCustomerById(cid)
        }

        val prodMap = mutableMapOf<String, ProductEntity>()
        items.forEach { item ->
            app.productRepository.getProductById(item.productId)?.let {
                prodMap[item.productId] = it
            }
        }
        products = prodMap
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sale Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    sale?.let { s ->
                        IconButton(onClick = { viewModel.shareInvoice(context, s) }) {
                            Icon(Icons.Default.Share, null)
                        }
                        IconButton(onClick = { viewModel.printInvoice(context, s) }) {
                            Icon(Icons.Default.Print, null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        sale?.let { s ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(s.invoiceNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("₹${String.format(Locale.getDefault(), "%.2f", s.totalAmount)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Text(dateFormat.format(Date(s.createdAt)), style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Customer: ${customer?.name ?: "Cash Customer"}", fontWeight = FontWeight.Medium)
                            Text("Phone: ${customer?.phone ?: ""}", style = MaterialTheme.typography.bodySmall)
                            customer?.address?.let {
                                if (it.isNotBlank()) {
                                    Text("Address: $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            customer?.gstNumber?.let {
                                if (it.isNotBlank()) {
                                    Text("GST: $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Products
                item { Text("Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                
                items(items) { item ->
                    val product = products[item.productId]
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(product?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                            Text("Model: ${product?.modelNumber ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Qty: ${item.quantity} x ₹${item.unitPrice}")
                                Text("Total: ₹${String.format(Locale.getDefault(), "%.2f", item.subtotal)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Payment History
                item { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val outstanding = s.totalAmount - payments.sumOf { it.amount }
                        if (outstanding > 0) {
                            Button(onClick = { showPaymentDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                                Text("Collect ₹${String.format(Locale.getDefault(), "%.2f", outstanding)}")
                            }
                        }
                    }
                }

                items(payments) { p ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(p.paymentMode, fontWeight = FontWeight.Medium)
                            Text(dateFormat.format(Date(p.paymentDate)), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("₹${String.format(Locale.getDefault(), "%.2f", p.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                item { HorizontalDivider() }

                // Footer Totals
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SaleSummaryRow("Subtotal (Incl. GST)", "₹${String.format(Locale.getDefault(), "%.2f", s.totalAmount + s.discount)}")
                        SaleSummaryRow("Total Discount", "₹${String.format(Locale.getDefault(), "%.2f", s.discount)}", color = MaterialTheme.colorScheme.error)
                        SaleSummaryRow("Grand Total", "₹${String.format(Locale.getDefault(), "%.2f", s.totalAmount)}", isBold = true, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }

        if (showPaymentDialog && sale != null) {
            com.sahil.retailcore.ui.ledger.PaymentDialog(
                onDismiss = { showPaymentDialog = false },
                onConfirm = { amount, mode, notes ->
                    sale?.customerId?.let { cid ->
                        ledgerViewModel.collectPayment(cid, amount, mode, notes) { success ->
                            if (success) {
                                showPaymentDialog = false
                            }
                        }
                    }
                }
            )
        }
    }
}
