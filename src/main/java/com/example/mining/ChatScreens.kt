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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ChatListScreen(vm: MiningViewModel, nav: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chats", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (vm.chats.isEmpty()) {
            Text("No conversations yet. Open a listing in Home and tap Send.", color = Color.Gray)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.chats.keys.toList()) { id ->
                val l = LISTINGS.first { it.id == id }
                Card(onClick = { nav.navigate("chat/$id") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).background(Teal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(l.seller.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.seller, fontWeight = FontWeight.Bold)
                            Text(
                                vm.chats[id]!!.last().text, color = Color.Gray, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (vm.unread[id] == true) Badge { Text("new") }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(id: Int, vm: MiningViewModel, nav: NavController) {
    val l = LISTINGS.first { it.id == id }
    var text by remember { mutableStateOf("") }

    DisposableEffect(id) {
        vm.openThread = id
        vm.unread[id] = false
        onDispose { vm.openThread = null }
    }

    val msgs = vm.chats[id].orEmpty()
    val state = rememberLazyListState()
    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) state.animateScrollToItem(msgs.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { nav.popBackStack() }) { Text("‹") }
            Text(l.seller, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            state = state,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(msgs) { m ->
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = if (m.mine) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        m.text,
                        color = if (m.mine) Color.White else Color.Black,
                        modifier = Modifier
                            .background(if (m.mine) Teal else Color(0xFFE6ECEC), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                text, { text = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Message about ${l.title}") }
            )
            Button(onClick = { vm.send(id, text); text = "" }) { Text("Send") }
        }
    }
}