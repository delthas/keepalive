@file:JvmName("KeepaliveUtils")
@file:JvmMultifileClass
@file:Suppress("FunctionName", "unused")

package fr.delthas.keepalive

import com.sun.jna.*
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import fr.delthas.keepalive.Keepalive.Companion.getTcpKeepalive
import fr.delthas.keepalive.Keepalive.Companion.hasTcpKeepalive
import fr.delthas.keepalive.Keepalive.Companion.setTcpKeepalive
import java.io.FileDescriptor
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.Socket
import java.net.SocketException
import java.net.SocketImpl

/**
 * @see Keepalive.setTcpKeepalive
 */
fun Socket.setTcpKeepalive(options: KeepaliveOptions) {
    Keepalive.setTcpKeepalive(this, options)
}

/**
 * @see Keepalive.getTcpKeepalive
 */
fun Socket.getTcpKeepalive(): KeepaliveOptions {
    return Keepalive.getTcpKeepalive(this)
}

/**
 * @see Keepalive.hasTcpKeepalive
 */
fun Socket.hasTcpKeepalive(): Boolean {
    return Keepalive.hasTcpKeepalive(this)
}

/**
 * KeepaliveOptions stores TCP keepalive options about a socket: whether TCP keepalive is enabled/disabled, idle time, and interval.
 *
 * @property enabled whether TCP keepalive is enabled or disabled
 * @property idleTime the number of milliseconds of idle time before TCP keepalive initiates a probe
 * @property interval the number of milliseconds to wait before retransmitting a TCP keepalive probe
 */
class KeepaliveOptions(var enabled: Boolean, var idleTime: Int, var interval: Int) {
    companion object {
        @JvmField val DISABLED = KeepaliveOptions(false, 0, 0)
    }
}

/**
 * Keepalive is a static-only class with static functions for using **TCP keepalive** on a [Socket].
 *
 * For enabling and disabling TCP keepalive, as well as setting the keepalive idle time and interval, use [setTcpKeepalive].
 *
 * For checking if TCP keepalive is enabled, use [hasTcpKeepalive], and for getting the keepalive idle time and interval, use [getTcpKeepalive] (not supported on Windows).
 */
class Keepalive private constructor() {
    private class CLibrary: Library {
        companion object {
            init {
                if(!windows) {
                    Native.register(Platform.C_LIBRARY_NAME)
                }
            }

            const val SOL_SOCKET = 0xFFFF
            const val SO_KEEPALIVE = 0x0008
            const val IPPROTO_TCP = 6
            const val TCP_KEEPIDLE = 4
            const val TCP_KEEPINTVL = 5

            @JvmStatic @Throws(LastErrorException::class) external fun setsockopt(socket: Int, level: Int, optionName: Int, optionValue: Pointer, optionLen: Int): Int

            @JvmStatic @Throws(LastErrorException::class) external fun getsockopt(socket: Int, level: Int, optionName: Int, optionValue: Pointer, optionLen: IntByReference): Int

            fun getKeepalive(fd: Long): KeepaliveOptions {
                val time = IntByReference()
                val timeSize = IntByReference(4)
                val interval = IntByReference()
                val intervalSize = IntByReference(4)

                var r = getsockopt(fd.toInt(), IPPROTO_TCP, TCP_KEEPIDLE, time.pointer, timeSize)
                if(r < 0) {
                    throw SocketException("get option TCP_KEEPIDLE failed")
                }

                r = getsockopt(fd.toInt(), IPPROTO_TCP, TCP_KEEPINTVL, interval.pointer, intervalSize)
                if(r < 0) {
                    throw SocketException("get option TCP_KEEPINTVL failed")
                }

                // linux uses seconds, multiply back to milliseconds
                return KeepaliveOptions(hasTcpKeepalive(fd), time.value * 1000, interval.value * 1000)
            }

            fun setKeepalive(fd: Long, options: KeepaliveOptions) {
                val keepalive = IntByReference(if(options.enabled) 1 else 0)
                // linux uses seconds, round up
                val time = IntByReference((options.idleTime + 1000 - 1) / 1000)
                val interval = IntByReference((options.interval + 1000 - 1) / 1000)

                var r = setsockopt(fd.toInt(), SOL_SOCKET, SO_KEEPALIVE, keepalive.pointer, 4)
                if(r < 0) {
                    throw SocketException("set option SO_KEEPALIVE failed")
                }

                r = setsockopt(fd.toInt(), IPPROTO_TCP, TCP_KEEPIDLE, time.pointer, 4)
                if(r < 0) {
                    throw SocketException("set option TCP_KEEPIDLE failed")
                }

                r = setsockopt(fd.toInt(), IPPROTO_TCP, TCP_KEEPINTVL, interval.pointer, 4)
                if(r < 0) {
                    throw SocketException("set option TCP_KEEPINTVL failed")
                }
            }

            fun hasTcpKeepalive(fd: Long): Boolean {
                val keepalive = IntByReference()
                val keepaliveSize = IntByReference(4)

                try {
                    getsockopt(fd.toInt(), SOL_SOCKET, SO_KEEPALIVE, keepalive.pointer, keepaliveSize)
                } catch (e: LastErrorException) {
                    throw SocketException("failed getting TCP_KEEPIDLE: ${e.message}")
                }

                return keepalive.value != 0
            }
        }
    }

    private class WinSocketLibrary: StdCallLibrary {
        companion object {
            init {
                if(windows) {
                    Native.register("Ws2_32")
                }
            }

            const val SIO_KEEPALIVE_VALS = 0x98000004.toInt()
            const val SOL_SOCKET = 0xFFFF
            const val SO_KEEPALIVE = 0x0008

            @JvmStatic @Throws(LastErrorException::class) external fun getsockopt(socket: Pointer, level: Int, optionName: Int, optionValue: Pointer, optionLen: IntByReference): Int

            @JvmStatic @Throws(LastErrorException::class) external fun WSAIoctl(socket: Pointer, controlCode: Int, inBuffer: Pointer?, inSize: Int, outBuffer: Pointer?, outSize: Int, bytesReturned: IntByReference, overlapped: Pointer?, callback: Pointer?): Int

            fun setKeepalive(fd: Long, options: KeepaliveOptions) {
                val tcpKeepalive = TcpKeepalive(if(options.enabled) 1 else 0, options.idleTime, options.interval)
                tcpKeepalive.write()
                val bytesReturned = IntByReference()
                try {
                    WSAIoctl(Pointer(fd), SIO_KEEPALIVE_VALS, tcpKeepalive.pointer, tcpKeepalive.size(), Pointer.NULL, 0, bytesReturned, Pointer.NULL, Pointer.NULL)
                } catch (e: LastErrorException) {
                    throw SocketException("failed setting TCP keepalive: ${e.message}")
                }
            }

            fun hasTcpKeepalive(fd: Long): Boolean {
                val keepalive = IntByReference()
                val keepaliveSize = IntByReference(4)
                try {
                    getsockopt(Pointer(fd), SOL_SOCKET, SO_KEEPALIVE, keepalive.pointer, keepaliveSize)
                } catch (e: LastErrorException) {
                    throw SocketException("failed getting SO_KEEPALIVE: ${e.message}")
                }
                return keepalive.value != 0
            }
        }

        @Structure.FieldOrder("onOff", "keepaliveTime", "keepaliveInterval")
        internal class TcpKeepalive(@JvmField var onOff: Int, @JvmField var keepaliveTime: Int, @JvmField var keepaliveInterval: Int) : Structure(ALIGN_MSVC)
    }

    companion object {
        private var windows: Boolean
        private var socketImplMethod: Method
        private var fileDescriptorMethod: Method
        private var fdField: Field
        private var handleField: Field

        init {
            windows = System.getProperty("os.name").startsWith("Windows")

            socketImplMethod = Socket::class.java.getDeclaredMethod("getImpl")
            socketImplMethod.isAccessible = true
            fileDescriptorMethod = SocketImpl::class.java.getDeclaredMethod("getFileDescriptor")
            fileDescriptorMethod.isAccessible = true
            fdField = FileDescriptor::class.java.getDeclaredField("fd")
            fdField.isAccessible = true
            handleField = FileDescriptor::class.java.getDeclaredField("handle")
            handleField.isAccessible = true
        }

        private fun getFd(socket: Socket): Long {
            val socketImpl = socketImplMethod.invoke(socket) as SocketImpl
            val fileDescriptor = fileDescriptorMethod.invoke(socketImpl) as FileDescriptor
            if(windows) {
                val handle = handleField.get(fileDescriptor) as Long
                if(handle != -1L) {
                    return handle
                }
            }
            return (fdField.get(fileDescriptor) as Int).toLong()
        }

        /**
         * Sets TCP keepalive options on a socket: enabled/disabled, idle time and interval.
         *
         * @throws IOException if an underlying IO error occurs when setting the TCP keepalive
         * @see KeepaliveOptions
         */
        @JvmStatic @Throws(IOException::class) fun setTcpKeepalive(socket: Socket, options: KeepaliveOptions) {
            val fd = getFd(socket)
            if(windows) {
                WinSocketLibrary.setKeepalive(fd, options)
            } else {
                CLibrary.setKeepalive(fd, options)
            }
        }

        /**
         * Gets the current TCP keepalive options on a socket: enabled/disabled, idle time and interval.
         *
         * **This is currently not supported on Windows.** You can use [hasTcpKeepalive] instead, which only returns whether keepalive is enabled.
         *
         * @throws IOException if an underlying IO error occurs when getting the TCP keepalive options
         * @see KeepaliveOptions
         */
        @JvmStatic @Throws(IOException::class) fun getTcpKeepalive(socket: Socket): KeepaliveOptions {
            val fd = getFd(socket)
            if(windows) {
                throw SocketException("getting tcp keepalive parameters is not supported on windows")
            } else {
                return CLibrary.getKeepalive(fd)
            }
        }

        /**
         * Gets whether TCP keepalive is enabled on a socket.
         *
         * To get the keepalive idle time and interval, use [getTcpKeepalive] instead (not supported on Windows).
         *
         * @throws IOException if an underlying IO error occurs when getting whether TCP keepalive is enabled
         */
        @JvmStatic @Throws(IOException::class) fun hasTcpKeepalive(socket: Socket): Boolean {
            val fd = getFd(socket)
            return if(windows) {
                WinSocketLibrary.hasTcpKeepalive(fd)
            } else {
                CLibrary.hasTcpKeepalive(fd)
            }
        }
    }
}
