@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mining

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private class Service(val label: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun ProfileScreen(vm: MiningViewModel, nav: NavController) {
    val services = listOf(
        Service("Marketplace", Icons.Default.ShoppingCart) { vm.category = "All"; nav.goTab("market") },
        Service("Jobs", Icons.Default.Work) { nav.navigate("soon/Jobs") },
        Service("Car rentals", Icons.Default.DirectionsCar) { vm.category = "Car rentals"; nav.goTab("market") },
        Service("Maps", Icons.Default.LocationOn) { nav.goTab("map") },
        Service("Legal", Icons.Default.Gavel) { nav.goTab("legal") },
        Service("Prospecting", Icons.Default.Construction) { nav.navigate("soon/Prospecting") }
    )
    val loc = vm.userLocation?.let { "%.4f, %.4f".format(it.latitude, it.longitude) } ?: "Not available yet"

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(Teal, CircleShape), contentAlignment = Alignment.Center) {
                Text(
                    vm.userName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(vm.userName, fontWeight = FontWeight.Bold)
                Text(vm.userEmail, color = Color.Gray, fontSize = 13.sp)
                Text(vm.userPhone, color = Color.Gray, fontSize = 13.sp)
                Text("Location: $loc", color = Color.Gray, fontSize = 13.sp)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Services", fontWeight = FontWeight.Bold)
                services.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { s ->
                            Card(onClick = s.action, modifier = Modifier.weight(1f)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(s.icon, null, tint = Teal)
                                    Spacer(Modifier.width(8.dp))
                                    Text(s.label, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                vm.logout()
                nav.navigate("welcome") { popUpTo(nav.graph.id) { inclusive = true } }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Log out") }
    }
}

@Composable
fun SoonScreen(title: String, nav: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("$title is coming soon.", color = Color.Gray)
    }
}