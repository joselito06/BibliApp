package com.example.bibliaapp.view.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bibliaapp.SharedPreferenceBible
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.Verses
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(modifier: Modifier, bibleViewmodel: BibleViewmodel, navigation : NavHostController, scrollBehavior: TopAppBarScrollBehavior){
    bibleViewmodel.onGet()
    val context = LocalContext.current
    val list: List<Books> by bibleViewmodel.getAllBiblia.observeAsState(initial = listOf())
    val libro: String by bibleViewmodel.libro.observeAsState("Gen")
    val numCapitulo: Int by bibleViewmodel.capitulo.observeAsState(initial = 1)
    val shared: SharedPreferenceBible by bibleViewmodel.sharedComp.observeAsState(initial = SharedPreferenceBible(context = context))

    var currentBooks by remember { mutableStateOf(shared.GetCurrentBook())}
    var currentIndex by remember { mutableIntStateOf(shared.GetCurrentIndex(libro)) }
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var pausedText by remember { mutableStateOf("") }

    //var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val selectedItems = remember { mutableStateOf(setOf<Int>()) }
    //val clipboardManager = LocalClipboardManager.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    var searchLetter by remember { mutableStateOf("") }

    fun toggleSelection(index: Int) {
        val currentSelection = selectedItems.value.toMutableSet()
        if (currentSelection.contains(index)) {
            currentSelection.remove(index)
        } else {
            currentSelection.add(index)
        }
        selectedItems.value = currentSelection
    }

    fun copySelectedText(items: List<Verses>) {
        val selectedText = selectedItems.value.map { items[it] }.joinToString("\n")
        val clip = ClipData.newPlainText("selected text", selectedText)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    fun shareSelectedText(items: List<Verses>) {
        val selectedText = selectedItems.value.map { items[it] }.joinToString("\n")
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, selectedText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir texto con"))
    }

    fun scrollToLetter(letter: String, items: List<Verses>) {
        var index: Int = 0
        try {
            index = items.indexOfFirst { it.ChapterNum.toString() == letter }
        }catch (e: Exception){}

        if (index >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(index)
            }
        } else {
            Toast.makeText(context, "No se encontró un elemento que comience con la letra $letter", Toast.LENGTH_SHORT).show()
        }


    }

    LaunchedEffect(key1 = context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Idioma no soportado", Toast.LENGTH_SHORT).show()
                } else {
                    val voices = tts?.voices
                    val preferredVoice = voices?.find { it.name.contains("es-es-x-") }
                    if (preferredVoice != null) {
                        tts?.voice = preferredVoice
                        Toast.makeText(context, "TextToSpeech initialized with preferred voice", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Preferred voice not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                currentIndex = utteranceId?.toInt() ?: 0
                coroutineScope.launch {
                    listState.animateScrollToItem(currentIndex)
                }
            }

            override fun onDone(utteranceId: String?) {
                val nextIndex = (utteranceId?.toInt() ?: 0) + 1
                if (nextIndex < list.first().Verse.size) {
                    tts?.speak(list.first().Verse[nextIndex].Contenido, TextToSpeech.QUEUE_FLUSH, null, nextIndex.toString())
                } else {
                    currentIndex = 0
                    isSpeaking = false
                }
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    DisposableEffect(key1 = currentBooks) {
        shared.SaveCurrentBook(libro)
        shared.SaveCurrentIndex(currentBooks!!,currentIndex)
        onDispose {
            currentIndex = shared.GetCurrentIndex(currentBooks)
        }
    }

    BottomSheetScaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary),
        scaffoldState = scaffoldState,
        //sheetContainerColor = BottomSheetDefaults.ContainerColor.apply { MaterialTheme.colorScheme.onPrimary },
        //containerColor = MaterialTheme.colorScheme.onPrimary,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically)
                {
                    Button(
                        onClick = {
                            copySelectedText(list.first { v -> v.Osis == libro }.Verse)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary),
                        elevation = ButtonDefaults.elevatedButtonElevation(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CopyAll, contentDescription = "")
                        Spacer(modifier = Modifier.padding(end = 8.dp))
                        Text("Copiar")
                    }

                    Button(
                        onClick = {
                            shareSelectedText(list.first { v -> v.Osis == libro }.Verse)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary),
                        elevation = ButtonDefaults.elevatedButtonElevation(10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = "")
                        Spacer(modifier = Modifier.padding(end = 8.dp))
                        Text("Compartir")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

            }
        },
        sheetPeekHeight = 0.dp
    ){ innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ){

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp),
                state = listState){

                list.forEach{
                    stickyHeader {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(
                                bottomStart = 50.dp,
                                bottomEnd = 50.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ){
                            /*Button(onClick = {
                                selectedIndex?.let {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(it)
                                    }
                                }
                            }) {
                                Text("Scroll to selected item")
                            }*/
                            Row(
                                Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    bibleViewmodel.lessBook()
                                    shared.SaveCurrentBook(libro)
                                    currentBooks = shared.GetCurrentBook()
                                    currentIndex = shared.GetCurrentIndex(currentBooks)
                                }) {
                                    Icon(imageVector = Icons.Rounded.ArrowBackIos, contentDescription = null)
                                }
                                Text(text = it.Human, style = MaterialTheme.typography.displaySmall)
                                IconButton(onClick = {
                                    bibleViewmodel.plusBook()
                                    shared.SaveCurrentBook(libro)
                                    currentBooks = shared.GetCurrentBook()
                                    currentIndex = shared.GetCurrentIndex(currentBooks)
                                }) {
                                    Icon(imageVector = Icons.Rounded.ArrowForwardIos, contentDescription = null)
                                }
                            }
                        }
                    }
                    itemsIndexed((it.Verse)){ index, item ->
                        //val isSelected = selectedIndex == index
                        val isSelected = selectedItems.value.contains(index)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                //.height(80.dp)
                                .padding(8.dp)
                        ){
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (item.ChapterNum != null){
                                    Text(
                                        text = item.Chapter.toString(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .clickable {
                                                //toggleSelection(index)
                                                //selectedIndex = index
                                            },
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.displaySmall
                                    )

                                }

                                if (!item.ChapterName.isNullOrEmpty()){
                                    Text(text = item.ChapterName, modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        //.background(if (isSelected) Color.Yellow else Color.Transparent)
                                        .clickable {
                                            toggleSelection(index)
                                            //selectedIndex = index
                                            coroutineScope.launch {
                                                bottomSheetState.show()
                                            }
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        fontSize = 20.sp)
                                }
                                Row(
                                    Modifier
                                        //.height(100.dp)
                                        .fillMaxWidth()) {
                                    Box(modifier = Modifier
                                        //.background(Color.Blue)
                                        .fillMaxHeight(),
                                        contentAlignment = Alignment.Center

                                    ){
                                        Text(modifier = Modifier
                                            .padding(8.dp),
                                            text = "${item.Verse} ",
                                            fontSize = 12.sp)
                                    }
                                    Column(modifier = Modifier.padding(8.dp)){
                                        Text(text = "${item.Contenido}",
                                            modifier = Modifier
                                                .clickable {
                                                    toggleSelection(index)
                                                    //selectedIndex = index
                                                    coroutineScope.launch {
                                                        bottomSheetState.show()
                                                    }
                                                },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            )
                                    }

                                }
                            }

                        }
                    }
//                items(it.Verse){
//                    Item(it)
//                }
                }

            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                elevation = CardDefaults.cardElevation(16.dp),
                shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
            ) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ElevatedCard(modifier = Modifier
                        .weight(3f)
                        .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(50.dp)
                    ){
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = {
                                    bibleViewmodel.decrementCapitulo()
                                    searchLetter = numCapitulo.toString()
                                    try {
                                        scrollToLetter(searchLetter,list.first {v -> v.Osis == libro}.Verse)
                                    }catch (e: Exception){}

                                },
                                modifier = Modifier.padding(2.dp)) {
                                Icon(imageVector = Icons.Rounded.ArrowBackIos, contentDescription = null)
                            }
                            TextButton(onClick = {  }, modifier = Modifier.padding(2.dp)) {
                                Text(text = "Capitulo $numCapitulo")
                            }
                            TextButton(
                                onClick = {
                                    bibleViewmodel.aumentarCapitulo()
                                    searchLetter = numCapitulo.toString()
                                    try {
                                        scrollToLetter(searchLetter,list.first {v -> v.Osis == libro}.Verse)
                                    }
                                    catch (e: Exception){}

                                    //bibleViewModel.getByCapitulo()

                                },
                                modifier = Modifier.padding(2.dp)) {
                                Icon(imageVector = Icons.Rounded.ArrowForwardIos, contentDescription = null)
                            }
                        }

                    }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        //if(list.any()) SpeakButton(items = list.first().Verse)

                        Row {
                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        tts?.stop()
                                        isSpeaking = false
                                        isPaused = false
                                    } else {
                                        if (isPaused) {
                                            tts?.speak(pausedText, TextToSpeech.QUEUE_FLUSH, null, currentIndex.toString())
                                            isPaused = false
                                        } else {
                                            tts?.speak(list.first().Verse[currentIndex].Contenido, TextToSpeech.QUEUE_FLUSH, null, currentIndex.toString())
                                        }
                                        isSpeaking = true
                                    }
                                }
                            ) {
                                Icon(imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        tts?.stop()
                                        pausedText = list.first().Verse[currentIndex].Contenido!!
                                        isSpeaking = false
                                        isPaused = true
                                    }
                                },
                                enabled = isSpeaking
                            ) {
                                Icon(imageVector = Icons.Rounded.Pause, contentDescription = "pausa")
                            }
                        }
                    }

//                DisposableEffect(key1 = tts) {
//                    onDispose {
//                        tts?.shutdown()
//                    }
//                }

                    DisposableEffect(key1 = currentIndex) {
                        shared.SaveCurrentIndex(currentBooks!!, currentIndex)
                        onDispose {
                            //shared.SaveCurrentIndex(currentIndex)
                            shared.SaveCurrentBook(libro)
                            currentBooks = shared.GetCurrentBook()
                        }
                    }

                }
            }

        }
    }

}



/*@Composable
fun Item(verses: Verses, index: Int, selectedIndex: Int, isSelected: Boolean){

    Box(
        Modifier
            .fillMaxWidth()
            //.height(80.dp)
            .padding(8.dp)
    ){
        Column(modifier = Modifier.fillMaxWidth()) {
            if (verses.ChapterNum != null){
                Text(
                    text = verses.Chapter.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                        //.clickable { selectedIndex = index },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall
                )

            }

            if (!verses.ChapterName.isNullOrEmpty()){
                Text(text = verses.ChapterName!!, modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), textAlign = TextAlign.Center, fontSize = 20.sp)
            }
            Row(
                Modifier
                    //.height(100.dp)
                    .fillMaxWidth()) {
                Box(modifier = Modifier
                    //.background(Color.Blue)
                    .fillMaxHeight(),
                    contentAlignment = Alignment.Center

                ){
                    Text(modifier = Modifier.padding(8.dp), text = "${verses.Verse} ", fontSize = 12.sp)
                }
                Column(modifier = Modifier.padding(8.dp)){
                    Text(text = "${verses.Contenido}")
                }

            }
        }

    }
}*/

/*@Composable
fun SpeakButton(items: List<Verses>) {
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    LaunchedEffect(key1 = context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Idioma no soportado", Toast.LENGTH_SHORT).show()
                } else {
                    val voices = tts?.voices
                    val preferredVoice = voices?.find { it.name.contains("es-es-x-") }
                    if (preferredVoice != null) {
                        tts?.voice = preferredVoice
                        Toast.makeText(context, "TextToSpeech initialized with preferred voice", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Preferred voice not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Button(onClick = {
        items.forEach { item ->
            tts?.speak(item.Contenido, TextToSpeech.QUEUE_ADD, null, null)
        }
    }) {
        Text(text = "Leer en voz alta")
    }

    DisposableEffect(key1 = tts) {
        onDispose {
            tts?.shutdown()
        }
    }
}*/