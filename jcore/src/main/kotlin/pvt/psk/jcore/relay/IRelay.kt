package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*

interface IRelay {

    /**
     * Регистрация присоединенного хоста
     *
     * @param id Идентификатор присоединяемого хоста
     * @param sink Метод отправки принятых данных присоединяемому хосту
     */
    fun addAdjacentHost(id: HostID, sink: (RelayEnvelope) -> Unit, onRouteChanged: (() -> Unit)? = null)

    /**
     * Регистрация присоединенного ретранслятора
     *
     * @param relay Присоединяемый ретранслятор
     */
    fun addUplinkRelay(relay: BaseRelay)

    /**
     *  Маршрутизация пакета от присоединенного хоста соседям/наружу
     *  В списке целей RelayEnvelope не должно быть HostID.All
     */
    fun send(data: RelayEnvelope)
}