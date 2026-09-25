package com.mahyarmozafar.tik.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Springs from Material 3 Expressive. "Spatial" ones move and resize things and may bounce a
 * little; "effects" ones change colors and fades and never bounce.
 */
object Motion {
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** A soft bounce for glass that grows or melts into another shape. */
    fun <T> liquid(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.7f, stiffness = 420f)
}
