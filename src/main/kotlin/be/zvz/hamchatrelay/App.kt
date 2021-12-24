package be.zvz.hamchatrelay

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class App {
    private val log = LoggerFactory.getLogger(App::class.java)
    private var isAnonymous = true
    private var id = ""
    private var password = ""

    fun start() {
        readCredential("setting.txt")
        startServer()
    }

    private fun readCredential(filename: String) {
        try {
            File(filename).bufferedReader().use { br ->
                br.forEachLine {
                    val split = it.split('=')
                    if (split.size > 1) {
                        when (split[0]) {
                            "유동" -> isAnonymous = split[1].toIntOrNull() != 0
                            "id" -> id = split[1]
                            "pw" -> password = split[1]
                        }
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            log.error("{} 파일이 없습니다. 직접 정보를 입력하세요.", filename)
            print("유동 닉네임 로그인 = 1, 반/고정 닉네임 로그인 = 0 입력 > ")
            isAnonymous = readln().toIntOrNull() != 0
            print("아이디 입력 > ")
            id = readln()
            print("비밀번호 입력 > ")
            password = readln()
            log.info("{}: {}(으)로 접속", if (isAnonymous) "유동" else "반/고정", id)
        }
    }

    private fun startServer() {
        try {
            val chatDriver = ChatDriver(isAnonymous, id, password)
            while (true) {
                val tcpServer = TCPServer(chatDriver)
                tcpServer.open()
                chatDriver.start()

                tcpServer.join()
                log.info("클라이언트가 종료됨.")
            }
        } catch (e: Exception) {
            log.error("실행 중 오류 발생", e)
            exitProcess(1)
        }
    }
}

fun main(args: Array<String>) {
    App().start()
}
