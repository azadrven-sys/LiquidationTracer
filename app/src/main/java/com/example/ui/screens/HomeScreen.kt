package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.WaterDrop
import com.example.util.MarketUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ConfigurationDialog
import com.example.ui.components.CreateLiquidationSheet
import com.example.ui.components.DefaultsDialog
import com.example.ui.components.EditLiquidationDialog
import com.example.ui.components.LiquidationCard
import com.example.ui.components.PositionDetailsDialog
import com.example.ui.components.SellDialog
import com.example.ui.components.StatusDialog
import com.example.ui.components.UnitsDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.ContextMenuOption
import com.example.ui.viewmodel.LiquidationViewModel
import com.example.ui.viewmodel.RecordFilterTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LiquidationViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.records.collectAsState()
    val rawRecords by viewModel.rawRecords.collectAsState()
    val units by viewModel.allUnits.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val filterTab by viewModel.filterTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val showCreateSheet by viewModel.showCreateSheet.collectAsState()
    val recordToEdit by viewModel.recordToEdit.collectAsState()
    val recordToSell by viewModel.recordToSell.collectAsState()
    val activeContextMenu by viewModel.activeContextMenu.collectAsState()
    val activeDetailsRecord by viewModel.activeDetailsRecord.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showNoUnitsDialog by remember { mutableStateOf(false) }

    // Intercept system back button when in search mode to close search instead of closing the app
    BackHandler(enabled = isSearchExpanded || searchQuery.isNotEmpty()) {
        isSearchExpanded = false
        viewModel.setSearchQuery("")
    }

    // Records matching current search query (for dynamic tab counts during search)
    val searchedRecords = remember(rawRecords, searchQuery) {
        if (searchQuery.isBlank()) {
            rawRecords
        } else {
            rawRecords.filter { item ->
                item.assetSymbol.contains(searchQuery, ignoreCase = true) ||
                item.unit.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val allRecordsCount = searchedRecords.size
    val activeRecordsCount = searchedRecords.count { !it.isSold }
    val soldRecordsCount = searchedRecords.count { it.isSold }
    val eligibleRecordsCount = remember(searchedRecords, units) {
        searchedRecords.count { !it.isSold && MarketUtils.isEligibleForSale(it, MarketUtils.findMatchingUnit(it, units)?.currentPrice) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Likidasyon Takip",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val subtitle = if (eligibleRecordsCount > 0) {
                                "$activeRecordsCount Aktif ($eligibleRecordsCount Satışa Uygun) • $soldRecordsCount Satılan"
                            } else {
                                "$activeRecordsCount Aktif • $soldRecordsCount Satılan"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) viewModel.setSearchQuery("")
                        },
                        modifier = Modifier.testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Ara"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // FAB: On click, opens new liquidation record creation screen (guarded against empty units)
            ExtendedFloatingActionButton(
                onClick = {
                    if (units.isEmpty()) {
                        showNoUnitsDialog = true
                    } else {
                        viewModel.openCreateSheet()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni Kayıt", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_liquidation_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Search Bar if expanded
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Sembol veya nota göre ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_text_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Quick Access Chips for ContextMenu Options
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        onClick = { viewModel.openContextMenu(ContextMenuOption.UNITS) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("quick_btn_birimler")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ViewModule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Birimler", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { viewModel.openContextMenu(ContextMenuOption.STATUS) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("quick_btn_durum")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldGreen)
                            Text("Durum", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { viewModel.openContextMenu(ContextMenuOption.DEFAULTS) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("quick_btn_varsayilanlar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                            Text("Varsayılanlar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { viewModel.openContextMenu(ContextMenuOption.CONFIG) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("quick_btn_yapilandirma")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text("Yapılandırma", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Filter Tabs: Tümü | Aktif | Satışa Uygun | Satılanlar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTab == RecordFilterTab.ALL,
                    onClick = { viewModel.setFilterTab(RecordFilterTab.ALL) },
                    label = { Text("Tümü ($allRecordsCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = filterTab == RecordFilterTab.ACTIVE,
                    onClick = { viewModel.setFilterTab(RecordFilterTab.ACTIVE) },
                    label = { Text("Aktif ($activeRecordsCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = filterTab == RecordFilterTab.ELIGIBLE,
                    onClick = { viewModel.setFilterTab(RecordFilterTab.ELIGIBLE) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (eligibleRecordsCount > 0) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = EmeraldGreen
                                )
                            }
                            Text("Satışa Uygun ($eligibleRecordsCount)")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldGreen
                    )
                )

                FilterChip(
                    selected = filterTab == RecordFilterTab.SOLD,
                    onClick = { viewModel.setFilterTab(RecordFilterTab.SOLD) },
                    label = { Text("Satılanlar ($soldRecordsCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main List of Liquidation Records
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (units.isEmpty()) {
                            Text(
                                text = "Henüz tanımlı birim/varlık yok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Yeni likidasyon kaydı oluşturabilmek için önce birim tanımlamalısınız.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Arama kriterine uygun kayıt bulunamadı" else "Henüz likidasyon kaydı yok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Farklı bir arama terimi deneyin." else "Aşağıdaki + butonuna basarak yeni bir kayıt ekleyebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("liquidation_list"),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp)
                ) {
                    items(
                        items = records,
                        key = { it.id }
                    ) { record ->
                        val matchedUnit = remember(record, units) { MarketUtils.findMatchingUnit(record, units) }
                        val currentPrice = matchedUnit?.currentPrice?.takeIf { it > 0.0 }
                        LiquidationCard(
                            record = record,
                            currencySymbol = config.currencySymbol,
                            currentPrice = currentPrice,
                            onSellClick = { viewModel.openSellDialog(record) },
                            onEditClick = { viewModel.openEditDialog(record) },
                            onDeleteClick = { viewModel.deleteRecord(record) },
                            onReopenClick = { viewModel.reopenRecord(record.id) },
                            onCardClick = {
                                if (record.isSold) {
                                    viewModel.openDetails(record)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Creating New Liquidation Record (Opened by FAB)
    if (showCreateSheet) {
        CreateLiquidationSheet(
            currencySymbol = config.currencySymbol,
            units = units,
            defaultTargetPercentage = config.defaultTargetPercentage,
            defaultAmount = config.defaultAmount,
            onDismiss = { viewModel.closeCreateSheet() },
            onOpenUnitsDialog = { viewModel.openContextMenu(ContextMenuOption.UNITS) },
            onSubmit = { assetSymbol, buyPrice, amount, targetPercentage, notes ->
                viewModel.createRecord(assetSymbol, buyPrice, amount, targetPercentage, notes)
            }
        )
    }

    // Dialog shown when FAB is clicked but no units exist
    if (showNoUnitsDialog) {
        AlertDialog(
            onDismissRequest = { showNoUnitsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ViewModule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Kayıtlı Birim Bulunamadı",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dialog_title_no_units")
                )
            },
            text = {
                Text(
                    text = "Yeni bir likidasyon kaydı oluşturabilmek için önce en az bir birim (varlık) tanımlamalısınız."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoUnitsDialog = false
                        viewModel.openContextMenu(ContextMenuOption.UNITS)
                    },
                    modifier = Modifier.testTag("btn_add_unit_prompt")
                ) {
                    Text("Birim Ekle")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNoUnitsDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Modal Dialog for Selling Position (Triggered by "Sat" button)
    recordToSell?.let { record ->
        val matchedUnit = remember(record, units) { MarketUtils.findMatchingUnit(record, units) }
        val currentPrice = matchedUnit?.currentPrice?.takeIf { it > 0.0 }
        SellDialog(
            record = record,
            currencySymbol = config.currencySymbol,
            currentPrice = currentPrice,
            onDismiss = { viewModel.closeSellDialog() },
            onConfirmSell = { salePrice ->
                viewModel.sellRecord(record.id, salePrice)
            }
        )
    }

    // Modal Dialog for Editing Position (Triggered by "Düzenle" button)
    recordToEdit?.let { record ->
        EditLiquidationDialog(
            record = record,
            currencySymbol = config.currencySymbol,
            units = units,
            onDismiss = { viewModel.closeEditDialog() },
            onConfirmUpdate = { assetSymbol, buyPrice, amount, targetPercentage, notes ->
                viewModel.updateRecord(
                    record.id,
                    assetSymbol,
                    buyPrice,
                    amount,
                    targetPercentage,
                    notes,
                    record.unit,
                    record.createdAt,
                    record.isSold,
                    record.salePrice,
                    record.salePnl,
                    record.saleDate
                )
            }
        )
    }

    // Modal Dialog for Details View
    activeDetailsRecord?.let { record ->
        val matchedUnit = remember(record, units) { MarketUtils.findMatchingUnit(record, units) }
        val currentPrice = matchedUnit?.currentPrice?.takeIf { it > 0.0 }
        PositionDetailsDialog(
            record = record,
            currencySymbol = config.currencySymbol,
            currentPrice = currentPrice,
            onDismiss = { viewModel.closeDetails() },
            onSellClick = { viewModel.openSellDialog(record) },
            onEditClick = { viewModel.openEditDialog(record) },
            onDeleteClick = { viewModel.deleteRecord(record) }
        )
    }

    // ContextMenu Modals:
    when (activeContextMenu) {
        ContextMenuOption.UNITS -> {
            UnitsDialog(
                units = units,
                currencySymbol = config.currencySymbol,
                onDismiss = { viewModel.closeContextMenu() },
                onAddUnit = { name, symbol, price -> viewModel.addUnit(name, symbol, price) },
                onDeleteUnit = { unit -> viewModel.deleteUnit(unit) },
                onSetDefaultUnit = { id -> viewModel.setDefaultUnit(id) },
                onUpdatePrice = { id, price -> viewModel.updateUnitManualPrice(id, price) }
            )
        }
        ContextMenuOption.STATUS -> {
            StatusDialog(
                records = rawRecords,
                currencySymbol = config.currencySymbol,
                onDismiss = { viewModel.closeContextMenu() }
            )
        }
        ContextMenuOption.DEFAULTS -> {
            DefaultsDialog(
                config = config,
                units = units,
                onDismiss = { viewModel.closeContextMenu() },
                onSaveDefaults = { updatedConfig ->
                    viewModel.updateConfig(updatedConfig)
                    viewModel.closeContextMenu()
                }
            )
        }
        ContextMenuOption.CONFIG -> {
            ConfigurationDialog(
                records = rawRecords,
                units = units,
                config = config,
                onDismiss = { viewModel.closeContextMenu() },
                onRestoreData = { recs, uns, cfg ->
                    viewModel.restoreAllData(recs, uns, cfg) {
                        viewModel.closeContextMenu()
                    }
                }
            )
        }
        null -> {
            // Nothing
        }
    }
}
