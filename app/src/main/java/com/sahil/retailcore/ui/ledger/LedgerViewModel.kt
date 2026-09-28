package com.sahil.retailcore.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.*
import com.sahil.retailcore.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LedgerViewModel(
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    val customers = customerRepository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers = supplierRepository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCustomerPayments(customerId: String) = paymentRepository.getPaymentsByCustomer(customerId)
    
    fun getSupplierPayments(supplierId: String) = paymentRepository.getPaymentsBySupplier(supplierId)

    fun getCustomerSales(customerId: String) = saleRepository.allSales.map { list ->
        list.filter { it.customerId == customerId }
    }

    fun getSupplierPurchases(supplierId: String) = purchaseRepository.allPurchases.map { list ->
        list.filter { it.supplierId == supplierId }
    }

    fun collectPayment(
        customerId: String,
        amount: Double,
        mode: String,
        notes: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val customer = customerRepository.getCustomerById(customerId)
                if (customer != null) {
                    val payment = PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = customerId,
                        amount = amount,
                        paymentMode = mode,
                        type = PaymentType.RECEIVED,
                        notes = notes
                    )
                    paymentRepository.insertPayment(payment)
                    
                    // Update customer balance
                    customerRepository.insertCustomer(customer.copy(
                        totalReceived = customer.totalReceived + amount,
                        outstandingBalance = customer.outstandingBalance - amount,
                        updatedAt = System.currentTimeMillis()
                    ))
                    onResult(true)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun migratePreviousCustomer(
        name: String,
        phone: String,
        altPhone: String?,
        address: String?,
        gst: String?,
        openingBalance: Double,
        openingBalanceDate: Long,
        notes: String?,
        onConfirmMerge: (CustomerEntity) -> Unit,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existing = customerRepository.getCustomerByPhone(phone)
                if (existing != null) {
                    onConfirmMerge(existing)
                    return@launch
                }

                val newCustomer = CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phone = phone,
                    altPhone = altPhone,
                    address = address,
                    gstNumber = gst,
                    openingBalance = openingBalance,
                    openingBalanceDate = openingBalanceDate,
                    outstandingBalance = openingBalance,
                    notes = notes
                )
                customerRepository.insertCustomer(newCustomer)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun addSupplier(
        name: String,
        phone: String,
        gst: String?,
        address: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingByPhone = supplierRepository.getSupplierByPhone(phone)
                if (existingByPhone != null) {
                    onResult(false, "Supplier with this mobile already exists")
                    return@launch
                }

                if (!gst.isNullOrBlank()) {
                    val existingByGst = supplierRepository.getSupplierByGst(gst)
                    if (existingByGst != null) {
                        onResult(false, "Supplier with this GST already exists")
                        return@launch
                    }
                }

                val newSupplier = SupplierEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phone = phone,
                    gstNumber = gst,
                    address = address
                )
                supplierRepository.insertSupplier(newSupplier)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun mergePreviousBalance(
        existingCustomer: CustomerEntity,
        name: String,
        altPhone: String?,
        address: String?,
        gst: String?,
        additionalOpeningBalance: Double,
        openingBalanceDate: Long,
        notes: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedCustomer = existingCustomer.copy(
                    name = name,
                    altPhone = altPhone ?: existingCustomer.altPhone,
                    address = address ?: existingCustomer.address,
                    gstNumber = gst ?: existingCustomer.gstNumber,
                    openingBalance = existingCustomer.openingBalance + additionalOpeningBalance,
                    openingBalanceDate = openingBalanceDate,
                    outstandingBalance = existingCustomer.outstandingBalance + additionalOpeningBalance,
                    notes = if (notes.isNullOrBlank()) existingCustomer.notes else "${existingCustomer.notes}\n$notes",
                    updatedAt = System.currentTimeMillis()
                )
                customerRepository.insertCustomer(updatedCustomer)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun paySupplier(
        supplierId: String,
        amount: Double,
        mode: String,
        notes: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val supplier = supplierRepository.getSupplierById(supplierId)
                if (supplier != null) {
                    val payment = PaymentEntity(
                        id = UUID.randomUUID().toString(),
                        supplierId = supplierId,
                        amount = amount,
                        paymentMode = mode,
                        type = PaymentType.PAID,
                        notes = notes
                    )
                    paymentRepository.insertPayment(payment)
                    
                    // Update supplier balance
                    supplierRepository.insertSupplier(supplier.copy(
                        totalPaid = supplier.totalPaid + amount,
                        outstandingBalance = supplier.outstandingBalance - amount,
                        updatedAt = System.currentTimeMillis()
                    ))
                    onResult(true)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}

class LedgerViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val supplierRepository: SupplierRepository,
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LedgerViewModel(
                customerRepository,
                supplierRepository,
                saleRepository,
                purchaseRepository,
                paymentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
