package com.megaman.maverick.game.entities.protoman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.world.body.getCenter

internal class ShieldDeflects(private val onDeflect: (position: Vector2) -> Unit) {

    private class Contact(val projectile: IProjectileEntity, val velocity: Vector2, val position: Vector2) {
        var checks = 0
    }

    private val contacts = Array<Contact>()

    fun record(projectile: IProjectileEntity) {
        val body = projectile.body
        contacts.add(Contact(projectile, Vector2(body.physics.velocity), Vector2(body.getCenter())))
    }

    fun update() {
        for (index in contacts.size - 1 downTo 0) {
            val contact = contacts[index]
            when {
                deflected(contact) -> {
                    onDeflect(contact.position)
                    contacts.removeIndex(index)
                }
                ++contact.checks >= CHECKS -> contacts.removeIndex(index)
            }
        }
    }

    fun clear() = contacts.clear()

    private fun deflected(contact: Contact): Boolean {
        if (contact.checks == 0 && !contact.projectile.existenceState.liveAfterUpdate) return true
        val velocity = contact.projectile.body.physics.velocity
        return velocity.x * contact.velocity.x < 0f || velocity.y * contact.velocity.y < 0f
    }

    private companion object {
        const val CHECKS = 2
    }
}
