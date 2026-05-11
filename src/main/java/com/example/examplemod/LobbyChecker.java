package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class LobbyChecker {
    public static  String lobbyName;
    public LobbyChecker() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void checkLobby(TickEvent.ClientTickEvent e){
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player=mc.thePlayer;
        if(e.side!= Side.CLIENT||e.type!= TickEvent.Type.CLIENT||e.phase!= TickEvent.Phase.START){
            return;
        }
        if(player==null){
            lobbyName="Not In Game";
            return;
        }
        mc.ingameGUI.
    }
}
