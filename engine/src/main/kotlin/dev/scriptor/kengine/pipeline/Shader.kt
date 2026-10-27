package dev.scriptor.kengine.pipeline

import kotlin.reflect.KProperty
import kotlin.reflect.cast

abstract class Shader<L : ShaderLayout>(
    val layout: L,
    private val context: ShaderContext,
) {
    interface Delegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    }

    interface MutableDelegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    }

    fun <T : Any> use(field: Uniform<T>): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }
    }

    fun <T : Any> use(field: Uniform<T>, index: Int): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast((context[field.id] as Array<*>)[index])
        }
    }

    fun <T : Any> use(field: Uniform<T>, index: () -> Int): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast((context[field.id] as Array<*>)[index()])
        }
    }

    fun <T : Any> use(field: Input<T>): Delegate<T> = object : Delegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }
    }

    fun <T : Any> use(field: Output<T>): MutableDelegate<T> = object : MutableDelegate<T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return field.type.cast(context[field.id])
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            context[field.id] = value
        }
    }

    abstract fun main()
}
