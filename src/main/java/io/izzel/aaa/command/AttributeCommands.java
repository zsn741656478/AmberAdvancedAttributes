package io.izzel.aaa.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.izzel.aaa.Main;
import io.izzel.aaa.data.MarkerValue;
import io.izzel.aaa.data.RangeValue;
import io.izzel.aaa.service.Attribute;
import io.izzel.aaa.service.AttributeService;
import io.izzel.aaa.service.AttributeToLoreFunction;
import io.izzel.aaa.util.DataUtil;
import io.izzel.amber.commons.i18n.AmberLocale;
import io.izzel.amber.commons.i18n.args.Arg;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.ChangeEntityEquipmentEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.izzel.aaa.service.AttributeToLoreFunctions.*;

/**
 * @author ustc_zzzz
 */
@Singleton
public class AttributeCommands {
    private static final Text LORE_SEPARATOR = Text.of();

    private final Provider<Main> pluginProvider;
    private final CommandManager commandManager;
    private final EventManager eventManager;
    private final AmberLocale locale;

    @Inject
    public AttributeCommands(Provider<Main> plugin, CommandManager c, EventManager e, AmberLocale locale) {
        this.pluginProvider = plugin;
        this.commandManager = c;
        this.eventManager = e;
        this.locale = locale;
    }

    public void init() {
        Main plugin = this.pluginProvider.get();
        this.eventManager.registerListener(plugin, Attribute.RegistryEvent.class, Order.EARLY, this::on);
        this.eventManager.registerListener(plugin, ChangeEntityEquipmentEvent.class, Order.LATE, this::on);
    }

    public void on(ChangeEntityEquipmentEvent event) {
        Transaction<ItemStackSnapshot> transaction = event.getTransaction();
        if (transaction.isValid()) {
            ListMultimap<Byte, Text> texts;
            Key<ListValue<Text>> key = Keys.ITEM_LORE;
            ItemStack item = transaction.getFinal().createStack();
            if (DataUtil.hasData(item)) {
                texts = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);
                Map<String, Attribute<?>> attributes = AttributeService.instance().getAttributes();
                attributes.values().forEach(attribute -> DataUtil.collectLore(texts, item, attribute));
                item.offer(key, Multimaps.asMap(texts).values().stream().reduce(ImmutableList.of(), (a, b) -> {
                    if (a.isEmpty()) {
                        return b;
                    } else {
                        return ImmutableList.<Text>builder().addAll(a).add(LORE_SEPARATOR).addAll(b).build();
                    }
                }));
                transaction.setCustom(item.createSnapshot());
            }
        }
    }

    private void on(Attribute.RegistryEvent event) {
        Main plugin = this.pluginProvider.get();
        this.registerRangeValue(plugin, event, "attack");
        this.registerRangeValue(plugin, event, "tracing");
        this.registerRangeValue(plugin, event, "pvp-attack");
        this.registerRangeValue(plugin, event, "pve-attack");
        this.registerRangeValue(plugin, event, "defense");
        this.registerRangeValue(plugin, event, "pvp-defense");
        this.registerRangeValue(plugin, event, "pve-defense");
        this.registerRangeValue(plugin, event, "reflect");
        this.registerRangeValue(plugin, event, "pvp-reflect");
        this.registerRangeValue(plugin, event, "pve-reflect");
        this.registerRangeValue(plugin, event, "reflect-rate");
        this.registerRangeValue(plugin, event, "critical");
        this.registerRangeValue(plugin, event, "critical-rate");
        this.registerRangeValue(plugin, event, "dodge");
        this.registerRangeValue(plugin, event, "accuracy");
        this.registerRangeValue(plugin, event, "accelerate");
        this.registerRangeValueFixed(plugin, event, "attack-speed");
        this.registerRangeValueFixed(plugin, event, "move-speed");
        this.registerDurabilityValue(plugin, event, "durability");
        this.registerMarkerValue(plugin, event, "unbreakable");
        this.registerRangeValue(plugin, event, "loot-rate");
        this.registerMarkerValue(plugin, event, "loot-immune");
        this.registerRangeValue(plugin, event, "burn");
        this.registerRangeValue(plugin, event, "burn-rate");
        this.registerRangeValue(plugin, event, "life-steal");
        this.registerRangeValue(plugin, event, "life-steal-rate");
        this.registerRangeValueFixed(plugin, event, "max-health");
        this.registerRangeValueFixed(plugin, event, "attack-range");
        this.registerRangeValue(plugin, event, "starvation");
        this.registerRangeValue(plugin, event, "saturation");
        this.registerRangeValue(plugin, event, "regeneration");
        this.registerRangeValue(plugin, event, "knockback");
        this.registerRangeValue(plugin, event, "instant-death");
        this.registerMarkerValue(plugin, event, "instant-death-immune");
        this.registerPossessValue(plugin, event, "possession");
        this.registerTextValue(plugin, event, "original-lore");
    }

    private void registerTextValue(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<Text> function = values -> ImmutableList.of();
        Attribute<Text> attribute = event.register("aaa-" + id, Text.class, function);
        this.commandManager.register(plugin, this.getInitCommand(id, attribute), "aaa-init");
        this.commandManager.register(plugin, this.getDropCommand(id, attribute), "aaa-drop");
    }

    private void registerDurabilityValue(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = durability(this.locale);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(plugin, this.getRangeCommand(id, false, attribute), "aaa-" + id);
    }

    private void registerRangeValue(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue> function = rangeValue(this.locale, id);
        Attribute<RangeValue> attribute = event.register("aaa-" + id, RangeValue.class, function);
        this.commandManager.register(plugin, this.getRangeCommand(id, false, attribute), "aaa-" + id);
    }

    private void registerRangeValueFixed(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<RangeValue.Fixed> function = rangeValue(this.locale, id);
        Attribute<RangeValue.Fixed> attribute = event.register("aaa-" + id, RangeValue.Fixed.class, function);
        this.commandManager.register(plugin, this.getRangeCommand(id, true, attribute), "aaa-" + id);
    }

    private void registerMarkerValue(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<MarkerValue> function = markerValue(this.locale, id);
        Attribute<MarkerValue> attribute = event.register("aaa-" + id, MarkerValue.class, function);
        this.commandManager.register(plugin, this.getMarkerCommand(id, attribute), "aaa-" + id);
    }

    private void registerPossessValue(Main plugin, Attribute.RegistryEvent event, String id) {
        AttributeToLoreFunction<GameProfile> function = profile(this.locale);
        Attribute<GameProfile> attribute = event.register("aaa-" + id, GameProfile.class, function);
        this.commandManager.register(plugin, this.getPossessCommand(id, attribute), "aaa-possess");
        this.commandManager.register(plugin, this.getPublicizeCommand(id, attribute), "aaa-publicize");
    }

    private CommandSpec getDropCommand(String id, Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-drop")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        AtomicBoolean isCallbackExecuted = new AtomicBoolean(false);
                        Arg arg = Arg.ref("commands.drop.warning-ok").withCallback(value -> {
                            if (!isCallbackExecuted.getAndSet(true)) {
                                Optional<ItemStack> stackOptional = ((Player) value).getItemInHand(HandTypes.MAIN_HAND);
                                if (stackOptional.isPresent()) {
                                    ItemStack stack = stackOptional.get();
                                    if (DataUtil.hasData(stack)) {
                                        List<Text> lore = attribute.getValues(stack);
                                        DataUtil.dropData(stack);
                                        stack.offer(Keys.ITEM_LORE, lore);
                                        ((Player) value).setItemInHand(HandTypes.MAIN_HAND, stack);
                                        this.locale.to(value, "commands.drop.succeed");
                                        return;
                                    }
                                }
                                this.locale.to(value, "commands.drop.nonexist");
                            }
                        });
                        locale.to(src, "commands.drop.warning", arg);
                        return CommandResult.success();
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getInitCommand(String id, Attribute<Text> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-init")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                this.locale.to(src, "commands.init.already-exist");
                                return CommandResult.success();
                            } else {
                                attribute.setValues(stack, stack.get(Keys.ITEM_LORE).orElse(ImmutableList.of()));
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.init.succeed");
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends RangeValue> CommandSpec getRangeCommand(String id, boolean fixed, Attribute<T> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-" + id)
                .child(this.getRangeClearCommand(id, attribute), "clear")
                .child(this.getRangeAppendCommand(id, fixed, attribute), "append")
                .child(this.getRangePrependCommand(id, fixed, attribute), "prepend")
                .build();
    }

    private <T extends RangeValue> CommandSpec getRangePrependCommand(String id, boolean fixed, Attribute<T> attribute) {
        return CommandSpec.builder()
                .arguments(new RangeValueElement(this.locale, fixed, Text.of("value")))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(Text.of("value"));
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.prependValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.prepend-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends RangeValue> CommandSpec getRangeAppendCommand(String id, boolean fixed, Attribute<T> attribute) {
        return CommandSpec.builder()
                .arguments(new RangeValueElement(this.locale, fixed, Text.of("value")))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<T> rangeValueOptional = args.getOne(Text.of("value"));
                        if (stackOptional.isPresent() && rangeValueOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.appendValue(stack, rangeValueOptional.get());
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.append-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private <T extends RangeValue> CommandSpec getRangeClearCommand(String id, Attribute<T> attribute) {
        return CommandSpec.builder()
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                this.locale.to(src, "commands.range.clear-attribute", stack, id);
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getMarkerCommand(String id, Attribute<MarkerValue> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-" + id)
                .arguments(GenericArguments.choices(Text.of("marked"),
                        ImmutableMap.of("mark", Boolean.TRUE, "unmark", Boolean.FALSE)))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Optional<Boolean> marked = args.getOne(Text.of("marked"));
                        if (stackOptional.isPresent() && marked.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                if (marked.get()) {
                                    attribute.setValues(stack, ImmutableList.of(MarkerValue.of()));
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.mark-attribute", stack, id);
                                    return CommandResult.success();
                                } else {
                                    attribute.clearValues(stack);
                                    ((Player) src).setItemInHand(HandTypes.MAIN_HAND, stack);
                                    this.locale.to(src, "commands.marker.unmark-attribute", stack, id);
                                    return CommandResult.success();
                                }
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getPublicizeCommand(String id, Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-publicize")
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.clearValues(stack);
                                this.locale.to(src, "commands.possess.unmark-attribute");
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }

    private CommandSpec getPossessCommand(String id, Attribute<GameProfile> attribute) {
        return CommandSpec.builder()
                .permission("amberadvancedattributes.command.aaa-possess")
                .arguments(GenericArguments.optional(GenericArguments.player(Text.of("player"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Optional<ItemStack> stackOptional = ((Player) src).getItemInHand(HandTypes.MAIN_HAND);
                        Player target = args.<Player>getOne(Text.of("player")).orElse((Player) src);
                        if (stackOptional.isPresent()) {
                            ItemStack stack = stackOptional.get();
                            if (DataUtil.hasData(stack)) {
                                attribute.setValues(stack, ImmutableList.of(target.getProfile()));
                                this.locale.to(src, "commands.possess.mark-attribute", target.getName());
                                return CommandResult.success();
                            }
                        }
                    }
                    this.locale.to(src, "commands.drop.nonexist");
                    return CommandResult.success();
                })
                .build();
    }
}
