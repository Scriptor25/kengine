package dev.scriptor.kengine.math.simd

import java.util.*

class RegisterAllocator(
    private val instructions: List<Instruction<*, *>>,
    private val outputs: IntArray,
    private val count: Int,
) {
    private class ActiveRegister(
        val lastUse: Int,
        val physical: Int,
    )

    private val mapping = IntArray(count)
    private val types = Array(count) { RegisterType.Vector }

    private val counts = IntArray(RegisterType.entries.size)

    fun allocate(): RegisterAllocation {
        val lastUses = IntArray(count) { -1 }

        for (i in instructions.indices) {
            val instruction = instructions[i]

            for (source in instruction.sources.distinct()) {
                lastUses[source] = i
            }

            types[instruction.register.id] = instruction.type
        }

        for (output in outputs) {
            lastUses[output] = instructions.size
        }

        for (type in RegisterType.entries) {
            allocateClass(type, lastUses)
        }

        return RegisterAllocation(
            mapping,
            counts[RegisterType.Vector.ordinal],
            counts[RegisterType.Mask.ordinal],
        )
    }

    private fun allocateClass(
        type: RegisterType,
        lastUses: IntArray,
    ) {
        val free = ArrayDeque<Int>()
        val active = mutableListOf<ActiveRegister>()

        var nextPhysical = 0

        for (i in instructions.indices) {
            val instruction = instructions[i]

            val iterator = active.iterator()
            while (iterator.hasNext()) {
                val register = iterator.next()

                if (register.lastUse < i) {
                    free.addLast(register.physical)
                    iterator.remove()
                }
            }

            val destination = instruction.register.id
            if (types[destination] != type)
                continue

            val physical =
                if (free.isNotEmpty())
                    free.removeFirst()
                else
                    nextPhysical++

            mapping[destination] = physical

            active += ActiveRegister(
                lastUses[destination],
                physical,
            )

            for (source in instruction.sources.distinct()) {
                if (types[source] != type)
                    continue

                if (lastUses[source] == i) {
                    val sourcePhysical = mapping[source]

                    if (sourcePhysical != physical) {
                        free.addLast(sourcePhysical)

                        active.removeAll { it.physical == sourcePhysical }
                    }
                }
            }
        }

        counts[type.ordinal] = nextPhysical
    }
}
