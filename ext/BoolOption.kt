package net.rxaa.ext


inline fun <T> T?.isNull(f: () -> Unit): Boolean {
    if (this == null) {
        f()
        return true
    }
    return false
}

inline fun <T> T?.notNull(f: (p: T) -> Unit): Boolean {
    if (this != null) {
        f(this)
        return true
    }
    return false
}

inline fun String?.notEmpty(f: (p: String) -> Unit): Boolean {
    if (this != null && this.length > 0) {
        f(this)
        return true
    }
    return false
}

inline fun <T : List<*>> T?.notEmpty(f: (list: T) -> Unit): Boolean {
    if (this != null && this.size > 0) {
        f(this)
        return true
    }
    return false;
}

inline fun <T : Map<*,*>> T?.notEmpty(f: (list: T) -> Unit): Boolean {
    if (this != null && this.size > 0) {
        f(this)
        return true
    }
    return false;
}

inline fun Boolean?.isTrue(f: () -> Unit): Boolean {
    if (this != null && this == true) {
        f();
        return true
    }
    return false;
}

inline fun Boolean.nope(f: () -> Unit): Boolean {
    if (this == false)
        f();
    return this
}

inline fun Boolean?.nullOrFalse(f: () -> Unit): Boolean {
    if (this == null || this == false) {
        f();
        return true
    }
    return false
}

inline fun Long.isZero(f: () -> Unit): Boolean {
    if (this == 0L) {
        f();
        return true;
    }
    return false
}

inline fun Int.isZero(f: () -> Unit): Boolean {
    if (this == 0) {
        f();
        return true;
    }
    return false
}

inline fun Double.isZero(f: () -> Unit): Boolean {
    if (this == 0.0) {
        f();
        return true;
    }
    return false
}

inline fun Long?.notZero(f: (res: Long) -> Unit): Boolean {
    if (this != null && this != 0L) {
        f(this);
        return true;
    }
    return false
}

inline fun Int?.notZero(f: (res: Int) -> Unit): Boolean {
    if (this != null && this != 0) {
        f(this);
        return true;
    }
    return false
}