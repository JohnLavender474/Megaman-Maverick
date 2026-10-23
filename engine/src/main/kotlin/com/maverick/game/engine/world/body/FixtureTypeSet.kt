package com.maverick.game.engine.world.body

class FixtureTypeSet private constructor(private val types: Array<IFixtureType>) : Iterable<IFixtureType> {

    companion object {

        fun <E> of(entries: List<E>) where E : Enum<E>, E : IFixtureType = validated(entries)

        fun validated(types: List<IFixtureType>): FixtureTypeSet {
            types.forEachIndexed { position, type ->
                require(type.index == position) {
                    "Fixture type $type has index ${type.index}, but it is at position $position"
                }
            }
            return FixtureTypeSet(types.toTypedArray())
        }

        fun named(vararg names: String) = FixtureTypeSet(Array(names.size) { NamedFixtureType(names[it], it) })
    }

    val size: Int
        get() = types.size

    operator fun get(index: Int) = types[index]

    fun owns(type: IFixtureType) = type.index in types.indices && types[type.index] === type

    override fun iterator() = types.iterator()

    private class NamedFixtureType(private val name: String, override val index: Int) : IFixtureType {

        override fun toString() = name
    }
}
