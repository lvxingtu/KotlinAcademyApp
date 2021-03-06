package com.marcinmoskala.kotlinacademy.backend.db

import com.marcinmoskala.kotlinacademy.data.Feedback
import com.marcinmoskala.kotlinacademy.data.News
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.log
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.run
import org.jetbrains.squash.connection.DatabaseConnection
import org.jetbrains.squash.connection.Transaction
import org.jetbrains.squash.connection.transaction
import org.jetbrains.squash.dialects.h2.H2Connection
import org.jetbrains.squash.expressions.eq
import org.jetbrains.squash.query.orderBy
import org.jetbrains.squash.query.select
import org.jetbrains.squash.query.where
import org.jetbrains.squash.results.get
import org.jetbrains.squash.statements.insertInto
import org.jetbrains.squash.statements.set
import org.jetbrains.squash.statements.update
import org.jetbrains.squash.statements.values
import kotlin.coroutines.experimental.CoroutineContext

class Database(application: Application) {
    private val dispatcher: CoroutineContext
    private val connectionPool: HikariDataSource
    private val connection: DatabaseConnection

    init {
        val config = application.environment.config.config("database")
        val url = config.property("connection").getString()
        val poolSize = config.property("poolSize").getString().toInt()
        application.log.info("Connecting to database at '$url'")

        dispatcher = newFixedThreadPoolContext(poolSize, "database-pool")
        val cfg = HikariConfig().apply {
            jdbcUrl = url
            maximumPoolSize = poolSize
            validate()
        }
        connectionPool = HikariDataSource(cfg)
        connection = H2Connection { connectionPool.connection }.apply {
            transaction { databaseSchema().create(listOf(NewsTable, FeedbackTable, TokensTable)) }
        }
    }

    suspend fun getNews(): List<News> = run(dispatcher) {
        connection.transaction {
            NewsTable.select(NewsTable.id, NewsTable.title, NewsTable.subtitle, NewsTable.imageUrl, NewsTable.url)
                    .orderBy(ascending = false) { NewsTable.id }
                    .execute()
                    .map {
                        News(
                                id = it[NewsTable.id],
                                title = it[NewsTable.title],
                                subtitle = it[NewsTable.subtitle],
                                imageUrl = it[NewsTable.imageUrl],
                                url = it[NewsTable.url]
                        )
                    }.toList()
                    .reversed()
        }
    }

    suspend fun getComments() = run(dispatcher) {
        connection.transaction {
            FeedbackTable.select(FeedbackTable.newsId, FeedbackTable.rating, FeedbackTable.commentText, FeedbackTable.suggestionsText)
                    .execute()
                    .distinct()
                    .map { Feedback(it[FeedbackTable.newsId], it[FeedbackTable.rating], it[FeedbackTable.commentText], it[FeedbackTable.suggestionsText]) }
                    .toList()
        }
    }

    suspend fun updateOrAdd(news: News) {
        connection.transaction {
            val id = news.id
            if (id == null) {
                add(news)
            } else {
                when (countNewsWithId(id)) {
                    0 -> add(news)
                    1 -> update(id, news)
                    else -> throw Error("More then single element with id ${news.id} in the database")
                }
            }
        }
    }

    suspend fun add(feedback: Feedback) {
        connection.transaction {
            insertInto(FeedbackTable).values {
                it[newsId] = feedback.newsId
                it[rating] = feedback.rating
                it[commentText] = feedback.comment
                it[suggestionsText] = feedback.suggestions
            }.execute()
        }
    }

    suspend fun getWebTokens(): List<String> = run(dispatcher) {
        connection.transaction {
            TokensTable.select(TokensTable.token)
                    .where { TokensTable.type.eq(TokensTable.Types.Web.name) }
                    .execute()
                    .map { it[TokensTable.token] }
                    .toList()
        }
    }

    suspend fun addWebToken(tokenText: String) {
        connection.transaction {
            insertInto(TokensTable).values {
                it[type] = TokensTable.Types.Web.name
                it[token] = tokenText
            }.execute()
        }
    }

    private fun Transaction.countNewsWithId(id: Int) = NewsTable.select(NewsTable.id)
            .where { NewsTable.id.eq(id) }
            .execute()
            .count()

    private fun Transaction.update(id: Int, news: News) {
        update(NewsTable)
                .where { NewsTable.id eq id }
                .set {
                    it[title] = news.title
                    it[subtitle] = news.subtitle
                    it[imageUrl] = news.imageUrl
                    it[url] = news.url
                }.execute()
    }

    private fun Transaction.add(news: News) {
        insertInto(NewsTable).values {
            it[title] = news.title
            it[subtitle] = news.subtitle
            it[imageUrl] = news.imageUrl
            it[url] = news.url
        }.execute()
    }
}