package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.Collection;
import java.util.Objects;

import static com.example.examplemod.SidebarHelper.*;

public class LobbyChecker {
    public static  String lobbyName="Not In Game";
    public LobbyChecker() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e){
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player=mc.thePlayer;
        World world=mc.theWorld;
        Scoreboard scoreboard=world.getScoreboard();
        if(Keyboard.isKeyDown(Keyboard.KEY_G)){
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(lobbyName));
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_H)){
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(getSidebarTitlePlain()));
            for (String s:getSidebarLinesPlain()){
                mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(s));
            }
        }
    }
    @SubscribeEvent
    public void checkLobby(TickEvent.ClientTickEvent e){
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player=mc.thePlayer;
        World world=mc.theWorld;
        if(e.side!= Side.CLIENT||e.type!= TickEvent.Type.CLIENT||e.phase!= TickEvent.Phase.START){
            return;
        }
        if(player==null||world==null){
            lobbyName="Not In Game";
            return;
        }
        Scoreboard scoreboard=world.getScoreboard();
        ScoreObjective obj = world.getScoreboard().getObjectiveInDisplaySlot(1);
        if(obj==null){
            lobbyName="Vanilla";
            return;
        }
        String sidebarTitle=getSidebarTitlePlain();
        if(Objects.equals(sidebarTitle, "THE HYPIXEL PIT")){
            lobbyName="HYPIXEL PIT";
            return;
        }
        lobbyName="Not In Hypixel Pit";
        return;
    }
}
