@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mining

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun WelcomeScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painterResource(R.drawable.saso_logo), "Saso logo",
            Modifier.size(200.dp).clip(CircleShape), contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        Text("Welcome to Saso", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Your all in one Mining App")
        Text("Mauritania", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Button(onClick = { nav.navigate("consent") }, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Have an account?")
            TextButton(onClick = { nav.navigate("login") }) {
                Text("Log in", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConsentScreen(vm: MiningViewModel, nav: NavController) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) vm.fetchLocation()
        nav.navigate("register")
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Icon(Icons.Default.LocationOn, null, tint = Teal)
            Spacer(Modifier.width(8.dp))
            Text("Enable Location", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Consent is required to use Saso", fontWeight = FontWeight.Bold)
                Text("Saso collects basic information that helps in maintaining the integrity.")
                Text("The user should also be an adult over 18 years old.")
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirm") }
        TextButton(onClick = { nav.navigate("register") }, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now")
        }
    }
}

@Composable
fun RegisterScreen(vm: MiningViewModel, nav: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text("Identity Verification", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text("Enter your full name", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            name, { name = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Enter name") }, singleLine = true
        )
        Text("Enter your email", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Enter email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Text("Please enter your phone number", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            phone, { phone = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Enter phone number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                error = vm.register(name, email, phone)
                if (error == null) nav.navigate("login")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }
}

@Composable
fun LoginScreen(vm: MiningViewModel, nav: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text("Log in", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text("Full name", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            name, { name = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Enter name") }, singleLine = true
        )
        Text("Email", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            email, { email = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Enter email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (vm.login(name, email)) {
                    vm.fetchLocation()
                    nav.navigate("market") { popUpTo("welcome") { inclusive = true } }
                } else {
                    error = "Name and email don't match an account. Please register first."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Log in") }
        TextButton(onClick = { nav.navigate("consent") }, modifier = Modifier.fillMaxWidth()) {
            Text("No account? Register")
        }
    }
}