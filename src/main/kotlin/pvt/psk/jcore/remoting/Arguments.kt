package pvt.psk.jcore.remoting

class Arguments {

    var _args: Array<Any?>

    constructor(Args: Array<Any?>) {
        _args = Args
    }

    fun unpacked(): Array<Any?> = _args
}