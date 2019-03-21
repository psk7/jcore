@file:Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")

package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.commands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*

/**
 * Базовый класс, представляющий пользовательский канал передачи сообщений
 */
abstract class BaseChannel(val name: String,
                           protected val controlBus: IChannel,
                           protected val data: Router,
                           protected val logger: Logger?,
                           cancellationToken: CancellationToken) {

    protected val logCat = "BaseChannel"

    private lateinit var cbEp: IChannelEndPoint

    private val _l = ConcurrentHashMap<HostID, EndPoint>()

    private val cts: CancellationTokenSource

    protected val cancToken: CancellationToken

    @Suppress("unused")
    val isClosed: Boolean
        get() = cts.isCancellationRequested

    init {
        cts = cancellationToken.getSafeToken()
        cancToken = cts.token
    }

    protected fun initComplete() {
        cbEp = controlBus.getChannel(::controlReceived)
    }

    fun getChannel(description: String? = null, received: DataReceived? = null): IChannelEndPoint = data.getChannel(received, description)

    protected abstract fun processPollCommand(command: PollCommand)

    fun close() {
        //_cts.Cancel()

        logger?.writeLog(LogImportance.Info, logCat, "Канал $name закрывается")

        cbEp.close()
    }

    protected abstract fun onHostUpdate(endPointInfo: EndPointInfo, endPoint: EndPoint)

    protected abstract fun onHostCreate(endPointInfo: EndPointInfo): EndPoint

    /**
     * Обработка управляющих команд
     */
    private fun controlReceived(@Suppress("UNUSED_PARAMETER") c: IChannelEndPoint, m: Message) {

        when (m) {
            is NewHostInChannelCommand -> newHostInChannel(m)
            is HostLeaveChannelCommand -> leaveHostInChannel(m)
            is UpdateHostInChannelCommand -> updateHostInChannel(m)
            is PollCommand -> processPollCommand(m)
        }
    }

    private fun newHostInChannel(command: NewHostInChannelCommand) {
        if (command.endPointInfo.channelName != name)
            return    // Чужие команды не обрабатываем

        val tgt = command.endPointInfo.target

        _l[tgt] = onHostCreate(command.endPointInfo) // Создание нового хоста

        logger?.writeLog(LogImportance.Info, logCat, "Хост $tgt добавлен в канал $name")

        GlobalScope.launch(Dispatchers.Unconfined) {
            command.complete.await()

            data.sendMessage(command)
        }
    }

    private fun updateHostInChannel(command: UpdateHostInChannelCommand) {
        if (command.endPointInfo.channelName != name)
            return    // Чужие команды не обрабатываем

        val v = _l[command.endPointInfo.target] ?: return

        logger?.writeLog(LogImportance.Trace, logCat, "Обновлена информация о хосте ${command.endPointInfo.target} в канале $name")

        onHostUpdate(command.endPointInfo, v)
    }

    private fun leaveHostInChannel(command: HostLeaveChannelCommand) {
        if (command.channel != name)
            return   // Чужие команды не обрабатываем

        logger?.writeLog(LogImportance.Info, logCat, "Хост ${command.leavedHost} удален из канала $name")

        _l.remove(command.leavedHost)?.close()
    }
}