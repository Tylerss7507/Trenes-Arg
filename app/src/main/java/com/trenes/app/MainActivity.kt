package com.trenes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaPrincipal()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    var estacionCercana by remember { mutableStateOf("Ezeiza (Detectada por GPS)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trenes en Vivo") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Estación cercana auto-detectada:", style = MaterialTheme.typography.labelMedium)
                        Text(estacionCercana, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Text("Próximas Salidas", style = MaterialTheme.typography.headlineSmall)

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Train, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text("Destino: Plaza Constitución", style = MaterialTheme.typography.bodyLarge)
                            Text("A horario", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text("4 min", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
