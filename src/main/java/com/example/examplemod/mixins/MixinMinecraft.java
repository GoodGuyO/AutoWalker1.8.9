package com.example.examplemod.mixins;

import com.example.examplemod.AutoWalk.AutoWalker;
import com.example.examplemod.AutoWalk.PathFinder;
import com.example.examplemod.AutoWalk.WalkBlockRenderer;
import com.example.examplemod.LobbyChecker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "startGame", at = @At("HEAD"))
    public void onStart(CallbackInfo ci){
        new LobbyChecker();
        new PathFinder();
        AutoWalker.autoWalker=new AutoWalker();
        new WalkBlockRenderer();
    }
}
