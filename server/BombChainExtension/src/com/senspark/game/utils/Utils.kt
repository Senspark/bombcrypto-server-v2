package com.senspark.game.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import java.lang.reflect.Type
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object Utils {
    val gson: Gson = Gson()

    @Throws(JsonSyntaxException::class)
    fun <T> jsonSerialize(json: String, typeOfT: Type): T {
        return gson.fromJson(json, typeOfT)
    }

    fun randInt(min: Int, max: Int): Int {
        val rand = Random()
        return rand.nextInt((max - min) + 1) + min
    }

    fun subTwoDates(from: Date?, to: Date?, timeUnit: TimeUnit): Long {
        if (from == null || to == null) {
            return 0
        }
        val diffInMillies = kotlin.math.abs(to.time - from.time)
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS)
    }

    fun parseSQLException(ex: SQLException): CustomException {
        val message = ex.message?.split("\n")?.get(0)?.split(",") ?: return CustomException("Unknown exception", ErrorCode.SERVER_ERROR)
        if (message.isEmpty()) {
            return CustomException("Unknown exception", ErrorCode.SERVER_ERROR)
        }
        if (message.size == 1) {
            return CustomException(message[0], ErrorCode.SERVER_ERROR)
        }
        val errorCode = message[0].toIntOrNull() ?: ErrorCode.SERVER_ERROR
        return CustomException(message[1], errorCode, true)
    }

    fun roundAvoid(value: Float, places: Int): Double {
        val scale = Math.pow(10.0, places.toDouble())
        return Math.round(value * scale) / scale
    }

    fun compare2DateOfYear(cal1: Calendar, cal2: Calendar): Boolean {
        val date1 = cal1.get(Calendar.DAY_OF_YEAR)
        val date2 = cal2.get(Calendar.DAY_OF_YEAR)
        return date1 == date2
    }

    fun checkValidEmail(emailAddress: String): Boolean {
        val regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"
        return Pattern.compile(regexPattern)
            .matcher(emailAddress)
            .matches()
    }
}