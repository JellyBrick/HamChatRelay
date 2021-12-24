package be.zvz.hamchatrelay

import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class TCPServer(private val chatDriver: ChatDriver) {
    private val log = LoggerFactory.getLogger(TCPServer::class.java)
    private var listenerThread: Thread? = null

    fun open() {
        log.info("클라이언트 연결 대기")
        val listener = ServerSocket(PORT_NUMBER)
        listener.accept().use { socket ->
            log.info("IP: {} 연결됨", (socket.remoteSocketAddress as InetSocketAddress).address.hostAddress)
            listenerThread = thread(
                start = true,
                isDaemon = true,
                contextClassLoader = null,
                name = "listener-thread"
            ) {
                val buffer = ByteArray(BUFFER_SIZE)
                socket.getInputStream().use { ins ->
                    BufferedInputStream(ins).use {
                        while (it.read(buffer, 0, buffer.size) != 0) {
                            var str = buffer.toString(StandardCharsets.UTF_8).replace("\u0000", "")
                            val tokens = str.split(NET_DELIM)
                            if (tokens.size < 2) {
                                continue
                            }
                            val token = if (tokens[0].length == NET_SIG.length + 1) {
                                tokens[0].substring(1)
                            } else {
                                tokens[0]
                            }
                            if (token != NET_SIG) {
                                continue
                            }
                            str = tokens[1]
                            log.info("받음: {}", str)
                            chatDriver.enqueueMessage(str)
                        }
                    }
                }
            }
        }
    }

    fun join() {
        listenerThread?.join()
    }

    companion object {
        const val PORT_NUMBER = 9917
        const val BUFFER_SIZE = 32 * 1024
        const val NET_DELIM = '#'
        const val NET_SIG = "LEX"
    }
}
