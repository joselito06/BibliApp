package com.example.bibliaapp.view.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bibliaapp.domain.model.Books
import com.example.bibliaapp.domain.model.Verses
import com.example.bibliaapp.view.viewmodel.BibleViewmodel
import com.example.bibliaapp.view.viewmodel.MainViewmodel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun BibleScreen(modifier: Modifier, bibleViewmodel: BibleViewmodel, mainViewmodel: MainViewmodel, navigation : NavHostController, scrollBehavior: TopAppBarScrollBehavior){

    val context = LocalContext.current
    val list: List<Books> by bibleViewmodel.getAllBiblia.collectAsState()
    val listBook by bibleViewmodel.getAllBook.collectAsState()
    val libro: String by bibleViewmodel.libro.collectAsState("Gen")
    val capitulo: Int by bibleViewmodel.capitulo.collectAsState()
    //val shared: SharedPreferenceBible by bibleViewmodel.sharedComp.collectAsState(initial = SharedPreferenceBible(context = context))

//    var tts: TextToSpeech? by remember { mutableStateOf(null) }
//    var isSpeaking by remember { mutableStateOf(false) }
//    var isPaused by remember { mutableStateOf(false) }
//    var pausedText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isScrollingDown by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableStateOf(0) }
    var previousOffset by remember { mutableStateOf(0) }
    var isUserScrolling by remember { mutableStateOf(false) }

    val selectedItems = remember { mutableStateOf(setOf<Int>()) }
    val selectedItemToSpeak = remember { mutableIntStateOf(-1) }
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
        selectedItemToSpeak.intValue = -1

        coroutineScope.launch {
            bottomSheetState.show()
        }
    }

    fun toggleSelectionToSpeak(index: Int) {
        var currentSelection = selectedItemToSpeak.intValue
        if (currentSelection == index) {
            currentSelection = -1
        } else {
            currentSelection = index
        }
        selectedItemToSpeak.intValue = currentSelection
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

    // Detectar eventos de scroll manual (táctil) usando `nestedScrollConnection`
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    isUserScrolling = true // Usuario deslizó con el dedo
                }else{
                    isUserScrolling = false;
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(true) {
        mainViewmodel.showTopAppBar(true)
        mainViewmodel.showBottomBar(true)

        //mainViewmodel.updateFabIcon(Icons.Default.Add)
        //mainViewmodel.showFloatingActionButton(false)
    }

    // LaunchedEffect para detectar el scroll y actualizar capítulo
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->

                if (!isUserScrolling) return@collect

                isScrollingDown = if (index > previousIndex) {
                    true
                } else if (index < previousIndex) {
                    false
                } else {
                    offset > previousOffset
                }

                // Actualizar capítulo si cambia el índice
                val newChapter = if(list.any()) list.first().Verse[index].Chapter else capitulo // Implementa esta función según tu lógica
                if (newChapter != capitulo) {
                    bibleViewmodel.setCapitulo(newChapter!!)
                    bibleViewmodel.sharedComp.value.saveCapitulo(newChapter)
                }

                previousIndex = index
                previousOffset = offset

                // Resetear la bandera después de un corto tiempo
                isUserScrolling = false
            }
    }

    LaunchedEffect(capitulo) {
        try {
            if (!isUserScrolling){
                bibleViewmodel.scrollToChapter(coroutineScope, listState)
            }

        }
        catch (e: Exception){
            Log.e("Error ScrollToLetter Plus", e.message.toString())
        }
    }
    // Inicializar TextToSpeech cuando la pantalla se cargue
    LaunchedEffect(Unit) {
        bibleViewmodel.initializeTTS(context, coroutineScope, listState)
    }
    /*LaunchedEffect(key1 = context) {
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
                        //Toast.makeText(context, "Preferred voice not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
//                currentIndex = utteranceId?.toInt() ?: 0
//                coroutineScope.launch {
//                    listState.animateScrollToItem(currentIndex)
//                }
                //currentIndex = utteranceId?.toInt() ?: 0
                coroutineScope.launch {
                    listState.animateScrollToItem(capitulo)
                }
            }

            override fun onDone(utteranceId: String?) {
                val nextIndex = (utteranceId?.toInt() ?: 0) + 1
                if (nextIndex < list.first().Verse.size) {
                    tts?.speak(list.first().Verse[nextIndex].Contenido, TextToSpeech.QUEUE_FLUSH, null, nextIndex.toString())
                } else {
                    bibleViewmodel.setCapitulo(0)
                    //currentIndex = 0
                    isSpeaking = false
                }
            }

            override fun onError(utteranceId: String?) {}
        })

    }*/

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
                    .padding(bottom = 70.dp)
                    .nestedScroll(nestedScrollConnection) // Aplica el detector de scroll táctil
                ,
                state = listState
            )
            {

                list.forEach{ book ->
                    stickyHeader {
                        AnimatedVisibility(
                            visible = !isScrollingDown,
                            enter = fadeIn() + slideInVertically { -it },
                            exit = fadeOut() + slideOutVertically { -it }
                        )
                        {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    bottomStart = 50.dp,
                                    bottomEnd = 50.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ){

                                Row(
                                    Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        bibleViewmodel.lessBook()
                                        bibleViewmodel.sharedComp.value.saveCurrentBook(libro)

                                    }) {
                                        Icon(imageVector = Icons.Rounded.ArrowBackIos, contentDescription = null)
                                    }

                                    Text(text = book.Human, style = MaterialTheme.typography.displaySmall)
                                    IconButton(onClick = {
                                        bibleViewmodel.plusBook()
                                        bibleViewmodel.sharedComp.value.saveCurrentBook(libro)

                                    }) {
                                        Icon(imageVector = Icons.Rounded.ArrowForwardIos, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed((book.Verse)){ index, item ->
                        //val isSelected = selectedIndex == index
                        val isSelected = selectedItems.value.contains(index)
                        val isSelectedMode = selectedItemToSpeak.intValue >= 0

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
                                    Text(text = item.ChapterName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            //.background(if (isSelected) Color.Yellow else Color.Transparent)
                                            .pointerInput(Unit){
                                                detectTapGestures(
                                                    onLongPress = {
                                                        if (!isSelectedMode)
                                                            toggleSelection(index)
                                                    },
                                                    onTap = {
                                                        if(!isSelected)
                                                            toggleSelectionToSpeak(index)
                                                    }
                                                )
                                            }
                                            /*.clickable {
                                                //toggleSelection(index)
                                            },*/,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        fontSize = 20.sp
                                    )
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
                                                .pointerInput(Unit){
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            toggleSelection(index)
                                                        },
                                                        onTap = {
                                                            toggleSelectionToSpeak(index)
                                                        }
                                                    )
                                                }
                                                /*.clickable {
                                                    //toggleSelection(index)
                                                },*/,
                                            fontWeight = if (isSelected) FontWeight.Bold else if(isSelectedMode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                }
                            }

                        }
                    }
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
                        var expanded by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = {
                                    bibleViewmodel.decrementCapitulo()
                                    bibleViewmodel.sharedComp.value.saveCapitulo(capitulo)
                                },
                                modifier = Modifier.padding(2.dp)) {
                                Icon(imageVector = Icons.Rounded.ArrowBackIos, contentDescription = null)
                            }
                            Column(modifier = Modifier) {
                                TextButton(onClick = { expanded = true }, modifier = Modifier.padding(2.dp)) {
                                    Text(text = "Capitulo $capitulo")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .width(300.dp)
                                        .heightIn(max = 400.dp)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                                    ) {
                                        List(if(list.any()) list.first().Chapters else 0) { chapter ->
                                            val chapterr = chapter+1
                                            TextButton(
                                                onClick = {
                                                    bibleViewmodel.setCapitulo(chapterr)
                                                    bibleViewmodel.sharedComp.value.saveCapitulo(chapterr)
                                                    expanded = false
                                                },
                                                modifier = Modifier
                                                    .padding(2.dp)
                                                    .background(
                                                        if (chapterr == capitulo)
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        else Color.Transparent,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            ) {
                                                Text(
                                                    text = (chapterr).toString(),
                                                    fontWeight = if (chapterr == capitulo) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (chapterr == capitulo) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                                    modifier = Modifier.padding(4.dp)
                                                )
                                            }
                                        }
                                    }
//
                                }
                            }
                            TextButton(
                                onClick = {
                                    bibleViewmodel.aumentarCapitulo()
                                    bibleViewmodel.sharedComp.value.saveCapitulo(capitulo)
                                },
                                modifier = Modifier.padding(2.dp)) {
                                Icon(imageVector = Icons.Rounded.ArrowForwardIos, contentDescription = null)
                            }

                        }

                    }
                    TextToSpeechControls(viewModel = bibleViewmodel, listState, capitulo)
                    /*Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {

                        Row {
                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        tts?.stop()
                                        isSpeaking = false
                                        isPaused = false
                                    } else {
                                        if (isPaused) {
                                            tts?.speak(pausedText, TextToSpeech.QUEUE_FLUSH, null, capitulo.toString())
                                            isPaused = false
                                        } else {
                                            tts?.speak(list.first().Verse[capitulo].Contenido, TextToSpeech.QUEUE_FLUSH, null, capitulo.toString())
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
                                        pausedText = list.first().Verse[capitulo].Contenido!!
                                        isSpeaking = false
                                        isPaused = true
                                    }
                                },
                                enabled = isSpeaking
                            ) {
                                Icon(imageVector = Icons.Rounded.Pause, contentDescription = "pausa")
                            }
                        }
                    }*/

                }
            }

        }
    }

}

@Composable
fun TextToSpeechControls(viewModel: BibleViewmodel, listState: LazyListState, capitulo: Int) {
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    Row(Modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Row {
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        viewModel.stopReading()
                    } else {
                        if (isPaused) {
                            viewModel.resumeReading()
                        } else {
                            viewModel.speakNextVerse()
                        }
                    }
                }
            ) {
                Icon(imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        viewModel.pauseReading()
                    }
                },
                enabled = isSpeaking
            ) {
                Icon(imageVector = Icons.Rounded.Pause, contentDescription = "pausa")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicPagerWithSwipeAction(items: List<Books>, book: List<Books>, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit, content: @Composable PagerScope.(page: Int, pageState: PagerState) -> Unit){
    val pagerState = rememberPagerState(
        initialPage = if(items.any() && book.any()) items.indexOfFirst { v -> v.Osis == book.first().Osis } + 1 else 0,
        pageCount = { items.size })
    var previousPage by remember { mutableStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage

        if (currentPage > previousPage) {
            // Swipe hacia la izquierda (siguiente página)
            onSwipeLeft()
        } else if (currentPage < previousPage) {
            // Swipe hacia la derecha (página anterior)
            onSwipeRight()
        }

        previousPage = currentPage
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        content(page, pagerState)
    }

}

/*DynamicPagerWithSwipeAction(
items = listBook,
book = list,
onSwipeLeft = {
    bibleViewmodel.plusBook()
    bibleViewmodel.sharedComp.value.saveCurrentBook(libro)

},
onSwipeRight = {
    bibleViewmodel.lessBook()
    bibleViewmodel.sharedComp.value.saveCurrentBook(libro)

}
){ page, pagerState ->
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    // Suavizamos la rotación usando una animación con interpolación
    val rotation by animateFloatAsState(
        targetValue = pageOffset * -90f, // Rota más lento
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing), // Transición suave
        label = "page_rotation"
    )

    val alpha by animateFloatAsState(
        targetValue = 1f - kotlin.math.abs(pageOffset), // Suaviza el desvanecimiento
        animationSpec = tween(durationMillis = 500),
        label = "page_alpha"
    )
}

Modifier
                        .fillMaxSize()
                        .padding(bottom = 70.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 16f * density // Simula profundidad
                            this.alpha = alpha
                        }
                    ,

*/


