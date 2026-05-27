package mobfactions;

import net.minecraft.network.chat.Component;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.Collection;
import java.util.Arrays;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.StringReader;

public class FactionArgument implements ArgumentType<String> {
	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "*", "012");
	private static final DynamicCommandExceptionType ErrorNotFound = new DynamicCommandExceptionType(id -> Component.translatableEscape("arguments.faction.not_found", id));

	public static FactionArgument faction() {
		return new FactionArgument();
	}

	public static Faction getFaction(CommandContext<CommandSourceStack> context, String field) throws CommandSyntaxException {
		String id = context.getArgument(field, String.class);
		Optional<Faction> faction = FactionManager.byId(id);
		if (faction.isEmpty())
			throw ErrorNotFound.create(id);
		return faction.get();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		S s = context.getSource();
		if (s instanceof CommandSourceStack source)
			return SharedSuggestionProvider.suggest(FactionManager.getAllNames(), builder);
		return s instanceof SharedSuggestionProvider provider ? provider.customSuggestion(context) : Suggestions.empty();
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		return reader.readUnquotedString();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
