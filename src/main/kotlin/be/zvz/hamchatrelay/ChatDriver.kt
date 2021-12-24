package be.zvz.hamchatrelay

import be.zvz.kotlininside.KotlinInside
import be.zvz.kotlininside.api.async.article.AsyncArticleWrite
import be.zvz.kotlininside.api.type.Article
import be.zvz.kotlininside.api.type.content.MarkdownContent
import be.zvz.kotlininside.http.DefaultHttpClient
import be.zvz.kotlininside.session.user.Anonymous
import be.zvz.kotlininside.session.user.LoginUser
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ChatDriver(
    isAnonymous: Boolean,
    private val userId: String,
    private val password: String
) {
    private val log: Logger = LoggerFactory.getLogger(ChatDriver::class.java)
    private val chatQueue: Queue<String> = LinkedList()
    private var interrupt = false

    init {
        KotlinInside.createInstance(
            if (isAnonymous) {
                Anonymous(userId, password)
            } else {
                LoginUser(userId, password)
            },
            DefaultHttpClient(),
            true
        )
        log.info("ChatDriver initialized: ${SLEEP_DURATION}초 대기")
        Thread.sleep(SLEEP_DURATION)
    }

    fun enqueueMessage(msg: String) {
        chatQueue.add(msg)
    }

    private suspend fun writePost(title: String, content: String) {
        log.info("글 작성: $title")
        val writeResult = AsyncArticleWrite(
            GALLERY_NAME,
            Article(
                title,
                listOf(
                    MarkdownContent(content)
                )
            ),
            KotlinInside.getInstance().session
        ).writeAsync().await()
        if (writeResult.result) {
            log.info("글 작성 성공: $title")
        } else {
            log.info("글 작성 실패: ${writeResult.cause}")
        }
    }

    private suspend fun doSleep() {
        val postDelay = SLEEP_DURATION + (Math.random() * 5000)
        log.info("도배 방지를 위해 ${postDelay}초 대기...")
        delay(postDelay.toLong())
    }

    fun start() {
        CoroutineScope(Dispatchers.Default).launch {
            while (!interrupt) {
                if (chatQueue.size > 0) {
                    val (title, content) = getTitleAndContent()
                    async(Dispatchers.IO) {
                        writePost(title, content)
                    }.start()
                    doSleep()
                }
            }
        }
    }

    private fun getTitleAndContent(): Pair<String, String> {
        val firstMessage = chatQueue.poll()
        val content = StringBuilder(" - $firstMessage")
        val title = StringBuilder(firstMessage).apply {
            while (chatQueue.size > 0) {
                if (length < 10) {
                    val nextMessage = chatQueue.poll()
                    if (length < 30) {
                        append(' ').append(nextMessage)
                    }
                    content.append("\n - ").append(nextMessage)
                }
            }
        }.toString()
        return Pair(title, content.toString())
    }

    companion object {
        const val SLEEP_DURATION = 10 * 1000L
        const val GALLERY_NAME = "haruhiism"
    }
}
