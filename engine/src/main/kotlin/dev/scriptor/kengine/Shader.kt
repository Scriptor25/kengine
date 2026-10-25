package dev.scriptor.kengine

import dev.scriptor.kengine.math.Vec4f
import kotlin.reflect.KClass
import kotlin.reflect.cast

interface ShaderContext {

    operator fun get(id: UniformId): Any
    operator fun get(id: InputId): Any
    operator fun set(id: OutputId, value: Any)
}

@JvmInline
value class UniformId(val index: Int)

@JvmInline
value class InputId(val index: Int)

@JvmInline
value class OutputId(val index: Int)

data class Uniform<T : Any>(
    val id: UniformId,
    val type: KClass<T>,
) {
    context(context: ShaderContext)
    operator fun invoke(): T = type.cast(context[id])
}

data class Input<T : Any>(
    val id: InputId,
    val type: KClass<T>,
    val location: Int,
) {
    context(context: ShaderContext)
    operator fun invoke(): T = type.cast(context[id])
}

data class Output<T : Any>(
    val id: OutputId,
    val type: KClass<T>,
    val location: Int,
) {
    context(context: ShaderContext)
    operator fun invoke(value: T) {
        context[id] = value
    }
}

class Shader(
    val uniforms: Array<Uniform<*>>,
    val inputs: Array<Input<*>>,
    val outputs: Array<Output<*>>,
    val main: context(ShaderContext) () -> Unit,
)

open class ShaderBuilder {
    private val uniforms = mutableListOf<Uniform<*>>()
    private val inputs = mutableListOf<Input<*>>()
    private val outputs = mutableListOf<Output<*>>()

    private var nextInputLocation = 0
    private var nextOutputLocation = 0

    private lateinit var main: context(ShaderContext) () -> Unit

    internal fun build(): Shader = Shader(
        uniforms.toTypedArray(),
        inputs.toTypedArray(),
        outputs.toTypedArray(),
        main,
    )

    fun <T : Any> uniform(type: KClass<T>): Uniform<T> {
        val uniform = Uniform(
            UniformId(uniforms.size),
            type,
        )

        uniforms += uniform
        return uniform
    }

    fun <T : Any> input(type: KClass<T>, location: Int): Input<T> {
        nextInputLocation = location + 1

        val input = Input(
            InputId(inputs.size),
            type,
            location,
        )

        inputs += input
        return input
    }

    fun <T : Any> output(type: KClass<T>, location: Int): Output<T> {
        nextOutputLocation = location + 1

        val output = Output(
            OutputId(outputs.size),
            type,
            location,
        )

        outputs += output
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

    fun main(block: context(ShaderContext) () -> Unit) {
        main = block
    }
}

fun shader(block: ShaderBuilder.() -> Unit): Shader {
    return ShaderBuilder().apply(block).build()
}

class VertexShader(
    val shader: Shader,
    val position: Output<Vec4f>,
)

class VertexShaderBuilder : ShaderBuilder() {

    val position = output<Vec4f>(-1)

    internal fun buildVertex(): VertexShader = VertexShader(
        build(),
        position,
    )
}

fun vertexShader(block: VertexShaderBuilder.() -> Unit): VertexShader {
    return VertexShaderBuilder().apply(block).buildVertex()
}

class FragmentShader(
    val shader: Shader,
    val color: Output<Vec4f>,
)

class FragmentShaderBuilder : ShaderBuilder() {

    val color = output<Vec4f>(-1)

    internal fun buildFragment(): FragmentShader = FragmentShader(
        build(),
        color,
    )
}

fun fragmentShader(block: FragmentShaderBuilder.() -> Unit): FragmentShader {
    return FragmentShaderBuilder().apply(block).buildFragment()
}
