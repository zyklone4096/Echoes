package dev.zyklone.echoes.gui

import dev.zyklone.echoes.contents.echoes.EchoData
import net.kyori.adventure.text.event.ClickCallback

internal val DEFAULT_CALLBACK_OPTION = ClickCallback.Options.builder()
    .uses(1)
    .build()

internal typealias EchoEnhanceCallback = (EchoData) -> Unit
