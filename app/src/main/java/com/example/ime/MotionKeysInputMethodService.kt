package com.example.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.animation.KeyAnimationController
import com.example.data.KeyboardPreferences
import com.example.ui.keyboard.KeyboardView
import com.example.ui.theme.MotionKeysTheme

import android.view.ViewGroup
import android.widget.FrameLayout

class MotionKeysInputMethodService : InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var keyboardPreferences: KeyboardPreferences
    private val animationController = KeyAnimationController()
    private var isKeyboardPaused by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        keyboardPreferences = KeyboardPreferences.getInstance(this)
    }

    override fun onCreateInputView(): View {
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
            setViewTreeLifecycleOwner(this@MotionKeysInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@MotionKeysInputMethodService)
            setViewTreeViewModelStoreOwner(this@MotionKeysInputMethodService)

            setContent {
                val settings by keyboardPreferences.settingsFlow.collectAsState()

                MotionKeysTheme {
                    KeyboardView(
                        settings = settings,
                        animationController = animationController,
                        onTextCommit = { text -> handleTextCommit(text) },
                        onDelete = { handleDelete() },
                        onEnter = { handleEnter() },
                        onOpenSettings = { openSettingsApp() },
                        isPaused = isKeyboardPaused
                    )
                }
            }
        }

        rootLayout.addView(composeView)
        return rootLayout
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (::keyboardPreferences.isInitialized) {
            keyboardPreferences.refresh()
        }
        isKeyboardPaused = false
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (::keyboardPreferences.isInitialized) {
            keyboardPreferences.refresh()
        }
        isKeyboardPaused = false
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        isKeyboardPaused = true
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (::keyboardPreferences.isInitialized) {
            keyboardPreferences.refresh()
        }
        isKeyboardPaused = false
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        isKeyboardPaused = true
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    private fun handleTextCommit(text: String) {
        val ic: InputConnection = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun handleDelete() {
        val ic: InputConnection = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (selectedText.isNullOrEmpty()) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
    }

    private fun handleEnter() {
        val ic: InputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo

        if (editorInfo != null) {
            val imeOptions = editorInfo.imeOptions
            val action = imeOptions and EditorInfo.IME_MASK_ACTION
            val noEnter = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

            if (!noEnter && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
                return
            }
        }

        // Default: send newline or enter key
        if (!sendDefaultEditorAction(true)) {
            ic.commitText("\n", 1)
        }
    }

    private fun openSettingsApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }
}
