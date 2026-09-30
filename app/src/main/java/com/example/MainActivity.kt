package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ErpDashboardScreen
import com.example.ui.ErpViewModel
import com.example.ui.FinanceAndCostingScreen
import com.example.ui.InventoryAndChallanScreen
import com.example.ui.KarigarAndSpeedBoosterScreen
import com.example.ui.QualityControlScreen
import com.example.ui.RbacAndAiBlueprintScreen
import com.example.ui.ReportingAndWhatsAppHubScreen
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CbCreationsErpApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbCreationsErpApp(erpViewModel: ErpViewModel = viewModel()) {
    val companyProfile by erpViewModel.companyProfile.collectAsStateWithLifecycle()
    val materials by erpViewModel.materials.collectAsStateWithLifecycle()
    val karigars by erpViewModel.karigars.collectAsStateWithLifecycle()
    val challans by erpViewModel.challans.collectAsStateWithLifecycle()
    val qcInspections by erpViewModel.qcInspections.collectAsStateWithLifecycle()
    val vouchers by erpViewModel.vouchers.collectAsStateWithLifecycle()
    val auditLogs by erpViewModel.auditLogs.collectAsStateWithLifecycle()
    val rbacPermissions by erpViewModel.rbacPermissions.collectAsStateWithLifecycle()
    val manualTimeLogs by erpViewModel.manualTimeLogs.collectAsStateWithLifecycle()
    val activeRole by erpViewModel.activeRole.collectAsStateWithLifecycle()
    val isHindi by erpViewModel.isHindiMode.collectAsStateWithLifecycle()
    val useWhatsAppWebMode by erpViewModel.useWhatsAppWebMode.collectAsStateWithLifecycle()
    val searchQuery by erpViewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val costingInput by erpViewModel.costingInput.collectAsStateWithLifecycle()
    val aiResponseText by erpViewModel.aiResponseText.collectAsStateWithLifecycle()
    val isAiLoading by erpViewModel.isAiLoading.collectAsStateWithLifecycle()
    val bannerMessage by erpViewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isReadOnlyViewer = activeRole == "Viewer (Read-Only)"

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            erpViewModel.clearBanner()
        }
    }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = companyProfile.firmName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "BECOOPER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Role: $activeRole • ${if (useWhatsAppWebMode) "WA Web" else "Phone WA"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReadOnlyViewer) CrimsonDefectAlert
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        // Global Search Toggle
                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.testTag("btn_global_search_toggle")
                        ) {
                            Icon(
                                if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Global Search"
                            )
                        }

                        // Hindi / English Toggle
                        IconButton(
                            onClick = { erpViewModel.toggleHindiMode() },
                            modifier = Modifier.testTag("btn_toggle_language")
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Toggle Hindi/English")
                        }

                        // Costing, Day Book & GST E-Invoice Quick Button
                        IconButton(
                            onClick = { currentTab = 6 },
                            modifier = Modifier.testTag("btn_top_cost_gst")
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = "Job Costing, Accounts & GST",
                                tint = if (currentTab == 6) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // AI Copilot, RBAC Matrix & Blueprints Quick Button
                        IconButton(
                            onClick = { currentTab = 5 },
                            modifier = Modifier.testTag("btn_top_ai_rbac")
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Copilot, RBAC & Blueprints",
                                tint = if (currentTab == 5) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Active RBAC Role Switcher (Includes Read-Only Viewer)
                        Box {
                            IconButton(
                                onClick = { showRoleMenu = true },
                                modifier = Modifier.testTag("btn_role_switcher")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = "Switch RBAC Role")
                            }
                            DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                listOf(
                                    "Viewer (Read-Only)",
                                    "Administrator",
                                    "Production Manager",
                                    "Inventory Clerk",
                                    "Karigar (Worker)",
                                    "Brand Representative"
                                ).forEach { role ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (role == activeRole) "✓ $role" else role,
                                                fontWeight = if (role == activeRole) FontWeight.Bold else FontWeight.Normal,
                                                color = if (role == "Viewer (Read-Only)") CrimsonDefectAlert
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            erpViewModel.setActiveRole(role)
                                            showRoleMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Visible Read-Only Banner whenever "Viewer (Read-Only)" is active
                AnimatedVisibility(visible = isReadOnlyViewer) {
                    Surface(
                        color = CrimsonDefectAlert,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_readonly_mode")
                    ) {
                        Text(
                            text = if (isHindi) {
                                "🔒 READ-ONLY USER ACTIVE (Viewer): यह यूज़र कुछ भी एडिट, ऐड या डिलीट नहीं कर सकता।"
                            } else {
                                "🔒 READ-ONLY USER ACTIVE (Viewer): All Add, Edit, and Delete actions are strictly disabled."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { erpViewModel.updateGlobalSearch(it) },
                        label = {
                            Text(
                                if (isHindi) "ग्लोबल सर्च: जॉब ऑर्डर, चालान, कारीगर, लॉट, वाउचर..."
                                else "Global Search: Lot, Challan, Karigar, SKU, Voucher..."
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("input_global_search")
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("erp_bottom_nav")) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Main Dashboard") },
                    label = { Text(if (isHindi) "डैशबोर्ड" else "Dashboard") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Challans & Stock") },
                    label = { Text(if (isHindi) "चालान/स्टॉक" else "Challans") },
                    modifier = Modifier.testTag("nav_tab_challans")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = "Karigars & Speed") },
                    label = { Text(if (isHindi) "कारीगर" else "Karigars") },
                    modifier = Modifier.testTag("nav_tab_karigars")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = "Quality Control") },
                    label = { Text(if (isHindi) "क्वालिटी QC" else "QC Engine") },
                    modifier = Modifier.testTag("nav_tab_qc")
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reporting & WhatsApp") },
                    label = { Text(if (isHindi) "रिपोर्ट/WA" else "Reports & WA") },
                    modifier = Modifier.testTag("nav_tab_reports_wa")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val canCreateInventory = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("INVENTORY", "CREATE")
            }
            val canEditInventory = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("INVENTORY", "EDIT")
            }
            val canDeleteInventory = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("INVENTORY", "DELETE")
            }
            val canCreateJob = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("JOB_WORK", "CREATE")
            }
            val canEditJob = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("JOB_WORK", "EDIT")
            }
            val canDeleteJob = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("JOB_WORK", "DELETE")
            }
            val canWhatsAppJob = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("JOB_WORK", "WHATSAPP")
            }
            val canCreateQc = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("PRODUCTION_QC", "CREATE")
            }
            val canDeleteQc = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("PRODUCTION_QC", "DELETE")
            }
            val canCreateFinance = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("FINANCE", "CREATE")
            }
            val canEditFinance = remember(activeRole, rbacPermissions) {
                erpViewModel.hasPermission("FINANCE", "EDIT")
            }

            when (currentTab) {
                0 -> ErpDashboardScreen(
                    companyProfile = companyProfile,
                    materials = materials,
                    challans = challans,
                    karigars = karigars,
                    qcInspections = qcInspections,
                    vouchers = vouchers,
                    activeRole = activeRole,
                    isHindi = isHindi,
                    searchQuery = searchQuery,
                    canWhatsApp = canWhatsAppJob,
                    useWhatsAppWebMode = useWhatsAppWebMode,
                    onToggleKarigarPunch = erpViewModel::toggleKarigarPunch,
                    onNavigateToTab = { targetTab -> currentTab = targetTab },
                    onShowMessage = erpViewModel::showBanner
                )
                1 -> InventoryAndChallanScreen(
                    companyProfile = companyProfile,
                    materials = materials,
                    challans = challans,
                    karigars = karigars,
                    activeRole = activeRole,
                    isHindi = isHindi,
                    searchQuery = searchQuery,
                    canCreateInventory = canCreateInventory,
                    canEditInventory = canEditInventory,
                    canDeleteInventory = canDeleteInventory,
                    canCreateChallan = canCreateJob,
                    canEditChallan = canEditJob,
                    canDeleteChallan = canDeleteJob,
                    canDispatchWhatsApp = canWhatsAppJob,
                    onSaveMaterial = erpViewModel::saveMaterial,
                    onDeleteMaterial = erpViewModel::deleteMaterial,
                    onSaveChallan = erpViewModel::saveChallan,
                    onDeleteChallan = erpViewModel::deleteChallan,
                    onShowMessage = erpViewModel::showBanner
                )
                2 -> KarigarAndSpeedBoosterScreen(
                    karigars = karigars,
                    challans = challans,
                    manualTimeLogs = manualTimeLogs,
                    activeRole = activeRole,
                    isHindi = isHindi,
                    searchQuery = searchQuery,
                    useWhatsAppWebMode = useWhatsAppWebMode,
                    canCreate = canCreateJob,
                    canEdit = canEditJob,
                    canDelete = canDeleteJob,
                    canWhatsApp = canWhatsAppJob,
                    onSaveKarigar = erpViewModel::saveKarigar,
                    onSaveManualTimeLog = erpViewModel::saveManualTimeLog,
                    onDeleteManualTimeLog = erpViewModel::deleteManualTimeLog,
                    onShowMessage = erpViewModel::showBanner
                )
                3 -> QualityControlScreen(
                    qcInspections = qcInspections,
                    challans = challans,
                    isHindi = isHindi,
                    searchQuery = searchQuery,
                    canCreateQc = canCreateQc,
                    canDeleteQc = canDeleteQc,
                    canWhatsApp = canWhatsAppJob,
                    onRecordQc = erpViewModel::recordQcInspection,
                    onDeleteQc = erpViewModel::deleteQcInspection,
                    onShowMessage = erpViewModel::showBanner
                )
                4 -> ReportingAndWhatsAppHubScreen(
                    companyProfile = companyProfile,
                    materials = materials,
                    challans = challans,
                    karigars = karigars,
                    manualTimeLogs = manualTimeLogs,
                    qcInspections = qcInspections,
                    vouchers = vouchers,
                    costingInput = costingInput,
                    activeRole = activeRole,
                    isHindi = isHindi,
                    useWhatsAppWebMode = useWhatsAppWebMode,
                    isReadOnlyViewer = isReadOnlyViewer,
                    onSetWhatsAppWebMode = erpViewModel::setWhatsAppWebMode,
                    onSwitchToReadOnlyViewer = {
                        if (isReadOnlyViewer) erpViewModel.setActiveRole("Administrator")
                        else erpViewModel.setActiveRole("Viewer (Read-Only)")
                    },
                    onClearAllData = erpViewModel::clearAllTestData,
                    onShowMessage = erpViewModel::showBanner
                )
                5 -> RbacAndAiBlueprintScreen(
                    companyProfile = companyProfile,
                    activeRole = activeRole,
                    isHindi = isHindi,
                    rbacPermissions = rbacPermissions,
                    auditLogs = auditLogs,
                    aiResponseText = aiResponseText,
                    isAiLoading = isAiLoading,
                    onSwitchRole = erpViewModel::setActiveRole,
                    onToggleRbac = erpViewModel::toggleRbacPermission,
                    onUpdateCompanyProfile = erpViewModel::updateCompanyProfile,
                    onAskAi = erpViewModel::runAiErpAnalysis,
                    onScanChallanBitmap = erpViewModel::runChallanImageOcr,
                    onShowMessage = erpViewModel::showBanner
                )
                else -> FinanceAndCostingScreen(
                    companyProfile = companyProfile,
                    costingInput = costingInput,
                    vouchers = vouchers,
                    isHindi = isHindi,
                    searchQuery = searchQuery,
                    canCreateFinance = canCreateFinance,
                    canEditFinance = canEditFinance,
                    canWhatsApp = canWhatsAppJob,
                    onUpdateCosting = erpViewModel::updateCostingInput,
                    onSaveVoucher = erpViewModel::saveVoucher,
                    onShowMessage = erpViewModel::showBanner
                )
            }
        }
    }
}
