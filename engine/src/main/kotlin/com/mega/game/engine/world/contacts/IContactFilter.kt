package com.mega.game.engine.world.contacts

import com.mega.game.engine.world.body.IFixture

/**
 * Determines if a [Contact] should be created from the fixtures passed into the [filter] method.
 */
interface IContactFilter {

    /**
     * Returns true if filtering for contacts should proceed given the following [fixture]. If this method returns true,
     * then the [com.mega.game.engine.world.WorldSystem] will query for other fixtures that overlap the provided
     * [fixture].
     *
     * @param fixture The fixture which should be checked
     * @return If the world system should continue filtering
     */
    fun shouldProceedFiltering(fixture: IFixture): Boolean

    /**
     * Returns true if a [Contact] can be instantiated given the two [com.mega.game.engine.world.body.Fixture]s.
     *
     * @param fixture1 The first fixture
     * @param fixture2 The second fixture
     * @return If the two fixtures result in a contact
     */
    fun filter(fixture1: IFixture, fixture2: IFixture): Boolean
}
