package net.rxaa.util

import android.app.Activity
import kotlin.reflect.KProperty

object IntentExtra {
    const val ExtraStr = "_Extra_"
}

class StringExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): String? {
        return act.intent.getStringExtra(IntentExtra.ExtraStr + property.name)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: String?) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}

class ShortExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): Short {
        return act.intent.getShortExtra(IntentExtra.ExtraStr + property.name, 0)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: Short) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}

class IntExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): Int {
        return act.intent.getIntExtra(IntentExtra.ExtraStr + property.name, 0)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: Int) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}

class LongExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): Long {
        return act.intent.getLongExtra(IntentExtra.ExtraStr + property.name, 0)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: Long) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}

class DoubleExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): Double {
        return act.intent.getDoubleExtra(IntentExtra.ExtraStr + property.name, 0.0)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: Double) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}

class FloatExtra(val act: Activity) {

    operator fun getValue(thisRef: Activity?, property: KProperty<*>): Float {
        return act.intent.getFloatExtra(IntentExtra.ExtraStr + property.name, 0F)
    }

    operator fun setValue(thisRef: Activity?, property: KProperty<*>, value: Float) {
        act.intent.putExtra(IntentExtra.ExtraStr + property.name, value)
    }
}