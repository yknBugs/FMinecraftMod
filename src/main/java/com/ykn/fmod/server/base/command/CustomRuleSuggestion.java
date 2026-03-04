package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.commands.CommandSourceStack;

public class CustomRuleSuggestion implements SuggestionProvider<CommandSourceStack> {

    private final boolean needQuote;

    private final boolean allowAll;

    private final String filter;

    private final boolean enabledOnly;

    public CustomRuleSuggestion(boolean needQuote, boolean allowAll, String filter, boolean enabledOnly) {
        this.needQuote = needQuote;
        this.allowAll = allowAll;
        this.filter = filter;
        this.enabledOnly = enabledOnly;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (context.getSource() == null || context.getSource().getServer() == null) {
            return builder.buildFuture();
        }
        return context.getSource().getServer().submit(() -> {
            Map<String, RuleManager> customRules = Util.getServerData(context.getSource().getServer()).getCustomRules();
            Collection<String> ruleNames = customRules.keySet();
            for (String ruleName : ruleNames) {
                String suggestion = ruleName;
                RuleManager manager = customRules.get(ruleName);
                if (filter != null) {
                    if (!filter.equals(manager.getRule().getEvent().getType())) {
                        continue;
                    }
                }
                if (enabledOnly && !manager.isEnabled()) {
                    continue;
                }
                if (needQuote) {
                    suggestion = "\"" + suggestion + "\"";
                }
                if (suggestion.startsWith(builder.getRemaining())) {
                    builder.suggest(suggestion);
                }
            }
            if (allowAll && "*".startsWith(builder.getRemaining())) {
                builder.suggest("*");
            }
            return builder.build();
        });
    }

    public static CustomRuleSuggestion suggest(boolean needQuote) {
        return new CustomRuleSuggestion(needQuote, false, null, false);
    }

    public static CustomRuleSuggestion suggestSave() {
        return new CustomRuleSuggestion(false, true, null, false);
    }
    
}
