package com.example.examplemod.AutoWalk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;

public class AutoWalker {
    public static AutoWalker autoWalker=null;
    // 当前目标角度
    private float[] currentTargetAngles = null;
    private boolean isWalking=false;
    boolean needsJump=false;
    LinkedList<BlockPos> currentPath=null;
    public BlockPos target=null;

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    // 追随玩家相关字段
    boolean isFollowing=false;
    EntityPlayer targetPlayer=null;
    private int followUpdateCounter=0;
    private static final int FOLLOW_UPDATE_INTERVAL=0; // 每10tick更新一次路径

    public AutoWalker(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void startWalking(BlockPos targetIn){
        // 如果正在追随，先停止追随
        if(isFollowing){
            stopFollowing();
        }
        setWalking(true);
        target=targetIn;
        Minecraft.getMinecraft().thePlayer.movementInput=new ModMovementInput(this);
        currentPath=PathFinder.findPath(Minecraft.getMinecraft().theWorld, Minecraft.getMinecraft().thePlayer.getPosition().down(), target);
    }

    public void stopWalking(){
        setWalking(false);
        target=null;
        Minecraft.getMinecraft().thePlayer.movementInput=new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
        currentPath=null;
    }

    // 开始追随指定玩家
    public void startFollowing(EntityPlayer player){
        if(player==null){
            return;
        }
        // 如果正在行走，先停止行走
        if(isWalking()){
            stopWalking();
        }
        isFollowing=true;
        targetPlayer=player;
        Minecraft.getMinecraft().thePlayer.movementInput=new ModMovementInput(this);
        followUpdateCounter=0;
    }

    // 停止追随
    public void stopFollowing(){
        isFollowing=false;
        targetPlayer=null;
        Minecraft.getMinecraft().thePlayer.movementInput=new MovementInputFromOptions(Minecraft.getMinecraft().gameSettings);
        currentPath=null;
    }
    public static BlockPos getPlayerBlockPos(EntityPlayer player){
        BlockPos blockpos = new BlockPos(player.posX, player.getEntityBoundingBox().minY, player.posZ);
        return blockpos;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e){
        if(e.phase!= TickEvent.Phase.START){
            return;
        }
        Minecraft mc=Minecraft.getMinecraft();
        EntityPlayerSP player=mc.thePlayer;
        if(player==null||mc.theWorld==null){
            return;
        }

        if(player.isRiding()){
            stopAll();
        }

        // 处理追随玩家逻辑
        if(isFollowing && targetPlayer!=null){
            handleFollowing(mc, player);
            return;
        }

        // 原有的寻路逻辑
        if(!isWalking() ||target==null){
            return;
        }

        BlockPos blockpos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);

        // 如果到达目标，停止行走
        if (blockpos.equals(target)) {
            stopWalking();
            return;
        }

        // 如果当前路径不包含当前位置或路径为空，重新计算路径
        if (currentPath == null || currentPath.isEmpty() || !currentPath.contains(blockpos)) {
            BlockPos startPos = blockpos;

            if (!PathFinder.isStandable(mc.theWorld, startPos)) {
                System.out.println("[AutoWalker] 无法找到可行的起点位置");
                stopWalking();
                return;
            }

            currentPath = PathFinder.findPath(mc.theWorld, startPos, target);
            if (currentPath == null || currentPath.isEmpty()) {
                System.out.println("[AutoWalker] 无法找到从 " + startPos + " 到 " + target + " 的路径");
                stopWalking();
                return;
            }
        }

        gotoNextBlock();
    }
    public void gotoNextBlock(){
        // 获取下一个目标方块
        Minecraft mc = Minecraft.getMinecraft();
        BlockPos blockpos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);
        int currentIndex = currentPath.indexOf(blockpos);
        if (currentIndex >= 0 && currentIndex < currentPath.size() - 1) {
            BlockPos next = currentPath.get(currentIndex + 1);
            if(next.getY()!=blockpos.getY()){
                if(currentPath.size()>=currentIndex + 1 + 2){
                    next=currentPath.get(currentIndex + 2);
                }
            }
            // 转向下一个方块
            float targetYaw = getYawRotToBLockPos(mc.thePlayer, next);
            float targetPitch = 0.0f;
            setTargetAngles(targetYaw, targetPitch);

            // 判断是否需要跳跃（下一个方块比当前高）
            needsJump = next.getY() > blockpos.getY();
        }
    }
    @SubscribeEvent
    public void onWorld(WorldEvent.Load e){
        stopAll();
    }
    private void stopAll(){
        if(isWalking()){
            stopWalking();
        }
        if(isFollowing){
            stopFollowing();
        }
    }

    // 处理追随玩家的逻辑
    private void handleFollowing(Minecraft mc, EntityPlayerSP player){
        // 检查目标玩家是否还存在
        if(targetPlayer.isDead || targetPlayer.getEntityWorld()!=mc.theWorld){
            stopFollowing();
            return;
        }

        BlockPos currentPlayerPos = getPlayerBlockPos(player);
        BlockPos targetPos = getPlayerBlockPos(targetPlayer);

        // 每隔一定间隔更新路径
        followUpdateCounter++;
        if(followUpdateCounter>=FOLLOW_UPDATE_INTERVAL || currentPath==null || currentPath.isEmpty()){
            followUpdateCounter=0;
            currentPath = PathFinder.findPath(mc.theWorld, currentPlayerPos, targetPos);

            // 如果找不到路径，尝试直接朝目标玩家方向移动
            if(currentPath==null || currentPath.isEmpty()){
                // 转向下一个方块
                float targetYaw = getYawRotToBLockPos(player, targetPos);
                float targetPitch = 0.0f;
                setTargetAngles(targetYaw, targetPitch);
                //player.rotationYaw = smoothRotation(player.rotationYaw, getYawRotToBLockPos(player, targetPos));
                needsJump = targetPos.getY() > currentPlayerPos.getY();
                return;
            }
        }

        // 沿着路径移动
        gotoNextBlock();
    }

    //返回实体看向方块所需的横向角度,这个函数是ai写的
    public float getYawRotToBLockPos(Entity entity, BlockPos blockPos){
        if (entity == null || blockPos == null) {
            return 0.0f;
        }

        double dx = blockPos.getX() + 0.5 - entity.posX;
        double dz = blockPos.getZ() + 0.5 - entity.posZ;

        double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;

        return net.minecraft.util.MathHelper.wrapAngleTo180_float((float) yaw);
    }

    public boolean isWalking() {
        return isWalking;
    }

    public void setWalking(boolean walking) {
        isWalking = walking;
    }
    /**
     * 获取当前的目标角度 [yaw, pitch]
     * @return 角度数组，如果为空则返回 null
     */
    public float[] getTargetAngles() {
        return currentTargetAngles;
    }

    /**
     * 设置目标角度
     * @param yaw 偏航角
     * @param pitch 俯仰角
     */
    public void setTargetAngles(float yaw, float pitch) {
        this.currentTargetAngles = new float[]{yaw, pitch};
    }
}
