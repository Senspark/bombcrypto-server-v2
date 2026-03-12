package com.senspark.game.manager.pvp

class MatchExpiredException : Exception()
class InvalidMatchHashException(message: String) : Exception(message)
class InvalidMatchServerException : Exception()