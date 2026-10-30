package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

@JvmInline
value class OutputId(val index: Int)

data class Output<in T : Any>(
    val id: OutputId,
    val type: KClass<in T>,
    val location: Int,
)
