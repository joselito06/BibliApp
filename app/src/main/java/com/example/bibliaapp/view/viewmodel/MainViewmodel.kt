package com.example.bibliaapp.view.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewmodel() : ViewModel() {
    private val _isTopAppBarVisible = MutableStateFlow(true)
    val isTopAppBarVisible: StateFlow<Boolean> = _isTopAppBarVisible

    private val _isBottomBarVisible = MutableStateFlow(true)
    val isBottomBarVisible: StateFlow<Boolean> = _isBottomBarVisible

    private val _isFloatingActionButtonVisible = MutableStateFlow(true)
    val isFloatingActionButtonVisible: StateFlow<Boolean> = _isFloatingActionButtonVisible

    // Acción del FloatingActionButton
    private val _fabAction: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)
    val fabAction: StateFlow<(() -> Unit)?> = _fabAction

    private val _fabIcon: MutableStateFlow<ImageVector?> = MutableStateFlow(null)
    val fabIcon: StateFlow<ImageVector?> = _fabIcon



    // Métodos para cambiar los estados
    fun showTopAppBar(show: Boolean) {
        _isTopAppBarVisible.value = show
    }

    fun showBottomBar(show: Boolean) {
        _isBottomBarVisible.value = show
    }

    fun showFloatingActionButton(show: Boolean) {
        _isFloatingActionButtonVisible.value = show
    }

    fun updateFabIcon(icon: ImageVector?) {
        _fabIcon.value = icon
    }

    fun updateFabAction(action: (() -> Unit)?) {
        _fabAction.value = action
    }

}