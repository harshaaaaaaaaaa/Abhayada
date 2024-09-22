package com.help.Abhayada.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.help.Abhayada.R
import com.help.Abhayada.hooks.useState
import com.help.Abhayada.ui.component.EmergencyContactsSection
import com.help.Abhayada.ui.component.PopUp
import kotlinx.coroutines.launch

import com.help.Abhayada.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFemale(){

    val (openAlert, setOpenAlert) = useState(false)
    val (isExpanded, setIsExpanded) = useState(false)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var user_name = "Harshita Kumari"
    var user_phoneNumber = "+91-9149358878"



    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(.8f)) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 50.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(user_name, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                            Text(user_phoneNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0x88000000))
                        }
                        Spacer(modifier = Modifier.fillMaxWidth(.45f))
                        Image(painter = painterResource(R.drawable.profile), contentDescription = "Profile Image", modifier = Modifier.size(50.dp).fillMaxWidth())
                    }
                    HorizontalDivider(modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(.2f).padding(vertical = 20.dp), thickness = 3.dp)
                    NavigationDrawerItem(label = {
                        Text("Home", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                    },
                        onClick = {},
                        selected = true)
                    NavigationDrawerItem(label = {
                        Text("About", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                    },
                        onClick = {},
                        selected = false)
                    NavigationDrawerItem(label = {
                        Text("Profile", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                    },
                        onClick = {},
                        selected = false)
                    NavigationDrawerItem(label = {
                        Text("Aadhar Verification", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                    },
                        onClick = {},
                        selected = false)
                    NavigationDrawerItem(label = {
                        Text("Hotspots Near You", fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                    },
                        onClick = {},
                        selected = false)
                    NavigationDrawerItem(label = {
                        Column {
                            Text(
                                "Featured",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Red,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    },
                        onClick = { setIsExpanded(!isExpanded) },
                        selected = false)
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(start = 40.dp)) {
                            TextButton(onClick = {}) {
                                Text("Self Defence", fontWeight = FontWeight.Medium, modifier = Modifier)
                            }
                            TextButton(onClick = {}) {
                                Text(
                                    "Youtube Training Sessions",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            TextButton(onClick = {}) {
                                Text("Things to Buy", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                TopAppBar(title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.absoluteOffset(x = (-20).dp)
                        ) {
                            Button(
                                onClick = { scope.launch { drawerState.open() } },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.menu),
                                    contentDescription = "Menu button icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    "I am ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Red
                                )
                                Text(
                                    text = "Abhayada",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        HorizontalDivider(thickness = (1.5).dp, modifier = Modifier.fillMaxWidth().offset(x = (-10).dp).padding(top = 2.dp))
                    }
                })
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = {}, shape = CircleShape, modifier = Modifier.height(200.dp).width(200.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xffFFE0E0))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "SEND", fontSize = 15.sp, color = Color.Red, modifier = Modifier.offset(y = 5.dp))
                        Text(text = "SOS", fontSize = 64.sp, color = Color.Red, fontWeight = FontWeight.Normal)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                OutlinedButton(onClick = { setOpenAlert(true) }, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp), modifier = Modifier.fillMaxWidth(.9f)) {
                    Text(text = "Your current location.", fontSize = 20.sp, modifier = Modifier)
                    Spacer(modifier = Modifier.fillMaxWidth(.7f))
                    Icon(imageVector = ImageVector.vectorResource(R.drawable.location), contentDescription = "Location Icon")
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth(.2f).padding(vertical = 20.dp), thickness = 3.dp)
                EmergencyContactsSection()
                PopUp(openAlert, setOpenAlert)
            }
        }
    }
}