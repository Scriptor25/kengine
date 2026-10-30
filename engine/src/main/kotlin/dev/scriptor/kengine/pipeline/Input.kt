package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

@JvmInline
value class InputId(val index: Int)

data class Input<out T : Any>(
    val id: InputId,
    val type: KClass<out T>,
    val location: Int,
)
