package dev.scriptor.kengine

import java.awt.*
import java.awt.event.*

class KWindow {

    val environment: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val device: GraphicsDevice = environment.defaultScreenDevice
    val configuration: GraphicsConfiguration = device.defaultConfiguration

    val frame = Frame("Hello World", configuration)
        .also {
            it.type = Window.Type.NORMAL
            it.size = Dimension(800, 600)
            it.isEnabled = true
            it.isVisible = false
            it.isResizable = true
            it.isFocusable = true
            it.isAlwaysOnTop = false
            it.isUndecorated = false

            it.addWindowListener(object : WindowListener {
                override fun windowOpened(event: WindowEvent) {
                    windowOpenedListeners.forEach { listener -> listener() }
                }

                override fun windowClosing(event: WindowEvent) {
                    windowClosingListeners.forEach { listener -> listener() }
                }

                override fun windowClosed(event: WindowEvent) {
                    windowClosedListeners.forEach { listener -> listener() }
                }

                override fun windowIconified(event: WindowEvent) {
                    windowIconifiedListeners.forEach { listener -> listener() }
                }

                override fun windowDeiconified(event: WindowEvent) {
                    windowDeiconifiedListeners.forEach { listener -> listener() }
                }

                override fun windowActivated(event: WindowEvent) {
                    windowActivatedListeners.forEach { listener -> listener() }
                }

                override fun windowDeactivated(event: WindowEvent) {
                    windowDeactivatedListeners.forEach { listener -> listener() }
                }
            })

            it.addWindowFocusListener(object : WindowFocusListener {
                override fun windowGainedFocus(event: WindowEvent) {
                    windowGainedFocusListeners.forEach { listener -> listener() }
                }

                override fun windowLostFocus(event: WindowEvent) {
                    windowLostFocusListeners.forEach { listener -> listener() }
                }
            })

            it.addWindowStateListener { event ->
                windowStateListeners.forEach { listener ->
                    listener(
                        StateData(
                            event.oldState,
                            event.newState,
                        )
                    )
                }
            }

            it.addComponentListener(object : ComponentListener {
                override fun componentResized(event: ComponentEvent) {
                    componentResizedListeners.forEach { listener -> listener() }
                }

                override fun componentMoved(event: ComponentEvent) {
                    componentMovedListeners.forEach { listener -> listener() }
                }

                override fun componentShown(event: ComponentEvent) {
                    componentShownListeners.forEach { listener -> listener() }
                }

                override fun componentHidden(event: ComponentEvent) {
                    componentHiddenListeners.forEach { listener -> listener() }
                }
            })

            it.addKeyListener(object : KeyListener {
                override fun keyTyped(event: KeyEvent) {
                    keyTypedListeners.forEach { listener ->
                        listener(
                            KeyData(
                                event.keyCode,
                                event.extendedKeyCode,
                                event.keyLocation,
                                event.keyChar,
                                event.modifiersEx,
                                event.isActionKey,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun keyPressed(event: KeyEvent) {
                    keyPressedListeners.forEach { listener ->
                        listener(
                            KeyData(
                                event.keyCode,
                                event.extendedKeyCode,
                                event.keyLocation,
                                event.keyChar,
                                event.modifiersEx,
                                event.isActionKey,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun keyReleased(event: KeyEvent) {
                    keyReleasedListeners.forEach { listener ->
                        listener(
                            KeyData(
                                event.keyCode,
                                event.extendedKeyCode,
                                event.keyLocation,
                                event.keyChar,
                                event.modifiersEx,
                                event.isActionKey,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }
            })

            it.addMouseListener(object : MouseListener {
                override fun mouseClicked(event: MouseEvent) {
                    mouseClickedListeners.forEach { listener ->
                        listener(
                            MouseData(
                                event.x,
                                event.y,
                                event.xOnScreen,
                                event.yOnScreen,
                                event.button,
                                event.clickCount,
                                event.isPopupTrigger,
                                event.modifiersEx,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun mousePressed(event: MouseEvent) {
                    mousePressedListeners.forEach { listener ->
                        listener(
                            MouseData(
                                event.x,
                                event.y,
                                event.xOnScreen,
                                event.yOnScreen,
                                event.button,
                                event.clickCount,
                                event.isPopupTrigger,
                                event.modifiersEx,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun mouseReleased(event: MouseEvent) {
                    mouseReleasedListeners.forEach { listener ->
                        listener(
                            MouseData(
                                event.x,
                                event.y,
                                event.xOnScreen,
                                event.yOnScreen,
                                event.button,
                                event.clickCount,
                                event.isPopupTrigger,
                                event.modifiersEx,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun mouseEntered(event: MouseEvent) {
                    mouseEnteredListeners.forEach { listener ->
                        listener(
                            MouseData(
                                event.x,
                                event.y,
                                event.xOnScreen,
                                event.yOnScreen,
                                event.button,
                                event.clickCount,
                                event.isPopupTrigger,
                                event.modifiersEx,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }

                override fun mouseExited(event: MouseEvent) {
                    mouseExitedListeners.forEach { listener ->
                        listener(
                            MouseData(
                                event.x,
                                event.y,
                                event.xOnScreen,
                                event.yOnScreen,
                                event.button,
                                event.clickCount,
                                event.isPopupTrigger,
                                event.modifiersEx,
                                event.isAltDown,
                                event.isAltGraphDown,
                                event.isControlDown,
                                event.isMetaDown,
                                event.isShiftDown,
                            )
                        )
                    }
                }
            })

            it.addMouseWheelListener { event ->
                mouseWheelListeners.forEach { listener ->
                    listener(
                        MouseData(
                            event.x,
                            event.y,
                            event.xOnScreen,
                            event.yOnScreen,
                            event.button,
                            event.clickCount,
                            event.isPopupTrigger,
                            event.modifiersEx,
                            event.isAltDown,
                            event.isAltGraphDown,
                            event.isControlDown,
                            event.isMetaDown,
                            event.isShiftDown,
                        ),
                        MouseWheelData(
                            event.scrollType,
                            event.scrollAmount,
                            event.wheelRotation,
                            event.preciseWheelRotation,
                            event.unitsToScroll,
                        ),
                    )
                }
            }
        }

    private val windowOpenedListeners = mutableListOf<() -> Unit>()
    private val windowClosingListeners = mutableListOf<() -> Unit>()
    private val windowClosedListeners = mutableListOf<() -> Unit>()
    private val windowIconifiedListeners = mutableListOf<() -> Unit>()
    private val windowDeiconifiedListeners = mutableListOf<() -> Unit>()
    private val windowActivatedListeners = mutableListOf<() -> Unit>()
    private val windowDeactivatedListeners = mutableListOf<() -> Unit>()
    private val windowGainedFocusListeners = mutableListOf<() -> Unit>()
    private val windowLostFocusListeners = mutableListOf<() -> Unit>()

    data class StateData(
        val oldState: Int,
        val newState: Int,
    )

    private val windowStateListeners = mutableListOf<(StateData) -> Unit>()

    private val componentResizedListeners = mutableListOf<() -> Unit>()
    private val componentMovedListeners = mutableListOf<() -> Unit>()
    private val componentShownListeners = mutableListOf<() -> Unit>()
    private val componentHiddenListeners = mutableListOf<() -> Unit>()

    data class KeyData(
        val keyCode: Int,
        val keyCodeEx: Int,
        val keyLocation: Int,
        val keyChar: Char,
        val modifiersEx: Int,
        val isActionKey: Boolean,
        val isAltDown: Boolean,
        val isAltGraphDown: Boolean,
        val isControlDown: Boolean,
        val isMetaDown: Boolean,
        val isShiftDown: Boolean,
    )

    private val keyTypedListeners = mutableListOf<(KeyData) -> Unit>()
    private val keyPressedListeners = mutableListOf<(KeyData) -> Unit>()
    private val keyReleasedListeners = mutableListOf<(KeyData) -> Unit>()

    data class MouseData(
        val x: Int,
        val y: Int,
        val xOnScreen: Int,
        val yOnScreen: Int,
        val button: Int,
        val clickCount: Int,
        val isPopupTrigger: Boolean,
        val modifiersEx: Int,
        val isAltDown: Boolean,
        val isAltGraphDown: Boolean,
        val isControlDown: Boolean,
        val isMetaDown: Boolean,
        val isShiftDown: Boolean,
    )

    data class MouseWheelData(
        val scrollType: Int,
        val scrollAmount: Int,
        val wheelRotation: Int,
        val preciseWheelRotation: Double,
        val unitsToScroll: Int,
    )

    private val mouseClickedListeners = mutableListOf<(MouseData) -> Unit>()
    private val mousePressedListeners = mutableListOf<(MouseData) -> Unit>()
    private val mouseReleasedListeners = mutableListOf<(MouseData) -> Unit>()
    private val mouseEnteredListeners = mutableListOf<(MouseData) -> Unit>()
    private val mouseExitedListeners = mutableListOf<(MouseData) -> Unit>()
    private val mouseWheelListeners = mutableListOf<(MouseData, MouseWheelData) -> Unit>()

    fun onOpened(block: () -> Unit): KWindow {
        windowOpenedListeners.add(block)
        return this
    }

    fun onClosing(block: () -> Unit): KWindow {
        windowClosingListeners.add(block)
        return this
    }

    fun onClosed(block: () -> Unit): KWindow {
        windowClosedListeners.add(block)
        return this
    }

    fun onIconified(block: () -> Unit): KWindow {
        windowIconifiedListeners.add(block)
        return this
    }

    fun onDeiconified(block: () -> Unit): KWindow {
        windowDeiconifiedListeners.add(block)
        return this
    }

    fun onActivated(block: () -> Unit): KWindow {
        windowActivatedListeners.add(block)
        return this
    }

    fun onDeactivated(block: () -> Unit): KWindow {
        windowDeactivatedListeners.add(block)
        return this
    }

    fun onGainedFocus(block: () -> Unit): KWindow {
        windowGainedFocusListeners.add(block)
        return this
    }

    fun onLostFocus(block: () -> Unit): KWindow {
        windowLostFocusListeners.add(block)
        return this
    }

    fun onStateChange(block: (StateData) -> Unit): KWindow {
        windowStateListeners.add(block)
        return this
    }

    fun onResized(block: () -> Unit): KWindow {
        componentResizedListeners.add(block)
        return this
    }

    fun onMoved(block: () -> Unit): KWindow {
        componentMovedListeners.add(block)
        return this
    }

    fun onShown(block: () -> Unit): KWindow {
        componentShownListeners.add(block)
        return this
    }

    fun onHidden(block: () -> Unit): KWindow {
        componentHiddenListeners.add(block)
        return this
    }

    fun onKeyTyped(block: (KeyData) -> Unit): KWindow {
        keyTypedListeners.add(block)
        return this
    }

    fun onKeyPressed(block: (KeyData) -> Unit): KWindow {
        keyPressedListeners.add(block)
        return this
    }

    fun onKeyReleased(block: (KeyData) -> Unit): KWindow {
        keyReleasedListeners.add(block)
        return this
    }

    fun onMouseClicked(block: (MouseData) -> Unit): KWindow {
        mouseClickedListeners.add(block)
        return this
    }

    fun onMousePressed(block: (MouseData) -> Unit): KWindow {
        mousePressedListeners.add(block)
        return this
    }

    fun onMouseReleased(block: (MouseData) -> Unit): KWindow {
        mouseReleasedListeners.add(block)
        return this
    }

    fun onMouseEntered(block: (MouseData) -> Unit): KWindow {
        mouseEnteredListeners.add(block)
        return this
    }

    fun onMouseExited(block: (MouseData) -> Unit): KWindow {
        mouseExitedListeners.add(block)
        return this
    }

    fun onMouseWheel(block: (MouseData, MouseWheelData) -> Unit): KWindow {
        mouseWheelListeners.add(block)
        return this
    }
}
