package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.*;
import net.minecraft.world.World;
import java.util.*;
import java.util.stream.Collectors;

public class SidebarHelper {

    public static List<String> getSidebarLines() {
        // 获取客户端世界（适用于单人/多人）
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return Collections.emptyList();

        Scoreboard sb = world.getScoreboard();

        // 获取侧边栏槽位的计分项（1 = SIDEBAR）
        ScoreObjective sidebarObjective = sb.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null) return Collections.emptyList();

        Collection<Score> scores = sb.getScores();
        if (scores.isEmpty()) return Collections.emptyList();

        // 按分数降序排序（游戏内从上到下）
        List<Score> sortedScores = scores.stream()
                .sorted(Comparator.comparingInt(Score::getScorePoints).reversed())
                .collect(Collectors.toList());

        List<String> lines = new ArrayList<>();
        for (Score score : sortedScores) {
            String entry = score.getPlayerName();
            String display = formatEntry(sb, entry);
            lines.add(display);
        }
        return lines;
    }

    private static String formatEntry(Scoreboard sb, String entry) {
        for (ScorePlayerTeam team : sb.getTeams()) {
            if (team.getMembershipCollection().contains(entry)) {
                return team.getColorPrefix() + entry + team.getColorSuffix();
            }
        }
        return entry;
    }

    public static String getSidebarTitle() {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return "";
        ScoreObjective obj = world.getScoreboard().getObjectiveInDisplaySlot(1);
        return obj != null ? obj.getDisplayName() : "";
    }
}