package com.ambatrakan.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.ambatrakan.sigma.R
import com.ambatrakan.sigma.data.model.Location
import com.ambatrakan.sigma.data.model.Tenant
import com.ambatrakan.sigma.data.repository.AmbatrakanRepository
import com.ambatrakan.sigma.theme.*
import com.ambatrakan.sigma.ui.utils.BiometricHelper
import androidx.compose.ui.text.input.PasswordVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    repository: AmbatrakanRepository,
    onSetupComplete: () -> Unit
) {
    val locations by repository.locations.collectAsState(initial = emptyList())
    
    var selectedLocationId by remember { mutableStateOf<Long?>(null) }
    
    // Auto-select first location when loaded
    LaunchedEffect(locations) {
        if (selectedLocationId == null && locations.isNotEmpty()) {
            selectedLocationId = locations.first().id
        }
    }

    val activeLocation = locations.find { it.id == selectedLocationId }
    val allTenants by repository.getTenantsForLocation(selectedLocationId ?: -1).collectAsState(initial = emptyList())

    var activeTenantToLogin by remember { mutableStateOf<Tenant?>(null) }
    var inputPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    val context = LocalContext.current as? FragmentActivity

    if (activeTenantToLogin != null) {
        val tenant = activeTenantToLogin!!
        AlertDialog(
            onDismissRequest = { activeTenantToLogin = null; inputPassword = ""; loginError = false },
            title = { Text("Login: ${tenant.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan kata sandi untuk akun ini:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it; loginError = false },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = loginError,
                        placeholder = { Text("Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loginError) {
                        Text("Kata sandi salah!", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (inputPassword == tenant.password) {
                        val success = repository.selectCurrentTenant(tenant.id)
                        if (success) {
                            activeTenantToLogin = null
                            inputPassword = ""
                            onSetupComplete()
                        }
                    } else {
                        loginError = true
                    }
                }) {
                    Text("Masuk")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTenantToLogin = null; inputPassword = ""; loginError = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon & Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(90.dp).clip(CircleShape)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Selamat Datang di Ambatrakan",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih profil Anda untuk menyesuaikan tampilan perangkat ini",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Location Selector Tabs/Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                locations.forEach { location ->
                    val isSelected = location.id == selectedLocationId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedLocationId = location.id }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = location.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Tenants List Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                if (allTenants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Memuat data...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "PILIH NAMA ANDA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(allTenants) { tenant ->
                            val isAdmin = tenant.role == "ADMIN"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Transparent)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                .clickable {
                                    activeTenantToLogin = tenant
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tenant.initials,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tenant.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
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
