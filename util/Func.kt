package net.rxaa.util;

fun interface Func0 {
    fun run();
}

fun interface Func1<T> {
    fun run(arg: T);
}

fun interface Func2<T, T2> {
    fun run(arg: T, arg2: T2);
}
