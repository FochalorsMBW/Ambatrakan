package com.ambatrakan.sigma.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ambatrakan.sigma.AmbatrakanApplication
import com.ambatrakan.sigma.ui.screens.DashboardScreen
import com.ambatrakan.sigma.ui.screens.HistoryScreen
import com.ambatrakan.sigma.ui.screens.ProfileSetupScreen
import com.ambatrakan.sigma.ui.screens.SettingsScreen
import com.ambatrakan.sigma.ui.viewmodel.DashboardViewModel
import com.ambatrakan.sigma.ui.viewmodel.HistoryViewModel
import com.ambatrakan.sigma.ui.viewmodel.SettingsViewModel
import com.ambatrakan.sigma.ui.viewmodel.ViewModelFactory
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Tab {
    Dashboard,
    History,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as AmbatrakanApplication
    val repository = app.repository

    val factory = remember(repository) { ViewModelFactory(repository) }

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val currentTenantId by repository.currentTenantId.collectAsState(initial = null)
    var currentTab by remember { mutableStateOf(Tab.Dashboard) }

    val notifications by repository.notifications.collectAsState(initial = emptyList())
    var showNotifications by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (currentTenantId == null) {
        ProfileSetupScreen(
            repository = repository,
            onSetupComplete = {}
        )
    } else {
        Scaffold(
            topBar = {
                if (currentTab != Tab.Settings) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ambatrakan",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            val unreadCount = notifications.count { !it.isRead && (it.tenantId == null || it.tenantId == currentTenantId) }
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                IconButton(
                                    onClick = { showNotifications = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifikasi",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = (-2).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                            color = MaterialTheme.colorScheme.onError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            bottomBar = {
                val selectedItem = when (currentTab) {
                    Tab.Dashboard -> 0
                    Tab.History -> 1
                    Tab.Settings -> 2
                }

                val items = listOf(
                    Triple(Tab.Dashboard, Icons.Default.Home, "Beranda"),
                    Triple(Tab.History, Icons.Default.DateRange, "Riwayat"),
                    Triple(Tab.Settings, Icons.Default.Person, "Profil")
                )

                CustomCurvedNavigationBar(
                    items = items,
                    selectedIndex = selectedItem,
                    onItemSelected = { currentTab = it },
                    modifier = Modifier.shadow(elevation = 16.dp, spotColor = Color.Black.copy(alpha = 0.1f))
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val slideDirection = if (targetState.ordinal > initialState.ordinal) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        
                        val animationSpec: FiniteAnimationSpec<androidx.compose.ui.unit.IntOffset> = spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )
                        val floatAnimationSpec: FiniteAnimationSpec<Float> = spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )

                        (slideIntoContainer(
                            towards = slideDirection,
                            animationSpec = animationSpec,
                            initialOffset = { it / 4 }
                        ) + fadeIn(animationSpec = floatAnimationSpec) + scaleIn(
                            initialScale = 0.95f,
                            animationSpec = floatAnimationSpec
                        )) togetherWith (slideOutOfContainer(
                            towards = slideDirection,
                            animationSpec = animationSpec,
                            targetOffset = { it / 4 }
                        ) + fadeOut(animationSpec = floatAnimationSpec) + scaleOut(
                            targetScale = 0.95f,
                            animationSpec = floatAnimationSpec
                        ))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        Tab.Dashboard -> DashboardScreen(viewModel = dashboardViewModel, contentPadding = innerPadding)
                        Tab.History -> HistoryScreen(viewModel = historyViewModel, contentPadding = innerPadding)
                        Tab.Settings -> SettingsScreen(viewModel = settingsViewModel, contentPadding = innerPadding)
                    }
                }
            }
        }

        if (showNotifications) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showNotifications = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pusat Notifikasi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (notifications.any { !it.isRead && (it.tenantId == null || it.tenantId == currentTenantId) }) {
                            TextButton(onClick = { repository.markAllNotificationsAsRead() }) {
                                Text("Tandai Dibaca")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val displayNotifications = notifications.filter { it.tenantId == null || it.tenantId == currentTenantId }.sortedByDescending { it.timestamp }
                    if (displayNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada notifikasi baru.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val formatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("in", "ID")) }
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(displayNotifications.size) { index ->
                                val notif = displayNotifications[index]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .clickable { repository.markNotificationAsRead(notif.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (notif.type == "ALERT") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = if (notif.type == "ALERT") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = notif.title,
                                                    fontWeight = if (!notif.isRead) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!notif.isRead) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = notif.message,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = formatter.format(Date(notif.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCurvedNavigationBar(
    items: List<Triple<Tab, androidx.compose.ui.graphics.vector.ImageVector, String>>,
    selectedIndex: Int,
    onItemSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.surface
    val fabColor = MaterialTheme.colorScheme.primary

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val itemWidth = screenWidth / items.size

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val fabOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex + (itemWidth / 2) - 28.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = ""
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp + bottomInset)
    ) {
        val cutoutXOffset by animateFloatAsState(
            targetValue = (selectedIndex + 0.5f) / items.size,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = ""
        )

        Canvas(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
            val width = size.width
            val height = size.height
            
            val path = Path().apply {
                moveTo(0f, 0f)
                
                val cutoutCenterX = width * cutoutXOffset
                val radius = 32.dp.toPx()
                val controlPointX = 24.dp.toPx()
                
                lineTo(cutoutCenterX - radius - controlPointX, 0f)
                
                cubicTo(
                    cutoutCenterX - radius, 0f,
                    cutoutCenterX - radius, radius,
                    cutoutCenterX, radius
                )
                
                cubicTo(
                    cutoutCenterX + radius, radius,
                    cutoutCenterX + radius, 0f,
                    cutoutCenterX + radius + controlPointX, 0f
                )
                
                lineTo(width, 0f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(path = path, color = barColor)
        }

        Box(
            modifier = Modifier
                .offset(x = fabOffset, y = 4.dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .background(fabColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val selectedItem = items[selectedIndex]
            Icon(
                imageVector = selectedItem.second,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = bottomInset)
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(item.first) },
                    contentAlignment = Alignment.Center
                ) {
                    val alpha by animateFloatAsState(targetValue = if (isSelected) 0f else 1f, label = "")
                    val yOffset by animateDpAsState(targetValue = if (isSelected) 20.dp else 0.dp, label = "")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(alpha).offset(y = yOffset)
                    ) {
                        Icon(
                            imageVector = item.second,
                            contentDescription = item.first.name,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.third,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
