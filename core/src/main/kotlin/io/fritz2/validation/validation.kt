package io.fritz2.validation

import io.fritz2.flow.asSharedFlow
import io.fritz2.optics.withId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface ValidationMessage: withId {
    fun failed(): Boolean
}

@FlowPreview
@ExperimentalCoroutinesApi
abstract class Validator<D, M: ValidationMessage, T> {

    internal val channel = ConflatedBroadcastChannel<List<M>>()
    val msgs = channel.asFlow().distinctUntilChanged().asSharedFlow()

    abstract fun validate(data: D, metadata: T): List<M>

    val isValid by lazy {msgs.map { list -> list.none(ValidationMessage::failed)}}
}

@FlowPreview
@ExperimentalCoroutinesApi
interface Validation<D, M: ValidationMessage, T> {

    val validator: Validator<D, M, T>

    fun validate(data: D, metadata: T): Boolean {
        val messages = validator.validate(data, metadata)
        validator.channel.offer(messages)
        return messages.none(ValidationMessage::failed)
    }

    fun msgs(): Flow<List<M>> = validator.msgs
}