package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppConfig
import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem
import com.example.data.repository.LiquidationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecordFilterTab {
    ALL,
    ACTIVE,
    ELIGIBLE,
    SOLD
}

enum class ContextMenuOption(val title: String) {
    UNITS("Birimler"),
    STATUS("Durum"),
    DEFAULTS("Varsayılanlar"),
    CONFIG("Yapılandırma")
}

class LiquidationViewModel(
    private val repository: LiquidationRepository
) : ViewModel() {

    private val _filterTab = MutableStateFlow(RecordFilterTab.ALL)
    val filterTab: StateFlow<RecordFilterTab> = _filterTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isUpdatingPrices = MutableStateFlow(false)
    val isUpdatingPrices: StateFlow<Boolean> = _isUpdatingPrices.asStateFlow()

    private val _priceUpdateMessage = MutableStateFlow<String?>(null)
    val priceUpdateMessage: StateFlow<String?> = _priceUpdateMessage.asStateFlow()

    // Dialogs / Sheet States
    private val _showCreateSheet = MutableStateFlow(false)
    val showCreateSheet: StateFlow<Boolean> = _showCreateSheet.asStateFlow()

    private val _recordToEdit = MutableStateFlow<LiquidationRecord?>(null)
    val recordToEdit: StateFlow<LiquidationRecord?> = _recordToEdit.asStateFlow()

    private val _recordToSell = MutableStateFlow<LiquidationRecord?>(null)
    val recordToSell: StateFlow<LiquidationRecord?> = _recordToSell.asStateFlow()

    private val _activeContextMenu = MutableStateFlow<ContextMenuOption?>(null)
    val activeContextMenu: StateFlow<ContextMenuOption?> = _activeContextMenu.asStateFlow()

    private val _activeDetailsRecord = MutableStateFlow<LiquidationRecord?>(null)
    val activeDetailsRecord: StateFlow<LiquidationRecord?> = _activeDetailsRecord.asStateFlow()

    val allUnits: StateFlow<List<UnitItem>> = repository.allUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfig> = repository.appConfig
        .combine(MutableStateFlow(AppConfig())) { config, default ->
            config ?: default
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    // Filtered records correlated with live market prices
    val records: StateFlow<List<LiquidationRecord>> = combine(
        repository.allRecords,
        repository.allUnits,
        _filterTab,
        _searchQuery
    ) { list, unitsList, filter, query ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                RecordFilterTab.ALL -> true
                RecordFilterTab.ACTIVE -> !item.isSold
                RecordFilterTab.SOLD -> item.isSold
                RecordFilterTab.ELIGIBLE -> {
                    if (item.isSold) false
                    else {
                        val unit = com.example.util.MarketUtils.findMatchingUnit(item, unitsList)
                        com.example.util.MarketUtils.isEligibleForSale(item, unit?.currentPrice)
                    }
                }
            }
            val matchesQuery = query.isBlank() ||
                item.assetSymbol.contains(query, ignoreCase = true) ||
                item.unit.contains(query, ignoreCase = true) ||
                item.notes.contains(query, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawRecords: StateFlow<List<LiquidationRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultDataSeeded()
        }
    }

    fun setFilterTab(tab: RecordFilterTab) {
        _filterTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearPriceUpdateMessage() {
        _priceUpdateMessage.value = null
    }

    /**
     * API yoluyla otomatik fiyat güncelleme
     */
    fun refreshPrices() {
        viewModelScope.launch {
            _isUpdatingPrices.value = true
            try {
                val updatedCount = repository.refreshPricesFromApi()
                _priceUpdateMessage.value = if (updatedCount > 0) {
                    "$updatedCount varlığın fiyatı güncellendi."
                } else {
                    "Fiyatlar kontrol edildi (bağlantı aktif)."
                }
            } catch (e: Exception) {
                _priceUpdateMessage.value = "Fiyat güncellenirken hata oluştu: ${e.message}"
            } finally {
                _isUpdatingPrices.value = false
            }
        }
    }

    // Modal controls
    fun openCreateSheet() {
        _showCreateSheet.value = true
    }

    fun closeCreateSheet() {
        _showCreateSheet.value = false
    }

    fun openEditDialog(record: LiquidationRecord) {
        _recordToEdit.value = record
    }

    fun closeEditDialog() {
        _recordToEdit.value = null
    }

    fun openSellDialog(record: LiquidationRecord) {
        _recordToSell.value = record
    }

    fun closeSellDialog() {
        _recordToSell.value = null
    }

    fun openContextMenu(option: ContextMenuOption) {
        _activeContextMenu.value = option
    }

    fun closeContextMenu() {
        _activeContextMenu.value = null
    }

    fun openDetails(record: LiquidationRecord) {
        _activeDetailsRecord.value = record
    }

    fun closeDetails() {
        _activeDetailsRecord.value = null
    }

    // Actions
    fun createRecord(
        assetSymbol: String,
        buyPrice: Double,
        amount: Double,
        targetPercentage: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val defaultUnit = appConfig.value.defaultUnit
            repository.insertRecord(
                LiquidationRecord(
                    assetSymbol = assetSymbol.trim().uppercase(),
                    buyPrice = buyPrice,
                    amount = amount,
                    targetPercentage = targetPercentage,
                    unit = defaultUnit,
                    notes = notes.trim()
                )
            )
            _showCreateSheet.value = false
        }
    }

    fun updateRecord(
        id: Long,
        assetSymbol: String,
        buyPrice: Double,
        amount: Double,
        targetPercentage: Double,
        notes: String,
        unit: String,
        createdAt: Long,
        isSold: Boolean,
        salePrice: Double?,
        salePnl: Double?,
        saleDate: Long?
    ) {
        viewModelScope.launch {
            repository.updateRecord(
                LiquidationRecord(
                    id = id,
                    assetSymbol = assetSymbol.trim().uppercase(),
                    buyPrice = buyPrice,
                    amount = amount,
                    targetPercentage = targetPercentage,
                    unit = unit,
                    notes = notes.trim(),
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isSold = isSold,
                    salePrice = salePrice,
                    salePnl = salePnl,
                    saleDate = saleDate
                )
            )
            _recordToEdit.value = null
        }
    }

    fun sellRecord(id: Long, salePrice: Double) {
        viewModelScope.launch {
            repository.markAsSold(id, salePrice)
            _recordToSell.value = null
        }
    }

    fun reopenRecord(id: Long) {
        viewModelScope.launch {
            repository.reopenRecord(id)
        }
    }

    fun deleteRecord(record: LiquidationRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            if (_recordToEdit.value?.id == record.id) {
                _recordToEdit.value = null
            }
            if (_recordToSell.value?.id == record.id) {
                _recordToSell.value = null
            }
            if (_activeDetailsRecord.value?.id == record.id) {
                _activeDetailsRecord.value = null
            }
        }
    }

    // Units management
    fun addUnit(name: String, symbol: String, currentPrice: Double = 0.0) {
        viewModelScope.launch {
            repository.insertUnit(
                UnitItem(
                    name = name.trim().uppercase(),
                    symbol = symbol.trim().ifBlank { "${name.trim().uppercase()}/USDT" },
                    currentPrice = currentPrice,
                    isDefault = false,
                    isSystem = false
                )
            )
        }
    }

    fun updateUnitManualPrice(id: Long, price: Double) {
        viewModelScope.launch {
            repository.updateUnitManualPrice(id, price)
        }
    }

    fun deleteUnit(unit: UnitItem) {
        viewModelScope.launch {
            repository.deleteUnit(unit)
        }
    }

    fun setDefaultUnit(id: Long) {
        viewModelScope.launch {
            repository.setDefaultUnit(id)
        }
    }

    // Config management
    fun updateConfig(config: AppConfig) {
        viewModelScope.launch {
            repository.updateConfig(config)
        }
    }
}

class LiquidationViewModelFactory(
    private val repository: LiquidationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiquidationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LiquidationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
