package net.rxaa.util

//object ExceptionCode {
//    val normal=0
//    val activityClosed=1
//}

enum class ExceptionCode {
    normal,
    activityClosed,
    cancelHttp,
    statusCodeError,
}

class MsgException : Exception {

    //错误编号
    var code = ExceptionCode.normal.ordinal;

    //是否可以显示
    var showAble = true;

    constructor(msg: String) : // TODO Auto-generated constructor stub
            super(msg) {
    }

    constructor(msg: String, e: Throwable) : // TODO Auto-generated constructor stub
            super(msg, e) {
    }

    constructor(msg: String, code: Int) : // TODO Auto-generated constructor stub
            super(msg) {
        this.code = code;
    }

    constructor(msg: String, show: Boolean) : // TODO Auto-generated constructor stub
            super(msg) {
        this.showAble = show;
    }

    constructor(msg: String, code: Int, show: Boolean) : // TODO Auto-generated constructor stub
            super(msg) {
        this.code = code;
        this.showAble = show;
    }

    companion object {

        /**
         */
        private val serialVersionUID = -5560157952583772642L
    }

}
