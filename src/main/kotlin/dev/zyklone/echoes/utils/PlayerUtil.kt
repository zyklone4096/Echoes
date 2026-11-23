package dev.zyklone.echoes.utils

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Player

internal fun Player.getOrRegisterAttribute(attribute: Attribute): AttributeInstance {
    var got = this.getAttribute(attribute)
    if (got == null) {
        this.registerAttribute(attribute)
        got = this.getAttribute(attribute)!!
    }
    return got
}
