package fr.delthas.keepalive.test

import fr.delthas.keepalive.Keepalive
import fr.delthas.keepalive.KeepaliveOptions
import org.junit.Assert
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class KeepaliveTest {

    @Test
    fun testKeepalive() {
        ServerSocket(1264).use { serverSocket ->
            thread(isDaemon = true) {
                serverSocket.accept()
            }

            Socket("127.0.0.1", 1264).use { socket ->
                Keepalive.setTcpKeepalive(socket, KeepaliveOptions(true, 100, 100))
                val keepalive = Keepalive.hasTcpKeepalive(socket)

                Assert.assertTrue("keepalive is enabled", keepalive)
            }
        }
    }
}
