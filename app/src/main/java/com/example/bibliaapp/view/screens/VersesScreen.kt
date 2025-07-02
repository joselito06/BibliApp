package com.example.bibliaapp.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bibliaapp.data.networking.models.VersesDaily
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import com.example.bibliaapp.view.viewmodel.VersesViewmodel

@Composable
@Preview()
fun VersesScreenPreview(){
    //VersesScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersesScreen(modifier: Modifier, versesViewmodel: VersesViewmodel, navigation : NavHostController, scrollBehavior: TopAppBarScrollBehavior){
    versesViewmodel.onGet()
    val list: List<VersesDaily> by versesViewmodel.getAllVerses.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Column(modifier = Modifier
        .fillMaxSize()) {
        LazyColumn(
            //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            itemsIndexed(list){ index, item ->
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)){
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp), elevation = CardDefaults.elevatedCardElevation(5.dp)) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)) {
                            Text(
                                text = item.verse_direction,
                                fontStyle = FontStyle.Normal,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.padding(10.dp))
                            Text(text = item.verse_content)
                            Row {
                                IconButton(onClick = { /*TODO*/ }) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Compartir")
                                }
                                IconButton(onClick = { /*TODO*/ }) {
                                    Icon(imageVector = Icons.Filled.CopyAll, contentDescription = "Copiar")
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}