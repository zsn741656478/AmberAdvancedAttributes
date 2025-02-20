package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import io.izzel.aaa.data.RangeValue;
import io.izzel.amber.commons.i18n.AmberLocale;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Values can be: 1, 10%, 1 to 2, 10% to 20%
 *
 * @author ustc_zzzz
 */
@NonnullByDefault
public class RangeValueElement extends CommandElement {
    private final TypeToken<Text> token;
    private final boolean fixedValue;
    private final AmberLocale locale;

    public RangeValueElement(AmberLocale locale, boolean fixed, Text key) {
        super(key);
        this.locale = locale;
        this.fixedValue = fixed;
        this.token = TypeToken.of(Text.class);
    }

    @Nullable
    @Override
    protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        String string = args.next();
        try {
            if (string.endsWith("%")) {
                double lowerBound = Double.parseDouble(string.substring(0, string.length() - 1)) / 100;
                if (!this.fixedValue && args.hasNext() && args.peek().equals("to")) {
                    string = args.next();
                    string = args.next();
                    if (string.endsWith("%")) {
                        double upperBound = Double.parseDouble(string.substring(0, string.length() - 1)) / 100;
                        return RangeValue.relative(lowerBound, upperBound);
                    } else {
                        Optional<Text> text = this.locale.getAs("commands.args.range-consistency", this.token);
                        throw args.createError(text.orElseThrow(IllegalStateException::new));
                    }
                } else {
                    return RangeValue.relative(lowerBound);
                }
            } else {
                double lowerBound = Double.parseDouble(string);
                if (!this.fixedValue && args.hasNext() && args.peek().equals("to")) {
                    string = args.next();
                    string = args.next();
                    if (string.endsWith("%")) {
                        Optional<Text> text = this.locale.getAs("commands.args.range-consistency", this.token);
                        throw args.createError(text.orElseThrow(IllegalStateException::new));
                    } else {
                        double upperBound = Double.parseDouble(string);
                        return RangeValue.absolute(lowerBound, upperBound);
                    }
                } else {
                    return RangeValue.absolute(lowerBound);
                }
            }
        } catch (NumberFormatException e) {
            Optional<Text> text = this.locale.getAs("commands.args.not-a-number", this.token, string);
            throw args.createError(text.orElseThrow(IllegalStateException::new));
        }
    }

    @Override
    public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        CommandArgs.Snapshot snapshot = args.getSnapshot();
        if (!this.fixedValue && args.nextIfPresent().isPresent()) {
            Optional<String> literal = args.nextIfPresent();
            if (literal.isPresent() && !args.hasNext()) {
                args.applySnapshot(snapshot);
                return ImmutableList.of("to");
            }
        }
        args.applySnapshot(snapshot);
        return ImmutableList.of();
    }
}
