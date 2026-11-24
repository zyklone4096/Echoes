package dev.zyklone.echoes.commands

import org.checkerframework.common.returnsreceiver.qual.This
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

class EnumArgumentParser<C, E: Enum<E>>(private val type: Class<E>): ArgumentParser<C, E>, SuggestionProvider<C> {
    companion object {
        fun <C, E: Enum<E>> component(name: String, type: Class<E>): @This CommandComponent.Builder<C, E> {
            val provider = EnumArgumentParser<C, E>(type)
            return CommandComponent
                .builder<C, E>()
                .valueType(type)
                .parser(provider)
                .suggestionProvider(provider)
                .name(name)
        }
    }

    override fun parse(
        commandContext: CommandContext<C & Any>,
        commandInput: CommandInput
    ): ArgumentParseResult<E> {
        val read = commandInput.readString()

        return type.enumConstants.firstOrNull { it.name.equals(read, true) }
            ?.let { ArgumentParseResult.success(it) }
            ?: ArgumentParseResult.failure(IllegalArgumentException())
    }

    override fun suggestionsFuture(
        context: CommandContext<C?>,
        input: CommandInput
    ): CompletableFuture<out Iterable<Suggestion>> {
        val read = input.readString()
        return CompletableFuture.completedFuture(type.enumConstants
            .map { it.name }
            .filter { it.startsWith(read, true) }
            .map { Suggestion.suggestion(it) })
    }
}
