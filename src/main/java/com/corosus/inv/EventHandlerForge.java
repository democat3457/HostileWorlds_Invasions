package com.corosus.inv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.EnumStatus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import CoroUtil.util.CoroUtilBlock;
import CoroUtil.util.Vec3;

import com.corosus.inv.ai.BehaviorModifier;
import com.corosus.inv.ai.tasks.TaskCallForHelp;
import com.corosus.inv.ai.tasks.TaskDigTowardsTarget;
import com.corosus.inv.config.InvConfig;

public class EventHandlerForge {
	
	public static String dataPlayerInvasionActive = "HW_dataPlayerInvasionActive";
	public static String dataPlayerInvasionWaveCountCur = "HW_dataPlayerInvasionWaveCountCur";
	public static String dataPlayerInvasionWaveCountMax = "HW_dataPlayerInvasionWaveCountMax";
	
	public float inventoryStages = 5;
	
	public HashMap<Integer, EquipmentForDifficulty> lookupDifficultyToEquipment = new HashMap<Integer, EquipmentForDifficulty>();
	
	public Class[] tasksToInject = new Class[] { TaskDigTowardsTarget.class, TaskCallForHelp.class };
	public int[] taskPriorities = {5, 5};
	
	public static class EquipmentForDifficulty {
		
		//ordered head to toe
		private List<ItemStack> listArmor;
		private ItemStack weapon;
		//unused for now, worth considering in future
		private List<Potion> listPotions;

		public EquipmentForDifficulty() {
			
		}
		
		public List<ItemStack> getListArmor() {
			return listArmor;
		}

		public void setListArmor(List<ItemStack> listArmor) {
			this.listArmor = listArmor;
		}

		public ItemStack getWeapon() {
			return weapon;
		}

		public void setWeapon(ItemStack weapon) {
			this.weapon = weapon;
		}

		public List<Potion> getListPotions() {
			return listPotions;
		}

		public void setListPotions(List<Potion> listPotions) {
			this.listPotions = listPotions;
		}
		
	}
	
	public EventHandlerForge() {
		
		//init inventories for difficulties
		
		
		EquipmentForDifficulty obj = new EquipmentForDifficulty();
		List<ItemStack> listItems = new ArrayList<ItemStack>();
		obj.setListArmor(listItems);
		lookupDifficultyToEquipment.put(0, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.leather_helmet));
		listItems.add(new ItemStack(Items.leather_chestplate));
		listItems.add(new ItemStack(Items.leather_leggings));
		listItems.add(new ItemStack(Items.leather_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.wooden_sword));
		lookupDifficultyToEquipment.put(1, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.chainmail_helmet));
		listItems.add(new ItemStack(Items.chainmail_chestplate));
		listItems.add(new ItemStack(Items.chainmail_leggings));
		listItems.add(new ItemStack(Items.chainmail_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.stone_sword));
		lookupDifficultyToEquipment.put(2, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.iron_helmet));
		listItems.add(new ItemStack(Items.iron_chestplate));
		listItems.add(new ItemStack(Items.iron_leggings));
		listItems.add(new ItemStack(Items.iron_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.iron_sword));
		lookupDifficultyToEquipment.put(3, obj);
		
		obj = new EquipmentForDifficulty();
		listItems = new ArrayList<ItemStack>();
		listItems.add(new ItemStack(Items.diamond_helmet));
		listItems.add(new ItemStack(Items.diamond_chestplate));
		listItems.add(new ItemStack(Items.diamond_leggings));
		listItems.add(new ItemStack(Items.diamond_boots));
		obj.setListArmor(listItems);
		obj.setWeapon(new ItemStack(Items.diamond_sword));
		lookupDifficultyToEquipment.put(4, obj);
	}
	
	@SubscribeEvent
	public void canSleep(PlayerSleepInBedEvent event) {
		if (InvConfig.preventSleepDuringInvasions) {
			if (isInvasionTonight(event.entityPlayer.worldObj)) {
				EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
				player.addChatMessage(new ChatComponentText("You can't sleep during invasion nights!"));
				event.result = EnumStatus.NOT_SAFE;
			} else {
				
			}
		}
	}
	
	@SubscribeEvent
	public void entityCreated(EntityJoinWorldEvent event) {
		if (event.entity instanceof EntityCreature) {
			EntityCreature ent = (EntityCreature) event.entity;
			if (ent.getEntityData().getBoolean(BehaviorModifier.dataEntityEnhanced)) {
				BehaviorModifier.addTaskIfMissing(ent, TaskDigTowardsTarget.class, tasksToInject, taskPriorities[0]);
			}
		}
	}
	
	@SubscribeEvent
	public void tickServer(ServerTickEvent event) {
		
		if (event.phase == Phase.START) {
			//System.out.println("tick ZA");
			//ZombieAwareness.instance.onTick(MinecraftServer.getServer());
			
			
			World world = DimensionManager.getWorld(0);
			if (world != null) {
				if (world.getTotalWorldTime() % 20 == 0) {
					for (EntityPlayer player : world.playerEntities) {
						tickPlayer(player);
					}
				}
			}
		}
	}
	
	public void tickPlayer(EntityPlayer player) {
		World world = player.worldObj;
		BlockPos pos = player.getPosition();
		float difficultyScale = getDifficultyScaleForPos(world, pos);
		///Chunk chunk = world.getChunkFromBlockCoords(pos);
		//long inhabTime = chunk.getInhabitedTime();
		//System.out.println("difficultyScale: " + difficultyScale);
		
		//start at "1"
		long dayNumber = (world.getWorldTime() / 24000) + 1;
		//System.out.println("daynumber: " + dayNumber + " - " + world.getWorldTime() + " - " + world.isDaytime());
		
		boolean invasionActive = false;
		
		//debug
		//difficultyScale = 1F;
		
		boolean activeBool = player.getEntityData().getBoolean(dataPlayerInvasionActive);
		
		//TODO: add a visual cue for invasion coming tonight + active invasion
		
		//track state of invasion for proper init and reset for wave counts, etc
		//new day starts just as sun is rising, so invasion stops just at the right time when sun is imminent, they burn 300 ticks before invasion ends, thats ok
		//FYI night val is based on sunlight level, so its not night ends @ 24000 cycle, its a bit before, 400ish ticks before, thats ok
		boolean invasionOnThisNight = isInvasionTonight(world);
		if (invasionOnThisNight && !world.isDaytime()) {
			
			invasionActive = true;
			//TODO: bug, on second day, invasion start method didnt trigger, but invasion IS active
			if (!activeBool) {
				//System.out.println("triggering invasion start");
				invasionStart(player, difficultyScale);
			}
		} else {
			invasionActive = false;
			if (activeBool) {
				//System.out.println("triggering invasion stop");
				invasionStopReset(player);
			}
		}
		

		System.out.println("invasion?: " + invasionActive + " - day# " + dayNumber + " - time: " + world.getWorldTime() + " - invasion tonight: " + invasionOnThisNight);
		System.out.println("inv info: " + getInvasionDebug(difficultyScale));
		
		//debug
		//invasionActive = true;
		//world.getDifficultyForLocation(player.playerLocation);
		
		if (invasionActive) {
			if (player.onGround && world.getTotalWorldTime() % 200 == 0) {
			
				int range = getTargettingRangeBuff(difficultyScale);
				double moveSpeedAmp = 1.2D;
				
				//TODO: instead of this expensive method and entity iteration, we could make distant targetting a targetTask! 
				List<EntityCreature> listEnts = world.getEntitiesWithinAABB(EntityCreature.class, new AxisAlignedBB(pos, pos).expand(range, range, range));
				
				//System.out.println("ents: " + listEnts.size());
				
				TaskDigTowardsTarget task = new TaskDigTowardsTarget();
				
				int modifyRange = 100;
				float chanceToEnhance = getDigChanceBuff(difficultyScale);
				//TODO: consider making the digging tasks disable after invasions "ends" so that player wont get surprised later on in day if a zombie survives and takes a while to get to him
				BehaviorModifier.enhanceZombiesToDig(world, new Vec3(player.posX, player.posY, player.posZ), 
						tasksToInject, taskPriorities[0], 
						modifyRange, chanceToEnhance);
				
				for (EntityCreature ent : listEnts) {
					if (ent instanceof IMob && ent instanceof EntityZombie) {
						
						if (ent.getNavigator().noPath()) {
						
							double distToPlayer = ent.getDistanceToEntity(player);
							
							double followDist = ent.getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
							
							ent.setAttackTarget(player);
							
							if (distToPlayer <= followDist) {
								boolean success = ent.getNavigator().tryMoveToEntityLiving(player, moveSpeedAmp);
								//System.out.println("success? " + success + "- move to player: " + ent + " -> " + player);
							} else {
						        int x = MathHelper.floor_double(player.posX);
						        int y = MathHelper.floor_double(player.posY);
						        int z = MathHelper.floor_double(player.posZ);
						        
						        double d = x+0.5F - ent.posX;
						        double d2 = z+0.5F - ent.posZ;
						        double d1;
						        d1 = y+0.5F - (ent.posY + (double)ent.getEyeHeight());
						        
						        double d3 = MathHelper.sqrt_double(d * d + d2 * d2);
						        float f2 = (float)((Math.atan2(d2, d) * 180D) / 3.1415927410125732D) - 90F;
						        float f3 = (float)(-((Math.atan2(d1, d3) * 180D) / 3.1415927410125732D));
						        float rotationPitch = -f3;//-ent.updateRotation(rotationPitch, f3, 180D);
						        float rotationYaw = f2;//updateRotation(rotationYaw, f2, 180D);
						        
						        EntityLiving center = ent;
						        
						        Random rand = world.rand;
						        
						        float randLook = rand.nextInt(90)-45;
						        //int height = 10;
						        double dist = (followDist * 0.75D) + rand.nextInt((int)followDist / 2);//rand.nextInt(26)+(queue.get(0).retryState * 6);
						        int gatherX = (int)Math.floor(center.posX + ((double)(-Math.sin((rotationYaw+randLook) / 180.0F * 3.1415927F)/* * Math.cos(center.rotationPitch / 180.0F * 3.1415927F)*/) * dist));
						        int gatherY = (int)center.posY;//Math.floor(center.posY-0.5 + (double)(-MathHelper.sin(center.rotationPitch / 180.0F * 3.1415927F) * dist) - 0D); //center.posY - 0D;
						        int gatherZ = (int)Math.floor(center.posZ + ((double)(Math.cos((rotationYaw+randLook) / 180.0F * 3.1415927F)/* * Math.cos(center.rotationPitch / 180.0F * 3.1415927F)*/) * dist));
						        
						        Block block = world.getBlockState(new BlockPos(gatherX, gatherY, gatherZ)).getBlock();
						        int tries = 0;
						        if (!CoroUtilBlock.isAir(block)) {
						        	int offset = -5;
			    			        
			    			        while (tries < 30) {
			    			        	if (CoroUtilBlock.isAir(block) || !block.isSideSolid(world, new BlockPos(gatherX, gatherY, gatherZ), EnumFacing.UP)) {
			    			        		break;
			    			        	}
			    			        	gatherY += offset++;
			    			        	block = world.getBlockState(new BlockPos(gatherX, gatherY, gatherZ)).getBlock();
			    			        	tries++;
			    			        }
						        } else {
						        	//int offset = 0;
						        	while (tries < 30) {
						        		if (!CoroUtilBlock.isAir(block) && block.isSideSolid(world, new BlockPos(gatherX, gatherY, gatherZ), EnumFacing.UP)) break;
						        		gatherY -= 1;//offset++;
						        		block = world.getBlockState(new BlockPos(gatherX, gatherY, gatherZ)).getBlock();
			    			        	tries++;
						        	}
						        }
						        
						        if (tries < 30) {
						        	boolean success = ent.getNavigator().tryMoveToXYZ(gatherX, gatherY, gatherZ, moveSpeedAmp);
						        	//System.out.println("pp success? " + success + "- move to player: " + ent + " -> " + player);
						        }
							}
						}
					}
				}
			}
			
			if (world.getTotalWorldTime() % 10 == 0) {
				int spawnCountCur = player.getEntityData().getInteger(dataPlayerInvasionWaveCountCur);
				int spawnCountMax = player.getEntityData().getInteger(dataPlayerInvasionWaveCountMax);
				if (spawnCountCur < spawnCountMax) {
					boolean spawned = spawnNewMobSurface(player, difficultyScale);
					if (spawned) {
						spawnCountCur++;
						player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, spawnCountCur);
						System.out.println("spawned mob, wave count: " + spawnCountCur + " of " + spawnCountMax);
					}
				}
			}
		}
	}
	
	public void invasionStart(EntityPlayer player, float difficultyScale) {
		System.out.println("invasion started");
		player.getEntityData().setBoolean(dataPlayerInvasionActive, true);
		
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountMax, getSpawnCountBuff(difficultyScale));
		player.getEntityData().setInteger(dataPlayerInvasionWaveCountCur, 0);
	}
	
	public void invasionStopReset(EntityPlayer player) {
		System.out.println("invasion ended");
		player.getEntityData().setBoolean(dataPlayerInvasionActive, false);
	}
	
	public boolean spawnNewMobSurface(EntityLivingBase player, float difficultyScale) {
        
        //adjusted to work best with new targetting range base value of 30
        int minDist = 30;//ZAConfigSpawning.extraSpawningDistMin;
        int maxDist = 60;//ZAConfigSpawning.extraSpawningDistMax;
        int range = maxDist*2;
        
        Random rand = player.worldObj.rand;
        
        for (int tries = 0; tries < 5; tries++) {
	        int tryX = MathHelper.floor_double(player.posX) - (range/2) + (rand.nextInt(range));
	        int tryZ = MathHelper.floor_double(player.posZ) - (range/2) + (rand.nextInt(range));
	        int tryY = player.worldObj.getHeight(new BlockPos(tryX, 0, tryZ)).getY();
	
	        if (player.getDistance(tryX, tryY, tryZ) < minDist || player.getDistance(tryX, tryY, tryZ) > maxDist || !canSpawnMob(player.worldObj, tryX, tryY, tryZ) || player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)) >= 6) {
	        	//System.out.println("light: " + player.worldObj.getLightFromNeighbors(new BlockPos(tryX, tryY, tryZ)));
	            continue;
	        }
	
	        EntityZombie entZ = new EntityZombie(player.worldObj);
			entZ.setPosition(tryX, tryY, tryZ);
			entZ.onInitialSpawn(player.worldObj.getDifficultyForLocation(new BlockPos(entZ)), (IEntityLivingData)null);
			enhanceMobForDifficulty(entZ, difficultyScale);
			player.worldObj.spawnEntityInWorld(entZ);
			
			/*if (ZAConfigSpawning.extraSpawningAutoTarget) */entZ.setAttackTarget(player);
			
	        //if (ZAConfig.debugConsoleSpawns) ZombieAwareness.dbg("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);
			//System.out.println("spawnNewMobSurface: " + tryX + ", " + tryY + ", " + tryZ);
	        
	        return true;
        }
        
        return false;
    }
	
	public boolean canSpawnMob(World world, int x, int y, int z) {
        //Block id = world.getBlockState(new BlockPos(x-1,y,z)).getBlock();//Block.pressurePlatePlanks.blockID;
		Block id = world.getBlockState(new BlockPos(x,y,z)).getBlock();//Block.pressurePlatePlanks.blockID;

        /*if (id == Block.grass.blockID || id == Block.stone.blockID || id == Block.tallGrass.blockID || id == Block.grass.blockID || id == Block.sand.blockID) {
            return true;
        }*/
        if (CoroUtilBlock.isAir(id) || id.getMaterial() == Material.leaves) {
        	return false;
        }
        return true;
    }
	
	public void enhanceMobForDifficulty(EntityCreature ent, float difficultyScale) {
		/*settings to consider:
		 *- health
		 *- speed
		 *- inventory 
		 *- potions
		 */
		
		//determines what integer stage of inventory we should be at based on the difficulty scale
		//code adapts for allowing for easily adding in more inventory stages if needed
		
		
		int inventoryStage = getInventoryStageBuff(difficultyScale);
		
		EquipmentForDifficulty equipment = lookupDifficultyToEquipment.get(inventoryStage);
		if (equipment != null) {
			ent.setCurrentItemOrArmor(0, equipment.getWeapon());
			for (int i = 0; i < 4; i++) {
				if (equipment.getListArmor().size() >= i+1) {
					ent.setCurrentItemOrArmor(i+1, equipment.getListArmor().get(i));
				} else {
					ent.setCurrentItemOrArmor(i+1, null);
				}
			}
			//remove any chance of equipment dropping
			for (int i = 0; i < 5; i++) {
				ent.setEquipmentDropChance(i, 0);
			}
			
		} else {
			System.out.println("error, couldnt find equipment for difficulty value: " + inventoryStage);
		}
		
		
	}
	
	public float getDifficultyScaleForPos(World world, BlockPos pos) {
		Chunk chunk = world.getChunkFromBlockCoords(pos);
		if (chunk != null) {
			long inhabTime = chunk.getInhabitedTime();
			float scale = convertInhabTimeToDifficultyScale(inhabTime);
			return scale;
			
		}
		return 0F;
	}
	
	/**
	 * 
	 * Returns value between 0 and 1 based on configured values
	 * 
	 * @param inhabTime
	 * @return
	 */
	public float convertInhabTimeToDifficultyScale(long inhabTime) {
		float scale = (float)inhabTime / (float)InvConfig.maxTicksForDifficulty;
		return scale;
	}
	
	public boolean isInvasionTonight(World world) {
		long dayNumber = (world.getWorldTime() / 24000) + 1;
		return dayNumber >= InvConfig.warmupDays && (dayNumber-InvConfig.warmupDays == 0 || (dayNumber-InvConfig.warmupDays) % Math.max(1, InvConfig.daysBetweenAttacks) == 0);
	}
	
	public int getSpawnCountBuff(float difficultyScale) {
		int maxSpawnsAllowed = 50;
		int initialSpawns = 10;
		float scaleRate = 1F;
		return MathHelper.clamp_int(((int) ((float)(maxSpawnsAllowed) * difficultyScale * scaleRate)), initialSpawns, maxSpawnsAllowed);
	}
	
	public int getTargettingRangeBuff(float difficultyScale) {
		int initialRange = 30;
		int max = 256;
		float scaleRate = 1F;
		return MathHelper.clamp_int(((int) ((float)(max) * difficultyScale * scaleRate)), initialRange, max); 
	}
	
	public float getDigChanceBuff(float difficultyScale) {
		float initial = 0.1F;
		float max = 1F;
		float scaleRate = 1F;
		return MathHelper.clamp_float((((float)(max) * difficultyScale * scaleRate)), initial, max);
	}
	
	public int getInventoryStageBuff(float difficultyScale) {
		float scaleDivide = 1F / inventoryStages;
		int inventoryStage = 0;
		for (int i = 0; i < inventoryStages; i++) {
			if (difficultyScale <= scaleDivide * (i+1)) {
				inventoryStage = i;
				break;
			}
		}
		return inventoryStage;
	}
	
	public String getInvasionDebug(float difficultyScale) {
		return "spawncount: " + getSpawnCountBuff(difficultyScale) + 
				" | targetrange: " + getTargettingRangeBuff(difficultyScale) + 
				" | dig chance: " + getDigChanceBuff(difficultyScale) + 
				" | inventory stage: " + getInventoryStageBuff(difficultyScale);
	}
}
