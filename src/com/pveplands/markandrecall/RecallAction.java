package com.pveplands.markandrecall;

import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.CreatureBehaviour;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.sounds.SoundPlayer;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.structures.Structures;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import com.wurmonline.server.zones.AreaSpellEffect;
import com.wurmonline.server.zones.Zones;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class RecallAction implements ActionPerformer, ModAction {
    private static final Random random = new Random();
    private static final Logger logger = Logger.getLogger(MarkAndRecall.getLoggerName(RecallAction.class));
    
    private short actionId;
    private ActionEntry actionEntry;
    
    private Options options;
    
    public RecallAction(Options ops) {
        options = ops;
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(
            actionId, 
            "Recall", 
            "recalling",
            new int[] { 6 /*nomove*/ });
        ModActions.registerAction(actionEntry);
    }
    
    @Override
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item source, @Nonnull Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }
    
    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item target, short num, float counter) {
        short x = target.getDataX();
        short y = target.getDataY();
        boolean surface = (target.getData2() == 0);
        int power = performer.getPower();
        
        int dist = Math.max(Math.abs(x - performer.getTileX()), Math.abs(y - performer.getTileY()));
        
        if (counter == 1f) {
            if (performer.getVehicle() != -10) {
                performer.getCommunicator().sendNormalServerMessage("You can't properly concentrate on the magic while embarked.");
                return true;
            }
            
            if (power == 0 && options.getCastSkill() > 0 && options.getCastFavour() > 0f && performer.getFavor() < options.getCastFavour()) {
                performer.getCommunicator().sendNormalServerMessage("You don't have enough favor to cast this spell.");
                return true;
            }
            
            if (power == 0 && target.getCurrentQualityLevel() < options.getMinUsableQuality()) {
                performer.getCommunicator().sendNormalServerMessage("The rune is in too poor shape to be used.");
                return true;
            }
            
            if (power == 0 && !options.isUndergroundRecall() && (!performer.isOnSurface() || !surface)) {
                performer.getCommunicator().sendNormalServerMessage("The material in the rock interferes with the magic, it would be very dangerous to try this here.");
                return true;
            }
            
            if (power == 0 && options.getMinRecallDistance() > 0 && dist < options.getMinRecallDistance()) {
                performer.getCommunicator().sendNormalServerMessage("The rune's connection seems to be too close nearby to work reliably.");
                return true;
            }
            else if (power == 0 && options.getMaxRecallDistance() > 0 && dist > options.getMaxRecallDistance()) {
                performer.getCommunicator().sendNormalServerMessage("The rune feels dull, it can't connect to its marked spot this far.");
                return true;
            }
            
            if (power == 0 && options.isQualityDistance()) {
                // rune max reach dependent on rune quality level.
                int runeMaxTiles = (int)Math.round(Zones.worldTileSizeX * (target.getCurrentQualityLevel() / 100));
                
                if (dist > runeMaxTiles) {
                    performer.getCommunicator().sendNormalServerMessage("The rune is in too poor shape to reach the marked spot, it is too far away.");
                    return true;
                }
            }
            
            if (power == 0 && !options.isAllowLeading() && performer.getNumberOfFollowers() > 0) {
                performer.getCommunicator().sendNormalServerMessage("You can not recall away while leading creatures.");
                return true;
            }
            
            if (x <= 0 || y <= 0 || x >= Zones.worldTileSizeX || y >= Zones.worldTileSizeY) {
                performer.getCommunicator().sendNormalServerMessage("The rune's markings are too damaged.");
                
                if (power > 0)
                    performer.getCommunicator().sendAlertServerMessage("X, Y coordinates are less than or equal 0, or greater than the actual word size.");
                
                return true;
            }
            
            int tile = surface ? Server.surfaceMesh.getTile(x, y): Server.caveMesh.getTile(x, y);
            if (power == 0 && !options.isWaterRecall() && Terraforming.isTileUnderWater(tile, x, y, surface)) {
                performer.getCommunicator().sendNormalServerMessage("The rune's magic can't get a hold, and it has a slightly wet feeling to it.");
                return true;
            }
            
            if (power == 0 && !options.isLavaRecall() && Tiles.decodeType(tile) == Tiles.Tile.TILE_LAVA.id) {
                performer.getCommunicator().sendNormalServerMessage("The rune suddenly gets very hot, the target location might be dangerous.");
                return true;
            }
            
            if (power == 0 && !surface && Tiles.isSolidCave(Tiles.decodeType(tile))) {
                performer.getCommunicator().sendNormalServerMessage("The rune feels as hard and heavy as solid rock.");
                return true;
            }
            
            if (power == 0 && !options.isSteepRecall()) {
                MeshIO mesh = surface ? Server.surfaceMesh : Server.caveMesh;
                int southTile = mesh.getTile(x + 1, y);
                int eastTile = mesh.getTile(x, y + 1);
                int height = Tiles.decodeHeight(tile);
                mesh = null;
                
                if (Math.abs(Tiles.decodeHeight(eastTile) - height) > options.getMaxSlope() ||  Math.abs(Tiles.decodeHeight(southTile) - height) > options.getMaxSlope()) {
                    performer.getCommunicator().sendNormalServerMessage("The rune almost slips out of your hand, as if it was on a steep slope.");
                    return true;
                }
            }
            
            if (power == 0 && !options.isHolyRecall() && Terraforming.isAltarBlocking(performer, x, y)) {
                performer.getCommunicator().sendNormalServerMessage("The rune does not respond, it seems to briefly emit a divine aura.");
                return true;
            }
            
            if (power == 0 && !options.isVillageRecall()) {
                Village village = Villages.getVillageWithPerimeterAt(x, y, true);

                if (village != null && !village.isActionAllowed(Actions.CAST, performer)) {
                    performer.getCommunicator().sendNormalServerMessage("There seems to be an aura that prevents you from casting this spell where this rune is connected to.");
                    village = null;
                    return true;
                }
            }
            
            Structure structure = Structures.getStructureForTile(x, y, surface);
            if (power == 0 && structure != null && !structure.mayPass(performer)) {
                performer.getCommunicator().sendNormalServerMessage("The rune suddenly feels bulkier, there seems to be something blocking the target location.");
                structure = null;
                return true;
            }

            performer.getCommunicator().sendNormalServerMessage("You concentrate on the rune and imagine the surroundings of the marked spot.");
            
            double mod = 1d - Math.max(0d, Math.min(100d, (target.getCurrentQualityLevel() + target.getRarity() * 10d) / 100d));
            int time = options.getMinActionTime() + (int)(options.getMaxActionTime() - options.getMinActionTime() * mod);
            
            if (options.isActionTimeDistance())
                time += (dist / options.getTilesPerSecond() * 10);
            
            time = Math.min(time, options.getMaxActionTime());
            time = (int)Math.max(options.getMinActionTime(), time * mod);
            
            if (power > 0)
                time = 1;
            
            action.setTimeLeft(time);
            performer.sendActionControl(action.getActionString(), true, time);
            
            if (power == 0 || performer.isVisible())
                Server.getInstance().broadCastAction(performer.getName() + " holds up a rune and concentrates intently.", performer, 5);
        }
        else {
            if (counter * 10f > action.getTimeLeft() + 10) {
                performer.sendActionControl(action.getActionString(), false, 0);
                performer.getCommunicator().sendActionResult(true);
                
                if (options.getCastSkill() > 0 && power == 0) {
                    Skill skill = performer.getSkills().getSkillOrLearn(options.getCastSkill());
                    double skillpower = skill.skillCheck(options.getCastDifficulty(), 0d, true, counter);
                    double requiredpower = dist / Zones.worldTileSizeX * 100d;
                    
                    try { performer.depleteFavor(options.getCastFavour(), false); }
                    catch (Exception ex) { logger.log(Level.SEVERE, String.format("%s casted recall but can't deplete favor.", performer.getName()), ex); } 
                    
                    if (requiredpower >= skillpower) {
                        performer.getCommunicator().sendNormalServerMessage("The recall spell fizzles");
                        performer.playPersonalSound("sound.combat.hit.croc");
                        return true;
                    }
                }
                
                if (target.getData2() == -1)
                    target.setData2(0);
                
                int layer = target.getData2() >> 16;
                int floorLevel = target.getData2() & 65535;
                
                performer.setTeleportPoints(x, y, layer, floorLevel);
                Creature[] followers = new Creature[0];
                
                if ((options.isAllowLeading() && performer.getNumberOfFollowers() > 0) || power > 0)
                    followers = performer.getFollowers();
                
                if (options.isLogRecall()) {
                    logger.info(String.format("%s recalled on rune (%d) to %d, %d (%d) with %d followers.",
                        performer.getName(), target.getWurmId(), x, y, layer, performer.getNumberOfFollowers()));
                    
                    if (performer.getNumberOfFollowers() > 0)
                        for (Creature follower : followers)
                            logger.info(String.format("Following: %s (%d)", follower.getName(), follower.getWurmId()));
                }

                if (power == 0 || performer.isVisible())
                    Server.getInstance().broadCastAction(performer.getName() + " suddenly disappears.", performer, 5);
                
                new AreaSpellEffect(performer.getWurmId(), x, y, layer, (byte)34, System.currentTimeMillis() + 2000, 100f, floorLevel, 0, false);
                
                if (performer.startTeleporting()) {
                    performer.getCommunicator().sendNormalServerMessage("You feel a strange sensation and suddenly appear in another location.");
                    performer.getCommunicator().sendTeleport(false);
                    
                    if (power == 0 || performer.isVisible())
                        Server.getInstance().broadCastAction(performer.getName() + " suddenly appears.", performer, 5);
                    
                    for (Creature follower : followers)
                        CreatureBehaviour.blinkTo(follower, performer.getPosX(), performer.getPosY(), layer, performer.getPositionZ(), performer.getBridgeId(), floorLevel);
                    
                    if (power == 0 && options.isLoseQuality())
                        target.setQualityLevel(target.getCurrentQualityLevel() - random.nextFloat());
                    
                    if (power == 0 && options.isTakeDamage())
                        target.setDamage(target.getDamage() + random.nextFloat(), true);
                    
                    if (options.isLightningEffect() && performer.isVisible())
                        Zones.flashSpell(x, y, 0, performer);
                    
                    if (options.isPlaySound()) {
                        if (power == 0 || performer.isVisible())
                            SoundPlayer.playSound(options.getSound(), x, y, surface, 1.6f);
                        
                        performer.playPersonalSound(options.getSound());
                    }
                }
                
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public short getActionId() {
        return actionId;
    }
    
    public ActionEntry getActionEntry() {
        return actionEntry;
    }
}
