package com.sahil.retailcore.ui.ledger

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sahil.retailcore.RetailApp
import com.sahil.retailcore.data.local.entity.CustomerEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerListScreen(
    onNavigateToCustomerLedger: (String) -> Unit,
    onNavigateToSupplierLedger: (String) -> Unit
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

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Customers", "Suppliers")

    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    var showMigrationDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Financial Ledgers") })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showMigrationDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Previous Customer") }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddSupplierDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Supplier") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    items(customers) { customer ->
                        LedgerItemCard(
                            name = customer.name,
                            phone = customer.phone,
                            balance = customer.outstandingBalance,
                            onClick = { onNavigateToCustomerLedger(customer.id) }
                        )
                    }
                } else {
                    items(suppliers) { supplier ->
                        SupplierLedgerItemCard(
                            supplier = supplier,
                            onClick = { onNavigateToSupplierLedger(supplier.id) }
                        )
                    }
                }
            }
        }

        if (showMigrationDialog) {
            AddPreviousCustomerDialog(
                viewModel = viewModel,
                onDismiss = { showMigrationDialog = false }
            )
        }

        if (showAddSupplierDialog) {
            AddSupplierDialog(
                viewModel = viewModel,
                onDismiss = { showAddSupplierDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.addSupplier(name, phone, gst.ifBlank { null }, address.ifBlank { null }) { success, msg ->
                        if (success) onDismiss() else errorMessage = msg
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SupplierLedgerItemCard(
    supplier: com.sahil.retailcore.data.local.entity.SupplierEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name, fontWeight = FontWeight.Bold)
                    Text(supplier.phone, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "₹${String.format(Locale.getDefault(), "%.2f", supplier.outstandingBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = if (supplier.outstandingBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Purchases", style = MaterialTheme.typography.labelSmall)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", supplier.totalPurchases)}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Paid", style = MaterialTheme.typography.labelSmall)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", supplier.totalPaid)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPreviousCustomerDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var altPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    var existingToMerge by remember { mutableStateOf<CustomerEntity?>(null) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (existingToMerge != null) {
        AlertDialog(
            onDismissRequest = { existingToMerge = null },
            title = { Text("Customer Exists") },
            text = { Text("A customer with mobile $phone already exists. Do you want to merge this previous balance with their current ledger?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.mergePreviousBalance(
                        existingToMerge!!, name, altPhone, address, gst, 
                        balance.toDoubleOrNull() ?: 0.0, date, notes
                    ) { success, _ ->
                        if (success) onDismiss()
                    }
                }) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = { existingToMerge = null }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Previous Customer") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = altPhone, onValueChange = { altPhone = it }, label = { Text("Alternate Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST Number") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = balance, onValueChange = { balance = it }, label = { Text("Previous Outstanding *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        value = dateFormat.format(Date(date)),
                        onValueChange = {},
                        label = { Text("Last Payment Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                cal.timeInMillis = date
                                DatePickerDialog(context, { _, y, m, d ->
                                    cal.set(y, m, d)
                                    date = cal.timeInMillis
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            }) { Icon(Icons.Default.CalendarToday, null) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.migratePreviousCustomer(
                        name, phone, altPhone.ifBlank { null }, address.ifBlank { null }, gst.ifBlank { null },
                        balance.toDoubleOrNull() ?: 0.0, date, notes.ifBlank { null },
                        onConfirmMerge = { existingToMerge = it },
                        onResult = { success, _ -> if (success) onDismiss() }
                    )
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && balance.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LedgerItemCard(
    name: String,
    phone: String,
    balance: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(name, fontWeight = FontWeight.Bold)
                Text(phone, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                Text(
                    "₹${String.format(Locale.getDefault(), "%.2f", balance)}",
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
