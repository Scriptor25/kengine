package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

@JvmInline
value class UniformId(val index: Int)

data class Uniform<T : Any>(
    val id: UniformId,
    val type: KClass<T>,
)
