package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bibliaapp.view.viewmodel.HomeViewmodel
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import com.example.bibliaapp.view.viewmodel.VersesViewmodel

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomePreview(){

}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewmodel : HomeViewmodel,
    versesViewmodel: VersesViewmodel,
    mainViewmodel: MainViewmodel,
    navigation : NavHostController
){

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(true)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }

    homeViewmodel.OnGet()
    HomeCard(versesViewmodel)
}


@Composable
fun HomeCard(versesViewmodel: VersesViewmodel){
    // Scroll state for the scrollable element
    val versesDaily by versesViewmodel.verseOfTheDay.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState))
    {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                ,
                elevation = CardDefaults.cardElevation(5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
            )
            {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Versiculo del Dia", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = versesDaily.verse_direction, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Spacer(modifier = Modifier.padding(16.dp))
                    Text(text = versesDaily.verse_content)
                }

            }
            Spacer(modifier = Modifier.padding(8.dp))
            ListVersesToday()
        }

    }

}


@Composable
fun ListVersesToday(){
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(5.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
        Column(modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Imagenes del Dia", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share")
                }
            }
            Box(modifier = Modifier.fillMaxWidth()){
                LazyRow(modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)) {

                }
            }
        }
    }
}

@Composable
fun ItemImageVerse(){

}