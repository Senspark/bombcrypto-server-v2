package com.senspark.common

import com.senspark.common.service.IGlobalService
import com.smartfoxserver.v2.entities.data.ISFSArray
import java.sql.ResultSet
import org.jetbrains.exposed.sql.Database

interface IQueryBuilder {
    fun addStatement(statement: String, args: Array<Any?>): IQueryBuilder
    fun executeQuery(): ISFSArray
    fun executeUpdate()
    fun executeMultiQuery()
    fun addStatementUpdate(
        statement: String,
        args: Array<Any?>
    ): IQueryBuilder

    fun <T : Any> executeQuery(transform: (ResultSet) -> T)
}

interface IDatabase : IGlobalService {
    val database: Database
    fun createQueryBuilder(enableLog: Boolean = false): IQueryBuilder
}