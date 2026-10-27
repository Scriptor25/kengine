package dev.scriptor.kengine.pipeline

import kotlin.reflect.KClass

abstract class ShaderLayout {

    private val uniformsList = mutableListOf<Uniform<*>>()
    private val inputsList = mutableListOf<Input<*>>()
    private val outputsList = mutableListOf<Output<*>>()

    private var nextInputLocation = 0
    private var nextOutputLocation = 0

    val uniforms: List<Uniform<*>>
        get() = uniformsList
    val inputs: List<Input<*>>
        get() = inputsList
    val outputs: List<Output<*>>
        get() = outputsList

    fun <T : Any> uniform(type: KClass<T>): Uniform<T> {
        val uniform = Uniform(
            UniformId(uniformsList.size),
            type,
        )

        uniformsList += uniform
        return uniform
    }

    fun <T : Any> input(type: KClass<T>, location: Int): Input<T> {
        nextInputLocation = location + 1

        val input = Input(
            InputId(inputsList.size),
            type,
            location,
        )

        inputsList += input
        return input
    }

    fun <T : Any> output(type: KClass<T>, location: Int): Output<T> {
        nextOutputLocation = location + 1

        val output = Output(
            OutputId(outputsList.size),
            type,
            location,
        )

        outputsList += output
        return output
    }

    fun <T : Any> input(type: KClass<T>): Input<T> {
        return input(type, nextInputLocation)
    }

    fun <T : Any> output(type: KClass<T>): Output<T> {
        return output(type, nextOutputLocation)
    }

    inline fun <reified T : Any> input(location: Int): Input<T> =
        input(T::class, location)

    inline fun <reified T : Any> output(location: Int): Output<T> =
        output(T::class, location)

    inline fun <reified T : Any> uniform(): Uniform<T> =
        uniform(T::class)

    inline fun <reified T : Any> input(): Input<T> =
        input(T::class)

    inline fun <reified T : Any> output(): Output<T> =
        output(T::class)
}
