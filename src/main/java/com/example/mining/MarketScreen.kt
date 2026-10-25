@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mining

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MarketScreen(vm: MiningViewModel, nav: NavController) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Marketplace", fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(CATEGORIES) { c ->
                FilterChip(
                    selected = vm.category == c,
                    onClick = { vm.category = c },
                    label = { Text(c) }
                )
            }
        }
        val list = LISTINGS.filter { vm.category == "All" || it.category == vm.category }
        if (list.isEmpty()) Text("No listings in this category.", color = Color.Gray)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { i ->
                Card(onClick = { nav.navigate("detail/${i.id}") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            painterResource(i.image), i.title,
                            Modifier.size(84.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(i.title, fontWeight = FontWeight.SemiBold)
                            Text(i.sub, color = Color.Gray, fontSize = 13.sp)
                            Text("%,d USD".format(i.price), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(id: Int, vm: MiningViewModel, nav: NavController) {
    val i = LISTINGS.first { it.id == id }
    var msg by remember { mutableStateOf("Hello there, I am interested!") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back") }
        Image(
            painterResource(i.image), i.title,
            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(12.dp))
        Text(i.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(i.sub, color = Color.Gray)
        Text("%,d USD".format(i.price), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(i.seller, fontWeight = FontWeight.Bold)
                Text(i.info, color = Color.Gray, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(msg, { msg = it }, modifier = Modifier.weight(1f))
            Button(onClick = {
                vm.send(id, msg)
                nav.navigate("chat/$id")
            }) { Text("Send") }
        }
    }
}