package com.help.Abhayada.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import  androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PopUp(openAlert: Boolean, setOpenAlert: (Boolean) -> Unit){
    when{
        openAlert ->{
            AlertDialog(
                icon = { Icon(Icons.Rounded.Warning, contentDescription = "Alert Icon", modifier = Modifier.size(60.dp), tint = Color.Red) },
                title = {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Row {
                            Text("You are entering an ", fontSize = 16.sp)
                            Text(
                                "Isolated",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(" area.", fontSize = 16.sp)
                        }
                        Text("You may face Network Issues", fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0x33ff0000), shape = CircleShape).wrapContentHeight(align = Alignment.CenterVertically), color = Color.Red)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Text("Things to do:", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 20.dp))
                        Text("1. Take out your Pepper spray.", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("2. Keep your phone handy.", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 10.dp))
                        Text("3. Stay on call with your Guardians.", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                onDismissRequest = { setOpenAlert(false) },
                confirmButton = {
                    TextButton(onClick = { setOpenAlert(false) }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { setOpenAlert(false) }) {
                        Text("Dismiss")
                    }
                },
            )
        }
    }
}