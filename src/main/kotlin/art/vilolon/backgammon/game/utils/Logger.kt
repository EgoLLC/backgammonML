package art.vilolon.backgammon.game.utils

interface Logger {
    val tag: String

    fun v(message: String)

    fun d(message: String)

    fun i(message: String)

    fun w(message: String, throwable: Throwable? = null)

    fun e(message: String, throwable: Throwable? = null)

    interface Factory {
        fun create(tag: String): Logger
    }

}

val LOGGER_FACTORY = object : Logger.Factory {
    override fun create(tag: String): Logger {
        return object: Logger {
            override val tag: String = ""
            override fun v(message: String) = Unit
            override fun d(message: String) = Unit
            override fun i(message: String)= Unit
            override fun w(message: String, throwable: Throwable?)= println(message)
            override fun e(message: String, throwable: Throwable?)= println(message)
        }
    }

}