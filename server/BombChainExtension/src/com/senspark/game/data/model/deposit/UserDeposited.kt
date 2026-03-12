package com.senspark.game.data.model.deposit

class UserDeposited(
    val bcoinDeposited: Float,
    val senDeposited: Float
) {
    constructor(bcoinDeposited: Float) : this(bcoinDeposited, 0f)
}