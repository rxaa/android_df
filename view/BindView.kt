package net.rxaa.view

class BindView<out T>(func: () -> T, binds: MutableList<() -> Unit>) : Lazy<T> {
    init {
        binds.add {
            _value = func()
        }
    }

    override fun isInitialized(): Boolean {
        return _value != null
    }

    private var _value: Any? = null

    override val value: T
        get() {
            return _value as T
        }
}