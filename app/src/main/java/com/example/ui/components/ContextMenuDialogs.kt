package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.example.util.JsonBackupUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.PriceApiService
import com.example.data.model.AppConfig
import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoseRed
import com.example.util.FormatUtils

/**
 * 1. "Birimler" Dialog:
 * Varlık/sembol seçeneklerini yönetme, güncel piyasa fiyatlarını görme ve elle/API ile güncelleme.
 */
@Composable
fun UnitsDialog(
    units: List<UnitItem>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddUnit: (name: String, symbol: String, price: Double) -> Unit,
    onDeleteUnit: (unit: UnitItem) -> Unit,
    onSetDefaultUnit: (id: Long) -> Unit,
    onUpdatePrice: (id: Long, price: Double) -> Unit
) {
    var newUnitName by remember { mutableStateOf("") }
    var newUnitSymbol by remember { mutableStateOf("") }
    var newUnitPriceText by remember { mutableStateOf("") }
    var isAddingUnit by remember { mutableStateOf(false) }

    var editingUnit by remember { mutableStateOf<UnitItem?>(null) }
    var editPriceText by remember { mutableStateOf("") }

    fun formatPrice(value: Double): String = FormatUtils.formatPrice(value)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ViewModule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Birimler & Varlıklar",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dialog_title_units")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Text(
                    text = "Likidasyon kayıtlarında varlık/sembol olarak kullanılır. Fiyatları listeden düzenleyebilirsiniz:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Yeni Ekle
                Button(
                    onClick = {
                        newUnitName = ""
                        newUnitSymbol = ""
                        newUnitPriceText = ""
                        isAddingUnit = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Ekle", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of existing units
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(units, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (item.isDefault) {
                                            Surface(
                                                color = EmeraldGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Varsayılan",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Fiyat: ${FormatUtils.prefixCurrency(formatPrice(item.currentPrice), currencySymbol)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Manual Price Edit Button
                                    IconButton(
                                        onClick = {
                                            editingUnit = item
                                            editPriceText = FormatUtils.formatPlain(item.currentPrice)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Fiyatı elle düzenle",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (!item.isDefault) {
                                        OutlinedButton(
                                            onClick = { onSetDefaultUnit(item.id) },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Seç", fontSize = 11.sp)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteUnit(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tamam")
            }
        }
    )

    // Add New Unit Modal Dialog (Manual price input)
    if (isAddingUnit) {
        AlertDialog(
            onDismissRequest = { isAddingUnit = false },
            title = { Text("Yeni Varlık / Birim Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Yeni varlığın sembolünü ve güncel fiyatını belirleyin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newUnitName,
                        onValueChange = { newUnitName = it },
                        label = { Text("Varlık / Sembol (Örn: DOT, AVAX, SOL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val curSuffix = FormatUtils.sanitizeCurrency(currencySymbol).let { if (it.isNotBlank()) " ($it)" else "" }
                    OutlinedTextField(
                        value = newUnitPriceText,
                        onValueChange = { newUnitPriceText = it },
                        label = { Text("Fiyat$curSuffix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUnitName.isNotBlank()) {
                            val price = newUnitPriceText.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val symbol = if (newUnitName.contains("/")) {
                                newUnitName.trim().uppercase()
                            } else {
                                "${newUnitName.trim().uppercase()}/USDT"
                            }
                            onAddUnit(newUnitName.trim().uppercase(), symbol, price)
                            newUnitName = ""
                            newUnitSymbol = ""
                            newUnitPriceText = ""
                            isAddingUnit = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isAddingUnit = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Manual Price Edit Mini Dialog
    editingUnit?.let { unitItem ->
        AlertDialog(
            onDismissRequest = { editingUnit = null },
            title = { Text("${unitItem.name} Fiyatını Düzenle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Güncel piyasa fiyatını belirleyin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val curSuffix = FormatUtils.sanitizeCurrency(currencySymbol).let { if (it.isNotBlank()) " ($it)" else "" }
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it },
                        label = { Text("Fiyat$curSuffix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = editPriceText.replace(',', '.').toDoubleOrNull()
                        if (p != null && p >= 0.0) {
                            onUpdatePrice(unitItem.id, p)
                        }
                        editingUnit = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingUnit = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

/**
 * 2. "Durum" Dialog:
 * Portfolio summary, total cost, active records, sold records, and net realized PnL.
 */
@Composable
fun StatusDialog(
    records: List<LiquidationRecord>,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val activeRecords = records.filter { !it.isSold }
    val soldRecords = records.filter { it.isSold }

    val totalActiveCost = activeRecords.sumOf { it.totalCost() }
    val totalSoldCost = soldRecords.sumOf { it.totalCost() }
    val totalRealizedPnl = soldRecords.sumOf { it.salePnl ?: 0.0 }
    val isProfit = totalRealizedPnl >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Durum & Özet",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dialog_title_status")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Overview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Kayıt Özeti",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aktif Kayıtlar:", style = MaterialTheme.typography.bodySmall)
                            Text("${activeRecords.size} adet", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Satılan / Kapatılanlar:", style = MaterialTheme.typography.bodySmall)
                            Text("${soldRecords.size} adet", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aktif Toplam Tutar:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                FormatUtils.prefixCurrency(FormatUtils.formatCurrency(totalActiveCost), currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Profit / Loss Summary Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Satış Kâr / Zarar Durumu",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) EmeraldGreen else RoseRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Satılanların Maliyeti:", style = MaterialTheme.typography.bodySmall)
                            Text(FormatUtils.prefixCurrency(FormatUtils.formatCurrency(totalSoldCost), currencySymbol), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gerçekleşen Net Kâr/Zarar:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${if (isProfit) "+" else ""}${FormatUtils.prefixCurrency(FormatUtils.formatCurrency(totalRealizedPnl), currencySymbol)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) EmeraldGreen else RoseRed
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tamam")
            }
        }
    )
}

/**
 * 3. "Varsayılanlar" Dialog:
 * Default unit and preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultsDialog(
    config: AppConfig,
    units: List<UnitItem>,
    onDismiss: () -> Unit,
    onSaveDefaults: (AppConfig) -> Unit
) {
    // Açılışta öncelikle varsayılan birim (isDefault == true) seçilsin
    val defaultUnitName = remember(units, config) {
        units.find { it.isDefault }?.name
            ?: config.defaultUnit.takeIf { sym -> units.any { it.name == sym } }
            ?: units.firstOrNull()?.name
            ?: ""
    }
    var selectedUnit by remember(defaultUnitName) { mutableStateOf(defaultUnitName) }
    var unitExpanded by remember { mutableStateOf(false) }
    var targetPctText by remember { mutableStateOf(FormatUtils.formatPercent(config.defaultTargetPercentage)) }
    var defaultAmountText by remember { mutableStateOf(FormatUtils.formatAmount(config.defaultAmount)) }

    LaunchedEffect(defaultUnitName) {
        if (selectedUnit.isBlank() || units.none { it.name == selectedUnit }) {
            selectedUnit = defaultUnitName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Varsayılanlar",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dialog_title_defaults")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Yeni kayıtlarda otomatik kullanılacak varsayılan birim, hedef satış yüzdesi ve miktar ayarları:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Default Unit
                Column {
                    Text(
                        "Varsayılan Birim",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Varsayılan birim seçin") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_default_unit")
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { unitItem ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(unitItem.name)
                                            if (unitItem.isDefault) {
                                                Text(
                                                    text = "Varsayılan",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedUnit = unitItem.name
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Default Amount
                Column {
                    Text(
                        "Varsayılan Miktar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = defaultAmountText,
                        onValueChange = { defaultAmountText = it },
                        placeholder = { Text("1.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_default_amount")
                    )
                }

                // Default Target Percentage (%)
                Column {
                    Text(
                        "Varsayılan Hedef Satış Yüzdesi (%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetPctText,
                        onValueChange = { targetPctText = it },
                        placeholder = { Text("1.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetPct = targetPctText.replace(',', '.').toDoubleOrNull() ?: 1.0
                    val defaultAmt = defaultAmountText.replace(',', '.').toDoubleOrNull() ?: 1.0
                    val finalSelectedUnit = selectedUnit.ifBlank { defaultUnitName }
                    onSaveDefaults(
                        config.copy(
                            defaultUnit = finalSelectedUnit,
                            defaultTargetPercentage = targetPct,
                            defaultAmount = defaultAmt
                        )
                    )
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

/**
 * 4. "Yapılandırma" Dialog:
 * Kullanıcı verilerini JSON formatında görüntüleme, düzenleme, panoya kopyalama ve geri yükleme (yedekleme).
 */
@Composable
fun ConfigurationDialog(
    records: List<LiquidationRecord>,
    units: List<UnitItem>,
    config: AppConfig,
    onDismiss: () -> Unit,
    onRestoreData: (records: List<LiquidationRecord>?, units: List<UnitItem>?, config: AppConfig?) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var jsonText by remember {
        mutableStateOf(JsonBackupUtils.exportToJson(records, units, config))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DataObject,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Yapılandırma (JSON)",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dialog_title_config")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tüm kullanıcı verileri (kayıtlar, birimler ve ayarlar) JSON olarak listelenmiştir. Buradan düzenleyebilir, kopyalayabilir veya yeni veri yapıştırıp geri yükleyebilirsiniz:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${records.size} Kayıt • ${units.size} Birim",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Format: JSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Bar: Kopyala, Yapıştır, Sıfırla
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonText))
                            statusMessage = "JSON panoya kopyalandı!"
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kopyala", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                jsonText = clip
                                statusMessage = "Panodaki JSON yapıştırıldı."
                                errorMessage = null
                            } else {
                                errorMessage = "Panoda metin bulunamadı."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yapıştır", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            jsonText = JsonBackupUtils.exportToJson(records, units, config)
                            statusMessage = "Güncel veriler yeniden yüklendi."
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sıfırla", fontSize = 11.sp)
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage!!,
                            color = EmeraldGreen,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // JSON Text Field (Monospace, scrollable, editable)
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMessage = null
                        statusMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .testTag("json_config_editor"),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("JSON verisini buraya yapıştırın veya düzenleyin...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = JsonBackupUtils.parseBackupJson(jsonText)
                    if (result.isSuccess) {
                        val backupData = result.getOrNull()!!
                        onRestoreData(backupData.records, backupData.units, backupData.config)
                        onDismiss()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Geçersiz JSON verisi."
                    }
                },
                modifier = Modifier.testTag("btn_restore_json")
            ) {
                Text("Geri Yükle & Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

