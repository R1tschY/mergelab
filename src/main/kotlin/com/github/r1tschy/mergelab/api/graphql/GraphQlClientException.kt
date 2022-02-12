package com.github.r1tschy.mergelab.api.graphql

import com.expediagroup.graphql.client.types.GraphQLClientError
import com.github.r1tschy.mergelab.exceptions.GitLabException

/**
 * GraphQL error.
 */
class GraphQlClientException(private val _errors: List<GraphQLClientError>) : GitLabException() {

    val errors: List<GraphQLClientError> = _errors

    override val message: String
        get() {
            val messages: List<String> = _errors.map { error ->
                val path = error.path?.joinToString("/") ?: "???"
                val locations = error.locations?.joinToString(":", prefix = ":") { loc -> "${loc.line}: ${loc.column}" }
                "$path$locations: ${error.message}"
            }

            return "GitLab GraphQL query error: " + messages.joinToString("\n")
        }
}