package com.senspark.game.api

interface IVerifyIAPBillApi {
    fun verify(
        packageName: String,
        productId: String,
        billToken: String,
        transactionId: String,
        redirect: String
    ): IVerifyIapBillResult
}