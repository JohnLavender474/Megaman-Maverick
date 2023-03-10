package com.megaman.game.entities.impl.enemies.impl;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectMap;
import com.megaman.game.ConstKeys;
import com.megaman.game.MegamanGame;
import com.megaman.game.ViewVals;
import com.megaman.game.animations.Animation;
import com.megaman.game.animations.AnimationComponent;
import com.megaman.game.assets.TextureAsset;
import com.megaman.game.entities.utils.damage.DamageNegotiation;
import com.megaman.game.entities.utils.damage.Damager;
import com.megaman.game.entities.utils.faceable.Faceable;
import com.megaman.game.entities.utils.faceable.Facing;
import com.megaman.game.entities.impl.enemies.Enemy;
import com.megaman.game.entities.impl.explosions.impl.ChargedShotExplosion;
import com.megaman.game.entities.impl.megaman.Megaman;
import com.megaman.game.entities.impl.projectiles.Projectile;
import com.megaman.game.entities.impl.projectiles.impl.Bullet;
import com.megaman.game.entities.impl.projectiles.impl.ChargedShot;
import com.megaman.game.entities.impl.projectiles.impl.Fireball;
import com.megaman.game.health.HealthVals;
import com.megaman.game.shapes.ShapeComponent;
import com.megaman.game.shapes.ShapeHandle;
import com.megaman.game.shapes.ShapeUtils;
import com.megaman.game.sprites.SpriteComponent;
import com.megaman.game.sprites.SpriteHandle;
import com.megaman.game.updatables.UpdatableComponent;
import com.megaman.game.utils.enums.Position;
import com.megaman.game.utils.interfaces.UpdateFunc;
import com.megaman.game.utils.objs.Timer;
import com.megaman.game.world.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import static com.badlogic.gdx.graphics.Color.GRAY;

public class MagFly extends Enemy implements Faceable {

    private static final float FORCE_FLASH_DURATION = .1f;
    private static final float X_VEL_NORMAL = 3f;
    private static final float X_VEL_SLOW = 1f;
    private static final float PULL_FORCE_X = 6f;
    private static final float PULL_FORCE_Y = 68f;

    private static TextureRegion magFlyReg;

    private final Sprite sprite;
    private final Timer forceFlashTimer;

    private Fixture forceFixture;

    private boolean flash;
    @Getter
    @Setter
    private Facing facing;

    public MagFly(MegamanGame game) {
        super(game, BodyType.DYNAMIC);
        if (magFlyReg == null) {
            magFlyReg = game.getAssMan().getTextureRegion(TextureAsset.ENEMIES_1, "MagFly");
        }
        defineBody();
        sprite = new Sprite();
        forceFlashTimer = new Timer(FORCE_FLASH_DURATION);
        if (MegamanGame.DEBUG) {
            putComponent(shapeComponent());
        }
        putComponent(spriteComponent());
        putComponent(animationComponent());
    }

    @Override
    public void init(Rectangle bounds, ObjectMap<String, Object> data) {
        Vector2 spawn = ShapeUtils.getCenterPoint(bounds);
        body.bounds.setCenter(spawn);
    }

    @Override
    protected Map<Class<? extends Damager>, DamageNegotiation> defineDmgNegs() {
        return new HashMap<>() {{
            put(Bullet.class, new DamageNegotiation(10));
            put(Fireball.class, new DamageNegotiation(HealthVals.MAX_HEALTH));
            put(ChargedShot.class, new DamageNegotiation(HealthVals.MAX_HEALTH));
            put(ChargedShotExplosion.class, new DamageNegotiation(HealthVals.MAX_HEALTH));
        }};
    }

    protected void defineBody() {
        body.bounds.setSize(WorldVals.PPM);
        Fixture bodyFixture = new Fixture(this, FixtureType.BODY, new Rectangle().setSize(WorldVals.PPM));
        body.add(bodyFixture);
        Fixture leftFixture = new Fixture(this, FixtureType.SIDE, new Rectangle().setSize(.1f * WorldVals.PPM));
        leftFixture.putUserData(ConstKeys.SIDE, ConstKeys.LEFT);
        leftFixture.offset.x = -.6f * WorldVals.PPM;
        leftFixture.offset.y = -.4f * WorldVals.PPM;
        body.add(leftFixture);
        Fixture rightFixture = new Fixture(this, FixtureType.SIDE, new Rectangle().setSize(.1f * WorldVals.PPM));
        rightFixture.putUserData(ConstKeys.SIDE, ConstKeys.RIGHT);
        rightFixture.offset.x = .6f * WorldVals.PPM;
        rightFixture.offset.y = -.4f * WorldVals.PPM;
        body.add(rightFixture);
        Fixture forceFixture = new Fixture(this, FixtureType.FORCE,
                new Rectangle().setSize(WorldVals.PPM / 2f, ViewVals.VIEW_HEIGHT * WorldVals.PPM));
        forceFixture.offset.y = -ViewVals.VIEW_HEIGHT * WorldVals.PPM / 2f;

        // TODO: test

        /*
        Function<Fixture, Vector2> forceFunc = f -> {
            if (f.entity instanceof Enemy || (f.entity instanceof Megaman m && m.isDamaged())) {
                return Vector2.Zero;
            }
            if (f.entity instanceof Projectile p) {
                p.owner = null;
            }
            float x = PULL_FORCE_X * WorldVals.PPM;
            if (is(Facing.LEFT)) {
                x *= -1f;
            }
            float y = PULL_FORCE_Y * WorldVals.PPM;
            return new Vector2(x, y);
        };
         */

        /*
        Function<Float, Vector2> forceFunc = delta -> {
            float x = PULL_FORCE_X * WorldVals.PPM;
            if (is(Facing.LEFT)) {
                x *= -1f;
            }
            float y = PULL_FORCE_Y * WorldVals.PPM;
            return new Vector2(x, y).scl(delta);
        };
         */

        UpdateFunc<Fixture, Vector2> forceFunc = (f, delta) -> {
            if (f.entity instanceof Enemy || (f.entity instanceof Megaman m && m.isDamaged())) {
                return Vector2.Zero;
            }
            if (f.entity instanceof Projectile p) {
                p.owner = null;
            }
            float x = PULL_FORCE_X * WorldVals.PPM;
            if (is(Facing.LEFT)) {
                x *= -1f;
            }
            float y = PULL_FORCE_Y * WorldVals.PPM;
            return new Vector2(x, y).scl(delta);
        };

        forceFixture.putUserData(ConstKeys.FUNCTION, forceFunc);

        body.add(forceFixture);
        this.forceFixture = forceFixture;
        Fixture damageableFixture = new Fixture(this, FixtureType.DAMAGEABLE, new Rectangle().setSize(WorldVals.PPM));
        body.add(damageableFixture);
        Fixture damagerFixture = new Fixture(this, FixtureType.DAMAGER, new Rectangle().setSize(.85f * WorldVals.PPM));
        body.add(damagerFixture);
    }

    @Override
    protected void defineUpdateComponent(UpdatableComponent c) {
        super.defineUpdateComponent(c);
        c.add(delta -> {
            forceFlashTimer.update(delta);
            if (forceFlashTimer.isFinished()) {
                flash = !flash;
                forceFlashTimer.reset();
            }
            boolean slow = isMegamanOverlappingForceFixture();
            if (!slow && !isMegamanAbove() && !doFacingAndMMDirMatch()) {
                setFacing(isMegamanRight() ? Facing.RIGHT : Facing.LEFT);
            }
            if ((is(Facing.LEFT) && is(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (is(Facing.RIGHT) && is(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))) {
                body.velocity.x = 0f;
            } else {
                body.velocity.x = (slow ? X_VEL_SLOW : X_VEL_NORMAL) * WorldVals.PPM;
                if (is(Facing.LEFT)) {
                    body.velocity.x *= -1f;
                }
            }
        });
    }

    private boolean isMegamanRight() {
        return game.getMegaman().body.isRightOf(body);
    }

    private boolean isMegamanAbove() {
        return game.getMegaman().body.isAbove(body);
    }

    private boolean doFacingAndMMDirMatch() {
        return (isMegamanRight() && is(Facing.RIGHT)) || (!isMegamanRight() && is(Facing.LEFT));
    }

    private boolean isMegamanOverlappingForceFixture() {
        Body megamanBody = game.getMegaman().body;
        return ShapeUtils.overlaps(forceFixture.shape, megamanBody.bounds);
    }

    private SpriteComponent spriteComponent() {
        sprite.setSize(1.5f * WorldVals.PPM, 1.5f * WorldVals.PPM);
        SpriteHandle h = new SpriteHandle(sprite, 4);
        h.updatable = delta -> {
            h.setPosition(body.bounds, Position.CENTER);
            sprite.setFlip(is(Facing.LEFT), false);
        };
        return new SpriteComponent(h);
    }

    private AnimationComponent animationComponent() {
        return new AnimationComponent(sprite, new Animation(magFlyReg, 2, .1f));
    }

    private ShapeComponent shapeComponent() {
        ShapeHandle shapeHandle = new ShapeHandle();
        shapeHandle.setShapeSupplier(() -> forceFixture.shape);
        shapeHandle.setDoRenderSupplier(() -> flash);
        shapeHandle.setColorSupplier(() -> GRAY);
        return new ShapeComponent(shapeHandle);
    }

}
