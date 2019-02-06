package io.snower.game.client

/** Implements ui rendering through nuklear, using JNI */
class NuklearUIRenderer : UIRenderer, UIDrawer {

    private val uiDrawables = hashMapOf<Class<out UIDrawable>, UIDrawable>()

    override fun <T : UIDrawable> registerUIElement(clazz: Class<T>, drawable: T) {
        check(clazz !in uiDrawables) { "class $clazz already has a drawable registered. Remove it first." }
        uiDrawables[clazz] = drawable
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UIDrawable> getUIElement(clazz: Class<T>): T? {
        return uiDrawables[clazz] as T?
    }

    override fun <T : UIDrawable> unregisterUIElement(clazz: Class<T>) {
        check(clazz in uiDrawables) { "class $clazz doesn't have a drawable registered." }
        uiDrawables -= clazz
    }

    private var width: Int = 800
    private var height: Int = 600

    override fun setResolution(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    // Now implement this using JNI.
    // first, completely ignore input. Just nuklear output.
    // Must have methods to, draw everything in UIDrawer (this may implement the interface natively)
    // And, methods to draw to openGL nuklear output

    private var nkContext = 0L

    override fun init() {
        nkContext = createNuklearContext()
    }

    override fun draw() {
        setCurrentNuklearContext(nkContext)
        for (drawable in uiDrawables.values) {
            drawable.draw(this, width.toFloat(), height.toFloat())
        }
        drawNuklearOutput(width, height)
    }

    override fun destroy() {
    }

    // Native functions

    // internally used (create and set the context for the functions below)
    private external fun createNuklearContext(): Long
    private external fun setCurrentNuklearContext(context: Long) // sets the global state to use the given context
    private external fun drawNuklearOutput(width: Int, height: Int)

    // Exposed in the interface
    // why direct access instead of wrapped?
    // essentially, less work at the cost of less flexibility
    external override fun begin(title: String, x: Float, y: Float, width: Float, height: Float, background: Int, flags: Int): Boolean
    external override fun end()
    external override fun layoutRowDynamic(height: Float, columns: Int)
    external override fun layoutRowStatic(height: Float, width: Float, columns: Int)
    external override fun label(text: String, align: Int)
    external override fun strokeCircle(x: Float, y: Float, diameter: Float, thickness: Float, color: Int)
    external override fun fillCircle(x: Float, y: Float, diameter: Float, color: Int)
    external override fun progress(current: Int, max: Int, color: Int, background: Int)

    // Constants
    // Hardcoded to avoid generating a damn method for each one
    override val WINDOW_TITLE: Int get() = 0
    override val WINDOW_NO_SCROLLBAR: Int get() = 0
    override val TEXT_LEFT: Int get() = 0
    override val TEXT_CENTER: Int get() = 0
    override val TEXT_RIGHT: Int get() = 0
}