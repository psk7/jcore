package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*

/**
 * Таблица маршрутизации
 */
interface IRoutingTable {

    /**
     * Обновляет информацию о присоединенных ретрансляторах и хостах
     *
     * Информация о начальной точке маршрута отвергается. Для ее обновления следует использовать UpdateRelayInfo(HostID[] Hosts, RelayID[] Relays)
     *
     * @param relayInfo Информация о ретрансляторе
     */
    fun updateRelayInfo(relayInfo: RelayInfo): Boolean

    /**
     * Обновляет информацию о присоединенных ретрансляторах и хостах начальной точки маршрута
     *
     * @param hosts Список присоединенных хостов
     * @param relays Список присоединенных ретрансляторов
     * @return true - если информация была обновлена, false - если переданные данные не изменились с прошлого вызова
     */
    fun updateRelayInfo(hosts: Array<HostID>, relays: Array<RelayID>): Boolean

    /**
     * Группирует список целей по идентификатору ближайшего ретранслятора, способного передать пакет
     *
     * Идентификатор отправителя не включается в список если в качестве цели указан HostID.All
     *
     * @param from Идентификатор отправителя
     * @param targets Список целей
     *
     * @return Список пар (ретранслятор - список целей для передачи этим ретранслятором)
     */
    fun expand(from: HostID, targets: HostEndpointIDSet): Collection<Pair<RelayID, HostEndpointIDSet>>

    /**
     * Список элементов таблицы маршрутизации
     */
    val table: Collection<RoutingTable.Entry>

    /**
     * Список всех известных хостов
     */
    val hosts: Collection<HostID>
}