package com.megaman.maverick.game.world.contacts.rules

import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.IContactRule
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.decorations.Splash.SplashType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.world.body.*

object ProjectileRule : IContactRule {

    override fun onBegin(a: IFixture, b: IFixture, delta: Float) = begin(a, b)

    private fun begin(projectileFixture: IFixture, otherFixture: IFixture) {
        val projectile1 = projectileFixture.getEntity() as IProjectileEntity

        if (otherFixture.hasFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION) &&
            otherFixture.receiver(Receivers.NO_PROJECTILE_COLLISION_EXCEPTION)
                ?.invoke(projectile1, projectileFixture) != true
        ) return

        otherFixture.receiver(Receivers.HIT_BY_PROJECTILE)?.invoke(projectile1)

        val thisShape = projectileFixture.getShape().copy()
        val otherShape = otherFixture.getShape().copy()
        when (otherFixture.getType()) {
            FixtureType.BLOCK -> {
                // If the body has a "shield" property set to true, then the projectile should ignore
                // the block fixture and instead make contact only with the other body's shield fixture.
                // IMPORTANT: If the body does NOT have a shield fixture, then nothing will trigger.
                val shielded = otherFixture.getBody().isProperty(ConstKeys.SHIELD, true)
                if (shielded) return

                projectile1.hitBlock(otherFixture, thisShape, otherShape)
                (otherFixture.getEntity() as Block).hitByProjectile(projectileFixture)
            }

            FixtureType.BODY -> {
                projectile1.hitBody(otherFixture, thisShape, otherShape)
            }

            FixtureType.SHIELD -> {
                projectile1.hitShield(otherFixture, thisShape, otherShape)
            }

            FixtureType.WATER -> {
                projectile1.hitWater(otherFixture, thisShape, otherShape)
            }

            FixtureType.SAND -> {
                projectile1.hitSand(otherFixture, thisShape, otherShape)
                val splash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)!!

                val overlap = GameObjectPools.fetch(Rectangle::class)
                Intersector.intersectRectangles(
                    projectileFixture.getShape().toGdxRectangle(),
                    otherFixture.getShape().toGdxRectangle(),
                    overlap
                )

                val position = overlap.getCenter()
                splash.spawn(
                    props(
                        ConstKeys.TYPE pairTo SplashType.SAND,
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.SOUND pairTo true
                    )
                )
            }

            FixtureType.PROJECTILE -> {
                projectile1.hitProjectile(otherFixture, thisShape, otherShape)

                val projectile2 = otherFixture.getEntity() as IProjectileEntity
                projectile2.hitProjectile(projectileFixture, otherShape, thisShape)
            }

            FixtureType.EXPLOSION -> {
                projectile1.hitExplosion(otherFixture, otherShape, thisShape)
            }
        }
    }
}
