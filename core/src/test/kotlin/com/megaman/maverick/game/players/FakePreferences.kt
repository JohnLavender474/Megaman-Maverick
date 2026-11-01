package com.megaman.maverick.game.players

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties

// Behaves as Lwjgl3Preferences does: changes stay in memory until flush(), and a read parses with no guard.
class FakePreferences : Preferences {

    private val memory = Properties()
    private var disk: ByteArray? = null

    var flushCount = 0
        private set

    val keys: Set<String>
        get() = memory.stringPropertyNames()

    val onDisk: Boolean
        get() = disk != null

    fun put(key: String, value: String) {
        memory.setProperty(key, value)
    }

    fun reopen() {
        memory.clear()
        disk?.let { memory.loadFromXML(ByteArrayInputStream(it)) }
    }

    override fun putBoolean(key: String, value: Boolean) = apply { memory.setProperty(key, value.toString()) }

    override fun putInteger(key: String, value: Int) = apply { memory.setProperty(key, value.toString()) }

    override fun putLong(key: String, value: Long) = apply { memory.setProperty(key, value.toString()) }

    override fun putFloat(key: String, value: Float) = apply { memory.setProperty(key, value.toString()) }

    override fun putString(key: String, value: String) = apply { memory.setProperty(key, value) }

    override fun put(vals: MutableMap<String, *>) = apply { vals.forEach { (k, v) -> memory.setProperty(k, "$v") } }

    override fun getBoolean(key: String) = getBoolean(key, false)

    override fun getInteger(key: String) = getInteger(key, 0)

    override fun getLong(key: String) = getLong(key, 0L)

    override fun getFloat(key: String) = getFloat(key, 0f)

    override fun getString(key: String) = getString(key, "")

    override fun getBoolean(key: String, defValue: Boolean) = memory.getProperty(key, "$defValue").toBoolean()

    override fun getInteger(key: String, defValue: Int) = Integer.parseInt(memory.getProperty(key, "$defValue"))

    override fun getLong(key: String, defValue: Long) = memory.getProperty(key, "$defValue").toLong()

    override fun getFloat(key: String, defValue: Float) = memory.getProperty(key, "$defValue").toFloat()

    override fun getString(key: String, defValue: String): String = memory.getProperty(key, defValue)

    override fun get(): MutableMap<String, *> = HashMap(memory.stringPropertyNames().associateWith { memory[it] })

    override fun contains(key: String) = memory.containsKey(key)

    override fun clear() = memory.clear()

    override fun remove(key: String) {
        memory.remove(key)
    }

    override fun flush() {
        val out = ByteArrayOutputStream()
        memory.storeToXML(out, null)
        disk = out.toByteArray()
        flushCount++
    }
}

class FakePreferencesFiles {

    val opened = ArrayList<String>()

    private val files = HashMap<String, FakePreferences>()

    fun file(name: String) = files.getOrPut(name) { FakePreferences() }

    fun install() {
        Gdx.app = mockk<Application>(relaxed = true) {
            every { getPreferences(any()) } answers {
                val name = firstArg<String>()
                opened.add(name)
                file(name)
            }
        }
    }
}
