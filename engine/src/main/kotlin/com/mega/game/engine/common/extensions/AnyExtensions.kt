package com.mega.game.engine.common.extensions

import kotlin.reflect.KClass

fun Any.isAny(vararg clazzes: KClass<out Any>): Boolean = clazzes.any { it.isInstance(this) }

fun Any.isAny(vararg clazzes: Class<out Any>): Boolean = clazzes.any { it.isInstance(this) }

fun Any.equalsAny(vararg objects: Any): Boolean = objects.any { this == it }

fun Any.equalsAll(objects: Iterable<Any>): Boolean = objects.all { this == it }

fun Any.equalsNone(vararg objects: Any): Boolean = objects.none { this == it }

fun Any.equalsCount(objects: Iterable<Any>): Int = objects.count { this == it }

fun Any.hashCodeAny(vararg objects: Any): Boolean = objects.any { this.hashCode() == it.hashCode() }

fun Any.hashCodeAll(objects: Iterable<Any>): Boolean =
    objects.all { this.hashCode() == it.hashCode() }

fun Any.hashCodeNone(vararg objects: Any): Boolean =
    objects.none { this.hashCode() == it.hashCode() }

fun Any.hashCodeCount(objects: Iterable<Any>): Int =
    objects.count { this.hashCode() == it.hashCode() }
