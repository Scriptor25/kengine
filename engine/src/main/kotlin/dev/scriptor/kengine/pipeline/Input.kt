package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

@JvmInline
value class InputId(val index: Int)

data class Input<T : Any>(
    val id: InputId,
    val type: KClass<T>,
    val location: Int,
)
