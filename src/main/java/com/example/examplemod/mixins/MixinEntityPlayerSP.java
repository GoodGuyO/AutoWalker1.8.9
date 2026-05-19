package com.example.examplemod.mixins;

import com.example.examplemod.AutoWalk.AutoWalker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {
    @Inject(
            method = "onUpdateWalkingPlayer",
            at = @At("TAIL")
    )
    private void onUpdateWalkingPlayerTail(CallbackInfo ci) {
        if (AutoWalker.autoWalker != null &&
                (AutoWalker.autoWalker.isWalking()||AutoWalker.autoWalker.isFollowing())) {
            float[] angles = AutoWalker.autoWalker.getTargetAngles();
            if (angles != null) {
                ((EntityPlayerSP)(Object)this).rotationYaw = smoothRotation(((EntityPlayerSP)(Object)this).rotationYaw, angles[0])%360f;
                ((EntityPlayerSP)(Object)this).rotationPitch = angles[1];
            }
        }
    }

    @Redirect(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z"
            )
    )
    private boolean alwaysSprint(net.minecraft.client.settings.KeyBinding instance) {
        return true;
    }
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void onMessage(String message, CallbackInfo ci){
        if(message.contains("goto")){
            AutoWalker.autoWalker.startWalking(parseToBlockPos(message));
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a开始寻路到目标位置"));
            ci.cancel();
        }else if(message.contains("stop")){
            AutoWalker.autoWalker.stopWalking();
            AutoWalker.autoWalker.stopFollowing();
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c已停止移动"));
            ci.cancel();
        }else if(message.contains("follow")){
            String playerName = extractPlayerName(message);
            if(playerName != null && !playerName.isEmpty()){
                EntityPlayer targetPlayer = findPlayerByName(playerName);
                if(targetPlayer != null){
                    AutoWalker.autoWalker.startFollowing(targetPlayer);
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a开始追随玩家: " + targetPlayer.getName()));
                }else{
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c未找到玩家: " + playerName));
                }
            }else{
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c用法: follow <玩家名> 或 follow stop"));
            }
            ci.cancel();
        }
    }
    // 从消息中提取玩家名字
    private String extractPlayerName(String message){
        if(message == null || message.isEmpty()){
            return null;
        }

        // 查找 "follow" 关键字
        int followIndex = message.indexOf("follow");
        if(followIndex == -1){
            return null;
        }

        // 提取 "follow" 后面的内容
        String afterFollow = message.substring(followIndex + 6).trim();

        // 如果是 "stop" 命令
        if(afterFollow.equalsIgnoreCase("stop") || afterFollow.equalsIgnoreCase("off")){
            AutoWalker.autoWalker.stopFollowing();
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§c已停止追随"));
            return null;
        }

        return afterFollow;
    }

    // 根据名字查找玩家
    private EntityPlayer findPlayerByName(String name){
        if(name == null || name.isEmpty()){
            return null;
        }

        // 在所有加载的实体中查找玩家
        for(Object entity : Minecraft.getMinecraft().theWorld.loadedEntityList){
            if(entity instanceof EntityPlayer){
                EntityPlayer player = (EntityPlayer) entity;
                if(player.getName().equalsIgnoreCase(name)){
                    return player;
                }
            }
        }
        return null;
    }

    //字符串转换是ai写的
    private final Pattern COORDINATE_PATTERN = Pattern.compile(
            "(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)\\s+(-?\\d+\\.?\\d*)"
    );

    /**
     * 从字符串中提取坐标信息并转换为BlockPos
     * 支持格式:
     * - "<player>:goto 123 456 789"
     * - "/goto 123 456 789"
     * - "123 456 789"
     * - 其他包含三个连续数字的字符串
     *
     * @param input 输入字符串
     * @return 解析出的BlockPos对象，如果解析失败返回null
     */
    public BlockPos parseToBlockPos(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        // 查找匹配坐标的部分
        Matcher matcher = COORDINATE_PATTERN.matcher(input);

        if (matcher.find()) {
            try {
                double x = Double.parseDouble(matcher.group(1));
                double y = Double.parseDouble(matcher.group(2));
                double z = Double.parseDouble(matcher.group(3));

                // 将double转换为int（BlockPos使用整数坐标）
                return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            } catch (NumberFormatException e) {
                System.err.println("解析坐标时出错: " + e.getMessage());
                return null;
            }
        }

        System.err.println("未在字符串中找到有效的坐标: " + input);
        return null;
    }

    // 平滑转头相关字段
    private static final float MAX_YAW_CHANGE_PER_TICK = 10.0f; // 每tick最大转向角度

    /**
     * 平滑旋转：将当前角度逐步转向目标角度
     * @param currentYaw 当前角度
     * @param targetYaw 目标角度
     * @return 插值后的角度
     */
    private float smoothRotation(float currentYaw, float targetYaw) {
        // 计算角度差（考虑-180到180的环绕）
        float angleDiff = net.minecraft.util.MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);

        // 限制每次转动的最大角度
        if (angleDiff > MAX_YAW_CHANGE_PER_TICK) {
            angleDiff = MAX_YAW_CHANGE_PER_TICK;
        } else if (angleDiff < -MAX_YAW_CHANGE_PER_TICK) {
            angleDiff = -MAX_YAW_CHANGE_PER_TICK;
        }

        // 返回新的角度
        return net.minecraft.util.MathHelper.wrapAngleTo180_float(currentYaw + angleDiff);
    }
}