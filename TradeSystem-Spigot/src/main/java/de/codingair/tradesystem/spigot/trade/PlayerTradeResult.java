package de.codingair.tradesystem.spigot.trade;

import de.codingair.codingapi.utils.ChatColor;
import de.codingair.tradesystem.spigot.TradeSystem;
import de.codingair.tradesystem.spigot.trade.gui.layout.types.impl.economy.EconomyIcon;
import de.codingair.tradesystem.spigot.trade.gui.layout.utils.Perspective;
import de.codingair.tradesystem.spigot.utils.Lang;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PlayerTradeResult extends TradeResult {
    private final Trade trade;
    private final Player player;

    public PlayerTradeResult(@NotNull Trade trade, @NotNull Player player, @NotNull Perspective perspective) {
        super(perspective);
        this.trade = trade;
        this.player = player;
    }

    @NotNull
    List<String> buildItemReport() {
        return buildItemReport(itemToNameMapper());
    }

    @NotNull
    public static Function<ItemStack, String> itemToNameMapper() {
        return item -> {
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                assert meta != null;

                if (meta.hasDisplayName())
                    return TradeSystem.handler().isOnlyDisplayNameInMessage() ? meta.getDisplayName() : formatName(item.getType().name()) + " (" + ChatColor.stripColor(meta.getDisplayName()) + ")";
            }

            return formatName(item.getType().name());
        };
    }

    /**
     * @param mapper A function that maps an {@link ItemStack} to a {@link String} that is used in the report.
     * @return An unsorted list of lines that can be used in a report. Will be sorted lexicographically before printing.
     */
    @NotNull
    public List<String> buildItemReport(Function<ItemStack, String> mapper) {
        List<String> lines = new ArrayList<>();

        Map<ItemStack, Integer> receiving = new HashMap<>();
        Map<ItemStack, Integer> sending = new HashMap<>();

        for (ItemStack item : sendingItems) {
            int amount = item.getAmount();
            item.setAmount(1);
            sending.merge(item, amount, Integer::sum);
        }

        for (ItemStack item : receivingItems) {
            int amount = item.getAmount();
            item.setAmount(1);
            receiving.merge(item, amount, Integer::sum);
        }

        receiving.forEach((item, amount) -> {
            item.setAmount(amount);

            String itemName = Lang.get("Trade_Finish_Report_Object_Item", player,
                    new Lang.P("amount", String.valueOf(amount)),
                    new Lang.P("item", mapper.apply(item))
            );

            Lang.P info = new Lang.P("object", itemName);
            lines.add(Lang.get("Trade_Finish_Report_Receive", player, info));
        });

        sending.forEach((item, amount) -> {
            item.setAmount(amount);

            String itemName = Lang.get("Trade_Finish_Report_Object_Item", player,
                    new Lang.P("amount", String.valueOf(amount)),
                    new Lang.P("item", mapper.apply(item))
            );

            Lang.P info = new Lang.P("object", itemName);
            lines.add(Lang.get("Trade_Finish_Report_Give", player, info));
        });

        return lines;
    }

    @NotNull
    List<String> buildEconomyReport() {
        List<String> lines = new ArrayList<>();

        for (EconomyIcon<?> icon : economyIcons) {
            BigDecimal diff = icon.getOverallDifference(trade, perspective);
            if (diff.signum() == 0) continue;
            boolean receive = diff.signum() > 0;

            boolean singular = diff.equals(BigDecimal.ONE);
            if (receive) {
                Lang.P info = new Lang.P("object",
                        EconomyIcon.makeFancyString(diff, icon.isDecimal()) + " " + icon.getName(player, singular));
                lines.add(Lang.get("Trade_Finish_Report_Receive", player, info));
            } else {
                Lang.P info = new Lang.P("object",
                        EconomyIcon.makeFancyString(diff.negate(), icon.isDecimal()) + " " + icon.getName(player, singular));
                lines.add(Lang.get("Trade_Finish_Report_Give", player, info));
            }
        }

        return lines;
    }

    @NotNull
    public static String formatName(@NotNull String name) {
        StringBuilder b = new StringBuilder(name.toLowerCase());

        int i = 0;
        do {
            b.replace(i, i + 1, b.substring(i, i + 1).toUpperCase());
            i = b.indexOf("_", i);
            if (i >= 0) b.replace(i, i + 1, " ");
            i++;
        } while (i > 0 && i < b.length());

        return b.toString().trim();
    }

    /**
     * @return The current player.
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The trade.
     */
    @NotNull
    public Trade getTrade() {
        return trade;
    }
}
