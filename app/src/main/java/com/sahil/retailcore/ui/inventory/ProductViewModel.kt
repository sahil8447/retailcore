package com.sahil.retailcore.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sahil.retailcore.data.local.entity.ProductEntity
import com.sahil.retailcore.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedProductId = MutableStateFlow<String?>(null)
    val selectedProductId = _selectedProductId.asStateFlow()

    fun setSelectedProduct(id: String?) {
        _selectedProductId.value = id
    }

    val products: StateFlow<List<ProductEntity>> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allProducts
            } else {
                repository.searchProducts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    suspend fun getProductById(id: String): ProductEntity? {
        return repository.getProductById(id)
    }

    fun upsertProduct(
        id: String? = null,
        name: String,
        sku: String,
        modelNumber: String,
        brand: String,
        categoryId: String,
        purchasePrice: Double,
        sellingPrice: Double,
        stockQuantity: Int,
        minimumStock: Int,
        location: String,
        notes: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Validation
        if (name.isBlank()) {
            onResult(false, "Product Name is required")
            return
        }
        if (modelNumber.isBlank()) {
            onResult(false, "Model Number is required")
            return
        }
        if (purchasePrice <= 0) {
            onResult(false, "Purchase Price must be greater than 0")
            return
        }
        if (sellingPrice <= 0) {
            onResult(false, "Selling Price must be greater than 0")
            return
        }
        if (stockQuantity < 0) {
            onResult(false, "Stock cannot be negative")
            return
        }

        val product = ProductEntity(
            id = id ?: UUID.randomUUID().toString(),
            sku = sku,
            name = name,
            modelNumber = modelNumber,
            brand = brand,
            categoryId = categoryId,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            stockQuantity = stockQuantity,
            minimumStock = minimumStock,
            location = location,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                if (id == null) {
                    repository.insertProduct(product)
                } else {
                    repository.updateProduct(product)
                }
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }
}

class ProductViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
