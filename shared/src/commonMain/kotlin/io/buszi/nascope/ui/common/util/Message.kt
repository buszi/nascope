package io.buszi.nascope.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Stable
sealed interface Message {

    suspend fun getText(environment: ResourceEnvironment): String

    @Composable
    fun getText(): String

    @ConsistentCopyVisibility
    data class Plain internal constructor(val text: String) : Message {

        override suspend fun getText(environment: ResourceEnvironment): String = text

        @Composable
        override fun getText(): String = text
    }

    @ConsistentCopyVisibility
    data class Resource internal constructor(val res: StringResource) : Message {

        override suspend fun getText(environment: ResourceEnvironment): String =
            getString(environment, res)

        @Composable
        override fun getText(): String = stringResource(res)
    }

    @ConsistentCopyVisibility
    @Immutable
    data class ResourceParametrized internal constructor(
        val res: StringResource,
        val params: List<Any>,
    ) : Message {

        override suspend fun getText(environment: ResourceEnvironment): String =
            getString(environment, res, *params.toTypedArray())

        @Composable
        override fun getText(): String = stringResource(res, *params.toTypedArray())
    }

    companion object {
        fun plain(text: String): Message = Plain(text)
        fun resource(res: StringResource): Message = Resource(res)
        fun resource(res: StringResource, vararg params: Any): Message =
            ResourceParametrized(res, params.toList())
    }
}
