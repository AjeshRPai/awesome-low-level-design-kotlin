package questions.loggingFramework

import java.io.FileWriter
import java.io.IOException
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class LogLevel(val priority: Int) {
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    FATAL(5);

    companion object {
        fun fromPriority(priority: Int) = values().find { it.priority == priority }
    }
}

data class LoggerConfig(
    var logLevel: LogLevel,
    var logAppenders: List<LogAppender> = listOf(ConsoleAppender())
)

data class Log(
    val timestamp: Long = System.currentTimeMillis(),
    val logLevel: LogLevel,
    val logMessage: String
)

interface LogAppender {
    fun append(log: Log)
}

class ConsoleAppender : LogAppender {
    override fun append(log: Log) {
        println("${log.timestamp} [${log.logLevel}] ${log.logMessage}")
    }
}

class FileAppender(private val filePath: String) : LogAppender {
    override fun append(log: Log) {
        try {
            FileWriter(filePath, true).use { writer ->
                writer.write("${log.timestamp} [${log.logLevel}] ${log.logMessage}\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()  // Consider logging this to a fallback destination
        }
    }
}

class DatabaseAppender(private val jdbcUrl: String, private val username: String, private val password: String) :
    LogAppender {
    override fun append(log: Log) {
        try {
            DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
                connection.prepareStatement("INSERT INTO logs (level, message, timestamp) VALUES (?, ?, ?)")
                    .use { statement ->
                        statement.setString(1, log.logLevel.toString())
                        statement.setString(2, log.logMessage)
                        statement.setLong(3, log.timestamp)
                        statement.executeUpdate()
                    }
            }
        } catch (e: SQLException) {
            e.printStackTrace()  // Consider logging this to a fallback destination
        }
    }
}

class Logger private constructor() {
    private var config: LoggerConfig = LoggerConfig(LogLevel.INFO, listOf(ConsoleAppender()))
    private val logQueue = ConcurrentLinkedQueue<Log>()
    private val executor = Executors.newSingleThreadExecutor()

    init {
        executor.submit {
            while (true) {
                while (logQueue.isNotEmpty()) {
                    val log = logQueue.poll()
                    log?.let {
                        config.logAppenders.forEach { appender -> appender.append(it) }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(100)  // Adjust sleep time based on performance needs
            }
        }
    }

    @Synchronized
    fun setConfig(config: LoggerConfig) {
        this.config = config
    }

    fun log(level: LogLevel, message: String) {
        if (level.priority >= config.logLevel.priority) {
            val logMessage = Log(logLevel = level, logMessage = message)
            logQueue.offer(logMessage)
        }
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warning(message: String) = log(LogLevel.WARNING, message)
    fun error(message: String) = log(LogLevel.ERROR, message)
    fun fatal(message: String) = log(LogLevel.FATAL, message)

    companion object {
        val INSTANCE: Logger by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Logger() }
    }
}

fun main() {
    val logger = Logger.INSTANCE

    // Logging with default configuration
    logger.info("This is an information message")
    logger.warning("This is a warning message")
    logger.error("This is an error message")

    // Changing log level and adding multiple appenders
    val config = LoggerConfig(
        logLevel = LogLevel.DEBUG,
        logAppenders = listOf(ConsoleAppender(), FileAppender("app.log"))
    )
    logger.setConfig(config)

    logger.debug("This is a debug message")
    logger.info("This is another information message")
}
