package com.jbncode.bibliaapp.view.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.jbncode.bibliaapp.R
import com.jbncode.bibliaapp.domain.model.Books
import com.jbncode.bibliaapp.domain.model.Verses
import com.jbncode.bibliaapp.view.viewmodel.BibleViewmodel
import com.jbncode.bibliaapp.view.viewmodel.MainViewmodel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun BibleScreen(modifier: Modifier, bibleViewmodel: BibleViewmodel, mainViewmodel: MainViewmodel, navigation : NavHostController, scrollBehavior: TopAppBarScrollBehavior){
    val view = LocalView.current

    // --- Define los colores que quieres para esta pantalla ---
    val desiredStatusBarColor = MaterialTheme.colorScheme.primaryContainer // Ejemplo: Negro semi-transparente
    val desiredNavBarColor = MaterialTheme.colorScheme.primaryContainer    // Ejemplo: Un azul oscuro opaco

    if (!view.isInEditMode) {
        val window = view.context.findActivity()?.window
        DisposableEffect(key1 = window, key2 = desiredStatusBarColor, key3 = desiredNavBarColor) {
            window?.let { win ->
                val originalStatusBarColor = win.statusBarColor
                val originalNavBarColor = win.navigationBarColor
                val insetsController = WindowCompat.getInsetsController(win, view)
                val originalIsLightStatusBar = insetsController.isAppearanceLightStatusBars
                val originalIsLightNavBar = insetsController.isAppearanceLightNavigationBars

                // Establecer nuevos colores para las barras
                win.statusBarColor = desiredStatusBarColor.toArgb()
                win.navigationBarColor = desiredNavBarColor.toArgb()

                // Ajustar el color de los iconos de las barras para contraste
                // Si la barra es oscura, los iconos deben ser claros (isAppearanceLight... = false)
                // Si la barra es clara, los iconos deben ser oscuros (isAppearanceLight... = true)
                insetsController.isAppearanceLightStatusBars = !desiredStatusBarColor.isPerceivedAsDark()
                insetsController.isAppearanceLightNavigationBars = !desiredNavBarColor.isPerceivedAsDark()

                onDispose {
                    // Restaurar colores e iconos originales al salir de la pantalla
                    win.statusBarColor = originalStatusBarColor
                    win.navigationBarColor = originalNavBarColor
                    insetsController.isAppearanceLightStatusBars = originalIsLightStatusBar
                    insetsController.isAppearanceLightNavigationBars = originalIsLightNavBar
                }
            } ?: onDispose {} // Si window es null, onDispose no tiene nada que hacer
        }
    }
    val context = LocalContext.current
    val list: List<Books> by bibleViewmodel.getAllBiblia.collectAsState()
    //val listBook by bibleViewmodel.getAllBook.collectAsState()
    val libro: String by bibleViewmodel.libro.collectAsState("Gen")
    val capitulo: Int by bibleViewmodel.capitulo.collectAsState()
    val libros by bibleViewmodel.getBooks.collectAsState()
    //val shared: SharedPreferenceBible by bibleViewmodel.sharedComp.collectAsState(initial = SharedPreferenceBible(context = context))

    // Observar el idioma actual del TTS
    val currentTTSLanguage by bibleViewmodel.currentTTSLanguage.collectAsState()
    val isTTSInitialized by bibleViewmodel.isTTSInitialized.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isScrollingDown by mainViewmodel.isTopAppBarAnimationVisible.collectAsState()
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }
    var isUserScrolling by remember { mutableStateOf(false) }

    val selectedItems = remember { mutableStateOf(setOf<Int>()) }
    val selectedItemToSpeak = remember { mutableIntStateOf(-1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    fun toggleSelection(index: Int) {
        val currentSelection = selectedItems.value.toMutableSet()
        if (currentSelection.contains(index)) {
            currentSelection.remove(index)
        } else {
            currentSelection.add(index)
        }
        selectedItems.value = currentSelection

        coroutineScope.launch {
            if (currentSelection.isNotEmpty())
                showBottomSheet = true
        }
    }

    fun copySelectedText(items: List<Verses>) {
        val chapterLabel = context.getString(R.string.bible_chapter_label)

        val selected = selectedItems.value
            .sorted()
            .mapNotNull { index -> items.getOrNull(index) }
            .filter { verser ->
                !verser.Contenido.isNullOrBlank()
            }

        if (selected.isEmpty()) return

        val bookName = selected.firstOrNull()?.Book ?: "Biblia"
        val sb = StringBuilder()
        sb.append("📖 $bookName\n\n")

        var lastChapter: Int? = null

        for (verse in selected) {
            if (verse.ChapterNum != null && verse.ChapterNum != lastChapter) {
                sb.append("$chapterLabel ${verse.ChapterNum}\n\n")
                lastChapter = verse.ChapterNum

            }

            if (!verse.ChapterName.isNullOrEmpty()) {
                sb.append("${verse.ChapterName}\n")
            }

            sb.append("${verse.Verse}. ${verse.Contenido?.trim()}\n")
        }

        val clip = ClipData.newPlainText(context.getString(R.string.bible_copy_label), sb.toString())
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.bible_copy_toast), Toast.LENGTH_SHORT).show()

        // Limpiar selección si deseas
        // selectedItems.value = emptySet()
    }

    fun shareSelectedText(items: List<Verses>) {
        val chapterLabel = context.getString(R.string.bible_chapter_label)
        val moreLinkText = context.getString(R.string.bible_share_more_link)

        val selected = selectedItems.value
            .sorted()
            .mapNotNull { index -> items.getOrNull(index) }
            .filter { !it.Contenido.isNullOrBlank() }

        if (selected.isEmpty()) return

        val bookName = selected.first().Book ?: "Biblia"
        val chapterNum = selected.first().ChapterNum ?: 1

        val formattedBookName = bookName.lowercase()
            .replace(" ", "-")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")

        val link = "https://bibliapp.com/biblia/$formattedBookName/$chapterNum"

        val sb = StringBuilder()
        sb.append("📖 $bookName\n\n")

        var lastChapter: Int? = null

        for (verse in selected) {
            if (verse.ChapterNum != null && verse.ChapterNum != lastChapter) {
                sb.append("$chapterLabel ${verse.ChapterNum}\n\n")
                lastChapter = verse.ChapterNum

                if (!verse.ChapterName.isNullOrEmpty()) {
                    sb.append("${verse.ChapterName}\n\n")
                }
            }

            sb.append("${verse.Verse}. ${verse.Contenido?.trim()}\n")
        }

        sb.append("\n📲 $moreLinkText $link")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.bible_share_title)))
    }

    // Variables para trackear sesión de lectura
    var sessionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isActivelyReading by remember { mutableStateOf(true) }

    // NUEVO: LaunchedEffect para trackear tiempo de sesión
    LaunchedEffect(Unit) {
        sessionStartTime = System.currentTimeMillis()
    }

    // Detectar eventos de scroll manual (táctil) usando `nestedScrollConnection`
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isUserScrolling = true // Usuario deslizó con el dedo
                    isActivelyReading = true // Usuario está leyendo activamente
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

                mainViewmodel.showTopAppBarAnimation( if (index > previousIndex) {
                    true
                } else if (index < previousIndex) {
                    false
                } else {
                    offset > previousOffset
                })

                // Actualizar capítulo si cambia el índice
                val newChapter = if(list.any()) list.first().Verse[index].Chapter else capitulo // Implementa esta función según tu lógica
                if (newChapter != capitulo) {
                    bibleViewmodel.setCapitulo(newChapter!!)
                    bibleViewmodel.sharedComp.value.saveCapitulo(newChapter)
                }

                if (list.isNotEmpty() && index < list.first().Verse.size) {
                    val currentVerse = list.first().Verse[index]
                    val newChapter = currentVerse.ChapterNum ?: currentVerse.Chapter

                    if (newChapter != null && newChapter != capitulo) {
                        Log.d("BibleScreen", "Scroll manual detectó cambio a capítulo: $newChapter")
                        bibleViewmodel.setCapitulo(newChapter) // ✅ Aquí se activará el tracking
                        bibleViewmodel.sharedComp.value.saveCapitulo(newChapter)
                    }
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

    // LaunchedEffect para reinicializar TTS cuando cambie el idioma
    LaunchedEffect(currentTTSLanguage) {
        if (isTTSInitialized) {
            // Reinicializar TTS con el nuevo idioma
            bibleViewmodel.initializeTTS(context, coroutineScope, listState)
        }
    }

    // NUEVO: DisposableEffect para guardar tiempo de sesión al salir
    DisposableEffect(Unit) {
        onDispose {
            if (isActivelyReading) {
                val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 60000 // minutos
                if (sessionDuration > 0) {
                    bibleViewmodel.sharedComp.value.addReadingTime(sessionDuration.toInt())
                    Log.d("BibleStats", "Sesión de lectura: ${sessionDuration} minutos")
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ){
            BottomModalCopyText(list, libro = libro, copySelectedText = { copySelectedText(it) }, shareSelectedText = { shareSelectedText(it) })
        }
    }

//    val navVisible by remember { mutableStateOf(true) }
//    val offsetY by animateDpAsState(
//        targetValue = if (navVisible) 0.dp else 80.dp, // altura del nav
//        label = ""
//    )
    Box(modifier = Modifier
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
                            var expanded by remember { mutableStateOf(false) }
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
                                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null)
                                }
                                //Text(text = book.Human, style = MaterialTheme.typography.displaySmall)
                                Column(modifier = Modifier) {
                                    TextButton(onClick = { expanded = true }, modifier = Modifier.padding(2.dp)) {
                                        Text(text = book.Human, style = MaterialTheme.typography.displaySmall)
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
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalArrangement = Arrangement.Center
                                        ) {

                                            libros.forEach { book2 ->
                                                val books = book2
                                                TextButton(
                                                    onClick = {
                                                        bibleViewmodel.setLibro(books.Osis)
                                                        bibleViewmodel.sharedComp.value.saveCurrentBook(books.Osis)
                                                        bibleViewmodel.getBook(books.Osis)
                                                        expanded = false
                                                    },
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .background(
                                                            if (books.Human == book.Human)
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                            else Color.Transparent,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                ) {
                                                    Text(
                                                        text = books.Human,
                                                        fontWeight = if (books.Human == book.Human) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (books.Human == book.Human) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                                        modifier = Modifier.padding(4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    bibleViewmodel.plusBook()
                                    bibleViewmodel.sharedComp.value.saveCurrentBook(libro)

                                }) {
                                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null)
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
//                                                        if (!isSelectedMode)
//                                                            toggleSelection(index)
                                                },
                                                onTap = {
//                                                        if(!isSelected)
//                                                            toggleSelectionToSpeak(index)
                                                    if (!isSelectedMode)
                                                        toggleSelection(index)
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
                                                        //toggleSelection(index)
                                                    },
                                                    onTap = {
                                                        //toggleSelectionToSpeak(index)
                                                        toggleSelection(index)
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
                            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null)
                        }
                        Column(modifier = Modifier) {
                            TextButton(onClick = { expanded = true }, modifier = Modifier.padding(2.dp)) {
                                Text(text = "${stringResource(R.string.bible_chapter_label)} $capitulo")
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
                            }
                        }
                        TextButton(
                            onClick = {
                                bibleViewmodel.aumentarCapitulo()
                                bibleViewmodel.sharedComp.value.saveCapitulo(capitulo)
                            },
                            modifier = Modifier.padding(2.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null)
                        }

                    }

                }
                TextToSpeechControls(viewModel = bibleViewmodel)

            }
        }
    }
}

@Composable
fun TextToSpeechControls(viewModel: BibleViewmodel) {
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

@Composable
fun BottomModalCopyText(items: List<Books>, libro: String, copySelectedText: (List<Verses>) -> Unit, shareSelectedText: (List<Verses>) -> Unit){
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
                    copySelectedText(items.first { v -> v.Osis == libro }.Verse)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.elevatedButtonElevation(10.dp)
            ) {
                Icon(imageVector = Icons.Rounded.CopyAll, contentDescription = "")
                Spacer(modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.bible_action_copy))
            }

            Button(
                onClick = {
                    shareSelectedText(items.first { v -> v.Osis == libro }.Verse)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.elevatedButtonElevation(10.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Share, contentDescription = "")
                Spacer(modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.bible_action_share))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}


/*@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicPagerWithSwipeAction(items: List<Books>, book: List<Books>, onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit, content: @Composable PagerScope.(page: Int, pageState: PagerState) -> Unit){
    val pagerState = rememberPagerState(
        initialPage = if(items.any() && book.any()) items.indexOfFirst { v -> v.Osis == book.first().Osis } + 1 else 0,
        pageCount = { items.size })
    var previousPage by remember { mutableIntStateOf(0) }

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

}*/

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


