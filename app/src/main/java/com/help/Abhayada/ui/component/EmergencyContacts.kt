package com.help.Abhayada.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.help.Abhayada.R

var contacts = mutableListOf<Contact>()

fun getContacts(){
    contacts.clear()
    contacts.add(Contact("Maa", 9876543210))
    contacts.add(Contact("Papa", 8005320074))
    contacts.add(Contact("Bhaiya", 9876543210))
    contacts.add(Contact("Didi", 9876543210))
}

@Composable
fun EmergencyContactsSection(){
    getContacts()

    Text(text = "EMERGENCY CONTACTS", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(.9f).padding(bottom = 10.dp))
    LazyColumn {
        items(items = contacts){
            OutlinedCard(modifier = Modifier.fillMaxWidth(.9f).padding(vertical = 10.dp), shape = RoundedCornerShape(23.dp)) {
                Row(modifier = Modifier.padding(vertical = 18.dp, horizontal = 25.dp)){
                    Column {
                        Text(text = it.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "+91-"+it.number.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0x55000000))
                    }
                    Spacer(modifier = Modifier.fillMaxWidth(.8f))
                    IconButton(onClick = {}, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)) {
                        Icon(imageVector = ImageVector.vectorResource(R.drawable.edit_icon), contentDescription = "Edit Icon")
                    }
                }
            }
        }
    }
}