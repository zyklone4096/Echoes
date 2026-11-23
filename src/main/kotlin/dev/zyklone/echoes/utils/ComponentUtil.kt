package dev.zyklone.echoes.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

private val styleless = Style.style()
    .decoration(TextDecoration.ITALIC, false)
    .color(NamedTextColor.WHITE)
    .build()

fun TextComponent.Builder.styleless() =
    this.style(styleless)

fun String.styleless() =
    Component.text(this, styleless)

fun TextComponent.styleless() =
    this.style(styleless)

fun Byte.asRarityColor() = when (this) {
    0.toByte() -> NamedTextColor.WHITE
    1.toByte() -> NamedTextColor.GREEN
    2.toByte() -> NamedTextColor.BLUE
    3.toByte() -> NamedTextColor.LIGHT_PURPLE
    else -> NamedTextColor.GOLD
}

fun String.literal(color: TextColor? = null) = if (color == null)
    Component.text(this)
else Component.text(this, color)
