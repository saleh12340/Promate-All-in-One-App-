package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesListScreen
import com.example.ui.screens.InvoicingScreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SmartInvoicingTheme
import com.example.ui.viewmodel.AccountingViewModel

enum class AppScreen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("الرئيسة", Icons.Default.Dashboard),
    INVOICING("فاتورة", Icons.Default.AddShoppingCart),
    LEDGER("كشف حساب", Icons.Default.MenuBook),
    INVOICES("الفواتير", Icons.Default.ReceiptLong),
    CUSTOMERS("العملاء", Icons.Default.Groups)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: AccountingViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

    // Dialog state collectors
    val showReceiptDialog by viewModel.showReceiptDialog.collectAsState()
    val previewReceiptBitmap by viewModel.previewReceiptBitmap.collectAsState()
    val currentInvoiceForPrint by viewModel.currentInvoiceForPrint.collectAsState()
    val currentItemsForPrint by viewModel.currentItemsForPrint.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()
    val isTranscribing by viewModel.isTranscribing.collectAsState()

    // Handle back button on sub-screens to return to Dashboard
    if (currentScreen != AppScreen.DASHBOARD) {
        BackHandler {
            currentScreen = AppScreen.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentScreen.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                },
                actions = {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "ريال يمني (YER)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
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
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToInvoicing = { currentScreen = AppScreen.INVOICING },
                    onNavigateToLedger = { currentScreen = AppScreen.LEDGER },
                    onNavigateToInvoices = { currentScreen = AppScreen.INVOICES },
                    onNavigateToCustomers = { currentScreen = AppScreen.CUSTOMERS }
                )
                AppScreen.INVOICING -> InvoicingScreen(
                    viewModel = viewModel
                )
                AppScreen.LEDGER -> CustomerLedgerScreen(
                    viewModel = viewModel
                )
                AppScreen.INVOICES -> InvoicesListScreen(
                    viewModel = viewModel
                )
                AppScreen.CUSTOMERS -> CustomersListScreen(
                    viewModel = viewModel,
                    onSelectCustomerLedger = {
                        currentScreen = AppScreen.LEDGER
                    }
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
