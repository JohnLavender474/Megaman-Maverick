package com.megaman.maverick.game.entities.bosses.timberwalker

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.pairTo

object TimberWalkerFrames {

    const val CANVAS_WIDTH = 304
    const val CANVAS_HEIGHT = 128
    const val BODY_X = 220
    const val FLOOR_Y = 123

    val ANCHORS = objectMapOf(
        "attack_strike" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(102, 111, 26, 12),
            claw = PxBox(140, 22, 31, 25),
            grip = PxPoint(151, 34),
            footY = 123,
            rightX = 274
        ),
        "attack_windup" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(129, 26, 24, 30),
            claw = PxBox(153, 8, 32, 29),
            grip = PxPoint(165, 22),
            footY = 123,
            rightX = 275
        ),
        "claw_ext_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 15, 33, 33),
            grip = PxPoint(153, 32),
            footY = 123,
            rightX = 275
        ),
        "claw_ext_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(116, 6, 31, 33),
            grip = PxPoint(127, 22),
            footY = 123,
            rightX = 275
        ),
        "claw_ext_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(94, 2, 31, 33),
            grip = PxPoint(105, 18),
            footY = 123,
            rightX = 275
        ),
        "claw_ext_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(94, 8, 31, 24),
            grip = PxPoint(105, 19),
            footY = 123,
            rightX = 275
        ),
        "crouch" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 38, 92, 66),
            cab = PxBox(174, 43, 34, 25),
            saw = PxBox(124, 92, 31, 9),
            claw = PxBox(144, 49, 31, 25),
            grip = PxPoint(155, 61),
            footY = 123,
            rightX = 280
        ),
        "damaged_idle_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "damaged_idle_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 81, 31, 10),
            claw = PxBox(142, 21, 31, 25),
            grip = PxPoint(153, 33),
            footY = 123,
            rightX = 275
        ),
        "damaged_walk_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(126, 87, 30, 16),
            claw = PxBox(143, 36, 32, 24),
            grip = PxPoint(156, 48),
            footY = 123,
            rightX = 279
        ),
        "damaged_walk_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 23, 92, 66),
            cab = PxBox(174, 28, 34, 25),
            saw = PxBox(126, 84, 30, 16),
            claw = PxBox(143, 37, 32, 24),
            grip = PxPoint(156, 49),
            footY = 123,
            rightX = 275
        ),
        "damaged_walk_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(126, 89, 30, 16),
            claw = PxBox(143, 38, 32, 24),
            grip = PxPoint(156, 50),
            footY = 123,
            rightX = 277
        ),
        "damaged_walk_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(126, 86, 30, 16),
            claw = PxBox(143, 39, 32, 24),
            grip = PxPoint(156, 51),
            footY = 123,
            rightX = 275
        ),
        "defeat_break" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 36, 92, 66),
            cab = null,
            saw = null,
            claw = null,
            grip = null,
            footY = 123,
            rightX = 282
        ),
        "defeat_shake_1" pairTo TimberWalkerAnchors(
            hull = PxBox(173, 27, 92, 66),
            cab = PxBox(176, 32, 34, 25),
            saw = PxBox(128, 88, 30, 16),
            claw = PxBox(145, 43, 32, 24),
            grip = PxPoint(158, 55),
            footY = 123,
            rightX = 277
        ),
        "defeat_shake_2" pairTo TimberWalkerAnchors(
            hull = PxBox(169, 27, 92, 66),
            cab = PxBox(172, 32, 34, 25),
            saw = PxBox(124, 88, 30, 16),
            claw = PxBox(141, 43, 32, 24),
            grip = PxPoint(154, 55),
            footY = 123,
            rightX = 273
        ),
        "defeat_wreck" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 53, 92, 66),
            cab = null,
            saw = null,
            claw = null,
            grip = null,
            footY = 123,
            rightX = 297
        ),
        "grab_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(127, 61, 30, 17),
            claw = PxBox(115, 76, 44, 47),
            grip = PxPoint(131, 107),
            footY = 123,
            rightX = 275
        ),
        "grab_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(127, 61, 30, 17),
            claw = PxBox(94, 76, 44, 47),
            grip = PxPoint(110, 107),
            footY = 123,
            rightX = 275
        ),
        "grab_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(127, 61, 30, 17),
            claw = PxBox(73, 76, 44, 47),
            grip = PxPoint(89, 107),
            footY = 123,
            rightX = 275
        ),
        "grab_hold" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(127, 61, 30, 17),
            claw = PxBox(73, 88, 44, 35),
            grip = PxPoint(89, 107),
            footY = 123,
            rightX = 275
        ),
        "grab_in" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(125, 70, 30, 13),
            claw = PxBox(120, 41, 43, 45),
            grip = PxPoint(136, 66),
            footY = 123,
            rightX = 275
        ),
        "grab_lift" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(123, 9, 44, 38),
            grip = PxPoint(139, 28),
            footY = 123,
            rightX = 275
        ),
        "grab_throw" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(124, 80, 31, 9),
            claw = PxBox(90, 1, 39, 55),
            grip = PxPoint(101, 28),
            footY = 123,
            rightX = 275
        ),
        "guard_close_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "guard_close_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "guard_close_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "guard_open_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "guard_open_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "guard_open_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "hit_flash" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "hop_tuck" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 14, 92, 66),
            cab = PxBox(174, 19, 34, 25),
            saw = PxBox(124, 62, 31, 12),
            claw = PxBox(142, 9, 31, 25),
            grip = PxPoint(153, 21),
            footY = 95,
            rightX = 264
        ),
        "idle_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "idle_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 20, 31, 25),
            grip = PxPoint(151, 32),
            footY = 123,
            rightX = 275
        ),
        "idle_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "idle_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 20, 31, 25),
            grip = PxPoint(151, 32),
            footY = 123,
            rightX = 275
        ),
        "idle_5" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "idle_6" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 20, 31, 25),
            grip = PxPoint(151, 32),
            footY = 123,
            rightX = 275
        ),
        "jump" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 14, 92, 66),
            cab = PxBox(174, 19, 34, 25),
            saw = PxBox(126, 54, 30, 16),
            claw = PxBox(149, 5, 32, 29),
            grip = PxPoint(161, 19),
            footY = 123,
            rightX = 278
        ),
        "land" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 41, 92, 66),
            cab = PxBox(174, 46, 34, 25),
            saw = PxBox(124, 95, 31, 9),
            claw = PxBox(141, 63, 32, 24),
            grip = PxPoint(154, 75),
            footY = 123,
            rightX = 297
        ),
        "log_grab" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(124, 80, 31, 9),
            claw = PxBox(107, 94, 24, 29),
            grip = PxPoint(118, 113),
            footY = 123,
            rightX = 275
        ),
        "log_in" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(108, 53, 33, 31),
            grip = PxPoint(124, 67),
            footY = 123,
            rightX = 275
        ),
        "log_lift" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(113, 53, 28, 25),
            grip = PxPoint(124, 67),
            footY = 123,
            rightX = 275
        ),
        "log_reach" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(101, 93, 35, 30),
            grip = PxPoint(118, 112),
            footY = 123,
            rightX = 275
        ),
        "log_swing" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(186, 4, 31, 23),
            grip = PxPoint(197, 15),
            footY = 123,
            rightX = 275
        ),
        "log_throw" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(124, 80, 31, 9),
            claw = PxBox(126, 9, 31, 35),
            grip = PxPoint(137, 27),
            footY = 123,
            rightX = 275
        ),
        "met_hit" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_recover_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_recover_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "met_recover_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_recover_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_stunned_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_stunned_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "met_stunned_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "met_stunned_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(124, 79, 31, 9),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "recover" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(115, 89, 30, 16),
            claw = PxBox(140, 19, 31, 25),
            grip = PxPoint(151, 31),
            footY = 123,
            rightX = 275
        ),
        "roar_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(120, 48, 29, 21),
            claw = PxBox(153, 1, 32, 32),
            grip = PxPoint(169, 17),
            footY = 123,
            rightX = 275
        ),
        "roar_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(120, 49, 29, 21),
            claw = PxBox(156, 4, 29, 29),
            grip = PxPoint(169, 17),
            footY = 123,
            rightX = 275
        ),
        "roar_in" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(120, 64, 31, 15),
            claw = PxBox(146, 9, 32, 29),
            grip = PxPoint(158, 23),
            footY = 123,
            rightX = 275
        ),
        "saw_level_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 18, 92, 66),
            cab = PxBox(174, 23, 34, 25),
            saw = PxBox(124, 72, 31, 9),
            claw = PxBox(150, 13, 31, 25),
            grip = PxPoint(161, 25),
            footY = 123,
            rightX = 280
        ),
        "saw_level_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 18, 92, 66),
            cab = PxBox(174, 23, 34, 25),
            saw = PxBox(101, 72, 31, 9),
            claw = PxBox(150, 13, 31, 25),
            grip = PxPoint(161, 25),
            footY = 123,
            rightX = 280
        ),
        "saw_level_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 18, 92, 66),
            cab = PxBox(174, 23, 34, 25),
            saw = PxBox(78, 72, 31, 9),
            claw = PxBox(150, 13, 31, 25),
            grip = PxPoint(161, 25),
            footY = 123,
            rightX = 280
        ),
        "saw_level_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 18, 92, 66),
            cab = PxBox(174, 23, 34, 25),
            saw = PxBox(54, 72, 31, 9),
            claw = PxBox(150, 13, 31, 25),
            grip = PxPoint(161, 25),
            footY = 123,
            rightX = 280
        ),
        "saw_low_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(126, 89, 30, 16),
            claw = PxBox(144, 41, 31, 25),
            grip = PxPoint(155, 53),
            footY = 123,
            rightX = 279
        ),
        "saw_low_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(102, 93, 30, 14),
            claw = PxBox(144, 41, 31, 25),
            grip = PxPoint(155, 53),
            footY = 123,
            rightX = 279
        ),
        "saw_low_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(78, 97, 31, 15),
            claw = PxBox(144, 41, 31, 25),
            grip = PxPoint(155, 53),
            footY = 123,
            rightX = 279
        ),
        "saw_low_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 28, 92, 66),
            cab = PxBox(174, 33, 34, 25),
            saw = PxBox(54, 102, 31, 14),
            claw = PxBox(144, 41, 31, 25),
            grip = PxPoint(155, 53),
            footY = 123,
            rightX = 279
        ),
        "stuck_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 27, 92, 66),
            cab = PxBox(174, 32, 34, 25),
            saw = PxBox(101, 110, 27, 13),
            claw = PxBox(140, 21, 31, 25),
            grip = PxPoint(151, 33),
            footY = 123,
            rightX = 275
        ),
        "stuck_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 27, 92, 66),
            cab = PxBox(174, 32, 34, 25),
            saw = PxBox(101, 110, 27, 13),
            claw = PxBox(140, 21, 31, 25),
            grip = PxPoint(151, 33),
            footY = 123,
            rightX = 275
        ),
        "tell_button_claw_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_button_claw_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_button_claw_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_chop_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_chop_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_chop_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_level_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_level_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_level_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_low_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_low_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "tell_lever_low_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 24, 92, 66),
            cab = PxBox(174, 29, 34, 25),
            saw = PxBox(124, 78, 31, 9),
            claw = PxBox(140, 18, 31, 25),
            grip = PxPoint(151, 30),
            footY = 123,
            rightX = 275
        ),
        "turn_1" pairTo TimberWalkerAnchors(
            hull = PxBox(183, 25, 75, 66),
            cab = PxBox(186, 30, 34, 25),
            saw = PxBox(160, 79, 21, 9),
            claw = PxBox(171, 15, 24, 24),
            grip = PxPoint(180, 27),
            footY = 123,
            rightX = 277
        ),
        "turn_2" pairTo TimberWalkerAnchors(
            hull = PxBox(200, 26, 41, 66),
            cab = PxBox(203, 31, 34, 25),
            saw = PxBox(216, 80, 9, 9),
            claw = PxBox(214, 12, 13, 25),
            grip = PxPoint(220, 24),
            footY = 123,
            rightX = 266
        ),
        "walk_1" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(124, 82, 31, 10),
            claw = PxBox(142, 17, 31, 25),
            grip = PxPoint(153, 29),
            footY = 123,
            rightX = 284
        ),
        "walk_2" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 23, 92, 66),
            cab = PxBox(174, 28, 34, 25),
            saw = PxBox(124, 74, 31, 10),
            claw = PxBox(142, 18, 31, 25),
            grip = PxPoint(153, 30),
            footY = 123,
            rightX = 274
        ),
        "walk_3" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 26, 92, 66),
            cab = PxBox(174, 31, 34, 25),
            saw = PxBox(124, 82, 31, 10),
            claw = PxBox(142, 17, 31, 25),
            grip = PxPoint(153, 29),
            footY = 123,
            rightX = 276
        ),
        "walk_4" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 23, 92, 66),
            cab = PxBox(174, 28, 34, 25),
            saw = PxBox(124, 74, 31, 10),
            claw = PxBox(142, 18, 31, 25),
            grip = PxPoint(153, 30),
            footY = 123,
            rightX = 275
        ),
        "windup_in" pairTo TimberWalkerAnchors(
            hull = PxBox(171, 25, 92, 66),
            cab = PxBox(174, 30, 34, 25),
            saw = PxBox(121, 54, 29, 20),
            claw = PxBox(147, 14, 31, 28),
            grip = PxPoint(158, 27),
            footY = 123,
            rightX = 275
        )
    )

    val ANIMATIONS = objectMapOf(
        "far_walk" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("far_walk_1", "far_walk_2", "far_walk_3", "far_walk_4"),
            durations = gdxArrayOf(0.15f, 0.15f, 0.15f, 0.15f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = true
        ),
        "far_crouch" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("far_crouch"),
            durations = gdxArrayOf(0.4f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "far_jump" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("far_jump"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "jump_mid_1" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("jump_mid_1"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "jump_mid_2" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("jump_mid_2"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "jump" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("jump"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "land" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("land", "crouch", "idle_1"),
            durations = gdxArrayOf(0.25f, 0.15f, 0.2f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "roar_start" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "roar_in"),
            durations = gdxArrayOf(0.1f, 0.09f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "damaged_roar_start" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "roar_in"),
            durations = gdxArrayOf(0.1f, 0.09f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "roar" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("roar_1", "roar_2"),
            durations = gdxArrayOf(0.09f, 0.09f),
            mirrored = booleanArrayOf(false, false),
            loop = true
        ),
        "roar_end" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("roar_in", "idle_1"),
            durations = gdxArrayOf(0.09f, 0.15f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "damaged_roar_end" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("roar_in", "damaged_idle_1"),
            durations = gdxArrayOf(0.09f, 0.15f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "idle" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "idle_2", "idle_3", "idle_4", "idle_5", "idle_6"),
            durations = gdxArrayOf(0.17f, 0.17f, 0.17f, 0.17f, 0.17f, 0.17f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = true
        ),
        "walk" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("walk_1", "walk_2", "walk_3", "walk_4"),
            durations = gdxArrayOf(0.15f, 0.15f, 0.15f, 0.15f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = true
        ),
        "damaged_idle" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "damaged_idle_2"),
            durations = gdxArrayOf(0.2f, 0.2f),
            mirrored = booleanArrayOf(false, false),
            loop = true
        ),
        "damaged_walk" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_walk_1", "damaged_walk_2", "damaged_walk_3", "damaged_walk_4"),
            durations = gdxArrayOf(0.15f, 0.15f, 0.19f, 0.15f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = true
        ),
        "turn" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "turn_1", "turn_2", "turn_1", "idle_1"),
            durations = gdxArrayOf(0.15f, 0.09f, 0.09f, 0.09f, 0.2f),
            mirrored = booleanArrayOf(false, false, false, true, true),
            loop = false
        ),
        "damaged_turn" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "turn_1", "turn_2", "turn_1", "damaged_idle_1"),
            durations = gdxArrayOf(0.15f, 0.09f, 0.09f, 0.09f, 0.2f),
            mirrored = booleanArrayOf(false, false, false, true, true),
            loop = false
        ),
        "tell_lever_low" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_low_1", "tell_lever_low_2", "tell_lever_low_3", "tell_lever_low_2", "tell_lever_low_3", "idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "tell_lever_level" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_level_1", "tell_lever_level_2", "tell_lever_level_3", "tell_lever_level_2", "tell_lever_level_3", "idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "tell_lever_chop" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_chop_1", "tell_lever_chop_2", "tell_lever_chop_3", "tell_lever_chop_2", "tell_lever_chop_3", "idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "tell_button_claw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_button_claw_1", "tell_button_claw_2", "tell_button_claw_3", "tell_button_claw_2", "tell_button_claw_3", "idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "saw_low" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "saw_low_1", "saw_low_2", "saw_low_3", "saw_low_4", "saw_low_3", "saw_low_2", "saw_low_1", "idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.05f, 0.05f, 0.4f, 0.08f, 0.08f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "saw_level" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "saw_level_1", "saw_level_2", "saw_level_3", "saw_level_4", "saw_level_3", "saw_level_2", "saw_level_1", "idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.05f, 0.05f, 0.4f, 0.08f, 0.08f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "attack_saw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "windup_in", "attack_windup", "attack_strike", "stuck_1", "stuck_2", "stuck_1", "stuck_2", "recover", "idle_1"),
            durations = gdxArrayOf(0.12f, 0.08f, 0.35f, 0.09f, 0.25f, 0.25f, 0.25f, 0.25f, 0.2f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "attack_log" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "claw_ext_1", "log_in", "log_reach", "log_grab", "log_lift", "log_swing", "log_throw", "idle_1"),
            durations = gdxArrayOf(0.2f, 0.12f, 0.08f, 0.2f, 0.22f, 0.07f, 0.18f, 0.25f, 0.35f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "hop_crouch" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "crouch"),
            durations = gdxArrayOf(0.15f, 0.25f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "hop_air" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("jump", "hop_tuck"),
            durations = gdxArrayOf(0.09f, 2.0f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "hop_land" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("land", "crouch", "idle_1"),
            durations = gdxArrayOf(0.15f, 0.18f, 0.3f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "claw_extend" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "claw_ext_1", "claw_ext_2", "claw_ext_3", "claw_ext_4", "claw_ext_2", "claw_ext_1", "idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.06f, 0.2f, 0.3f, 0.11f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false),
            loop = false
        ),
        "grab_reach" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("idle_1", "grab_in", "grab_1", "grab_2", "grab_3", "grab_hold"),
            durations = gdxArrayOf(0.12f, 0.08f, 0.25f, 0.06f, 0.08f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "grab_miss" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("grab_3", "grab_2", "grab_1", "grab_in", "idle_1"),
            durations = gdxArrayOf(0.1f, 0.11f, 0.12f, 0.08f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false),
            loop = false
        ),
        "grab_throw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("grab_lift", "grab_throw", "idle_1"),
            durations = gdxArrayOf(0.35f, 0.25f, 0.3f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "guard_open" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_open_1", "guard_open_2", "guard_open_3"),
            durations = gdxArrayOf(0.07f, 0.07f, 0.16f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "met_stunned" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("met_stunned_1", "met_stunned_2", "met_stunned_3", "met_stunned_4"),
            durations = gdxArrayOf(0.12f, 0.12f, 0.12f, 0.12f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = true
        ),
        "met_hit" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("hit_flash", "met_hit"),
            durations = gdxArrayOf(0.06f, 0.12f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "met_recover" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("met_recover_1", "met_recover_2", "met_recover_1", "met_recover_2", "met_recover_3", "met_recover_4"),
            durations = gdxArrayOf(0.11f, 0.11f, 0.11f, 0.11f, 0.2f, 0.25f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "guard_close" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_close_1", "guard_close_2", "guard_close_3", "idle_1"),
            durations = gdxArrayOf(0.07f, 0.07f, 0.14f, 0.2f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = false
        ),
        "defeat" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("defeat_shake_1", "defeat_shake_2", "defeat_shake_1", "defeat_shake_2", "defeat_shake_1", "defeat_shake_2", "defeat_shake_1", "defeat_shake_2", "defeat_break", "defeat_wreck"),
            durations = gdxArrayOf(0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.07f, 0.27f, 2.1f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_land" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("land", "crouch", "damaged_idle_1"),
            durations = gdxArrayOf(0.25f, 0.15f, 0.2f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "damaged_tell_lever_low" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_low_1", "tell_lever_low_2", "tell_lever_low_3", "tell_lever_low_2", "tell_lever_low_3", "damaged_idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "damaged_tell_lever_level" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_level_1", "tell_lever_level_2", "tell_lever_level_3", "tell_lever_level_2", "tell_lever_level_3", "damaged_idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "damaged_tell_lever_chop" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_lever_chop_1", "tell_lever_chop_2", "tell_lever_chop_3", "tell_lever_chop_2", "tell_lever_chop_3", "damaged_idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "damaged_tell_button_claw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("tell_button_claw_1", "tell_button_claw_2", "tell_button_claw_3", "tell_button_claw_2", "tell_button_claw_3", "damaged_idle_1"),
            durations = gdxArrayOf(0.12f, 0.1f, 0.1f, 0.1f, 0.1f, 0.06f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "damaged_saw_low" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "saw_low_1", "saw_low_2", "saw_low_3", "saw_low_4", "saw_low_3", "saw_low_2", "saw_low_1", "damaged_idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.05f, 0.05f, 0.4f, 0.08f, 0.08f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_saw_level" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "saw_level_1", "saw_level_2", "saw_level_3", "saw_level_4", "saw_level_3", "saw_level_2", "saw_level_1", "damaged_idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.05f, 0.05f, 0.4f, 0.08f, 0.08f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_attack_saw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "windup_in", "attack_windup", "attack_strike", "stuck_1", "stuck_2", "stuck_1", "stuck_2", "recover", "damaged_idle_1"),
            durations = gdxArrayOf(0.12f, 0.08f, 0.35f, 0.09f, 0.25f, 0.25f, 0.25f, 0.25f, 0.2f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_attack_log" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "claw_ext_1", "log_in", "log_reach", "log_grab", "log_lift", "log_swing", "log_throw", "damaged_idle_1"),
            durations = gdxArrayOf(0.2f, 0.12f, 0.08f, 0.2f, 0.22f, 0.07f, 0.18f, 0.25f, 0.35f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_hop_crouch" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "crouch"),
            durations = gdxArrayOf(0.15f, 0.25f),
            mirrored = booleanArrayOf(false, false),
            loop = false
        ),
        "damaged_hop_land" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("land", "crouch", "damaged_idle_1"),
            durations = gdxArrayOf(0.15f, 0.18f, 0.3f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "damaged_claw_extend" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "claw_ext_1", "claw_ext_2", "claw_ext_3", "claw_ext_4", "claw_ext_2", "claw_ext_1", "damaged_idle_1"),
            durations = gdxArrayOf(0.15f, 0.3f, 0.06f, 0.2f, 0.3f, 0.11f, 0.15f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false, false, false),
            loop = false
        ),
        "damaged_grab_reach" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("damaged_idle_1", "grab_in", "grab_1", "grab_2", "grab_3", "grab_hold"),
            durations = gdxArrayOf(0.12f, 0.08f, 0.25f, 0.06f, 0.08f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false, false),
            loop = false
        ),
        "damaged_grab_miss" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("grab_3", "grab_2", "grab_1", "grab_in", "damaged_idle_1"),
            durations = gdxArrayOf(0.1f, 0.11f, 0.12f, 0.08f, 0.3f),
            mirrored = booleanArrayOf(false, false, false, false, false),
            loop = false
        ),
        "damaged_grab_throw" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("grab_lift", "grab_throw", "damaged_idle_1"),
            durations = gdxArrayOf(0.35f, 0.25f, 0.3f),
            mirrored = booleanArrayOf(false, false, false),
            loop = false
        ),
        "damaged_guard_close" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_close_1", "guard_close_2", "guard_close_3", "damaged_idle_1"),
            durations = gdxArrayOf(0.07f, 0.07f, 0.14f, 0.2f),
            mirrored = booleanArrayOf(false, false, false, false),
            loop = false
        ),
        "guard_crack_1" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_crack_1"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "guard_crack_2" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_crack_2"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "guard_crack_3" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_crack_3"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "guard_hit" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("guard_hit"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        ),
        "hop_shadow" pairTo TimberWalkerAnimDef(
            frames = gdxArrayOf("hop_shadow"),
            durations = gdxArrayOf(1.0f),
            mirrored = booleanArrayOf(false),
            loop = false
        )
    )
}
