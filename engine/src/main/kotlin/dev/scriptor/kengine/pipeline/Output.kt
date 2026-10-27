package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

@JvmInline
value class OutputId(val index: Int)

data class Output<T : Any>(
    val id: OutputId,
    val type: KClass<T>,
    val location: Int,
)
