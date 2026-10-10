package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.components.VoiceRecordingDialog
import com.example.ui.screens.CustomerLedgerScreen
import com.example.ui.screens.CustomersListScreen
import com.example.ui.screens.InvoicesListScreen
import com.example.ui.screens.InvoicingScreen
import com.example.ui.screens.ItemsCatalogScreen
import com.example.ui.screens.StoreProfileScreen
import com.example.ui.theme.DarkText
import com.example.ui.theme.LightBluePill
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SmartInvoicingTheme
import com.example.ui.viewmodel.AccountingViewModel

enum class AppScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    INVOICING("جديدة", Icons.Default.Description),
    LEDGER("السجل", Icons.Default.History),
    CATALOG("الأصناف", Icons.Default.Category),
    CUSTOMERS("العملاء", Icons.Default.People),
    PRINTING("الطباعة", Icons.Default.Print),
    STORE("المتجر", Icons.Default.Store)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AccountingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartInvoicingTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: AccountingViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.INVOICING) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val activityContext = androidx.compose.ui.platform.LocalContext.current

    // Dialog state collectors
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
    val previewReceiptBitmap by viewModel.previewReceiptBitmap.collectAsState()
    val currentInvoiceForPrint by viewModel.currentInvoiceForPrint.collectAsState()
    val currentItemsForPrint by viewModel.currentItemsForPrint.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()

    BackHandler {
        if (currentScreen != AppScreen.INVOICING) currentScreen = AppScreen.INVOICING
        else showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("تأكيد الخروج") },
            text = { Text("هل تريد الخروج من التطبيق؟") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    (activityContext as? android.app.Activity)?.finish()
                }) { Text("خروج") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("إلغاء") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Modern bottom bar matching screenshot (White with subtle elevation and rounded active pill)
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AppScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = LightBluePill
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.INVOICING -> InvoicingScreen(
                    viewModel = viewModel
                )
                AppScreen.LEDGER -> CustomerLedgerScreen(
                    viewModel = viewModel
                )
                AppScreen.CATALOG -> ItemsCatalogScreen(
                    viewModel = viewModel,
                    onItemSelected = { currentScreen = AppScreen.INVOICING }
                )
                AppScreen.CUSTOMERS -> CustomersListScreen(
                    viewModel = viewModel,
                    onSelectCustomerLedger = {
                        currentScreen = AppScreen.LEDGER
                    }
                )
                AppScreen.PRINTING -> InvoicesListScreen(
                    viewModel = viewModel
                )
                AppScreen.STORE -> StoreProfileScreen(
                    viewModel = viewModel
                )
            }

            // Thermal 58mm Receipt Preview Dialog
            if (showReceiptDialog) {
                ThermalReceiptDialog(
                    bitmap = previewReceiptBitmap,
                    invoice = currentInvoiceForPrint,
                    items = currentItemsForPrint,
                    storeName = storeName,
                    storePhone = storePhone,
                    onDismiss = { viewModel.showReceiptDialog.value = false }
                )
            }

            // Voice Recording & Gemini Transcription Modal
            VoiceRecordingDialog(
                isRecording = isRecording,
                isTranscribing = isTranscribing,
                onStopAndTranscribe = { viewModel.stopVoiceInputAndTranscribe() },
                onCancel = { viewModel.cancelVoiceInput() }
            )
        }
    }
}
