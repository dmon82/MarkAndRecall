package com.pveplands.markandrecall;

import com.wurmonline.mesh.MeshIO;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Terraforming;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class MarkAction implements ActionPerformer, BehaviourProvider, ModAction {
    private static Logger logger = logger = Logger.getLogger(MarkAndRecall.getLoggerName(MarkAction.class));
    
    private short actionId;
    private ActionEntry actionEntry;
    
    private Options options;
    
    public MarkAction(Options ops) {
        options = ops;
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(
            actionId, 
            "Mark", 
            "marking rune", 
            new int[] { 6 /*nomove*/ });
        ModActions.registerAction(actionEntry);
    }
    
    @Override
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item source, @Nonnull Item target, short num, float counter) {
        return action(action, performer, target, num, counter);
    }
    
    @Override
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item target, short num, float counter) {
        int power = performer.getPower();
        
        if (counter == 1f) {
            if (!options.isAllowRemark() && (target.getData1() != -1 || target.getData2() != -1)) {
                performer.getCommunicator().sendNormalServerMessage("The rune already has markings on it.");
                return true;
            }
            if (power == 0 && !options.isUndergroundMark() && !performer.isOnSurface()) {
                performer.getCommunicator().sendNormalServerMessage("You won't be able to mark the rune based on your surroundings here.");
                return true;
            }
            
            int tile = performer.isOnSurface() ? Server.surfaceMesh.getTile(performer.getTilePos()) : Server.caveMesh.getTile(performer.getTilePos());
            if (power == 0 && !options.isWaterMark() && Terraforming.isTileUnderWater(tile, performer.getTileX(), performer.getTileY(), performer.isOnSurface())) {
                performer.getCommunicator().sendNormalServerMessage("The water is too deep for the rune's magic to get a hold.");
                return true;
            }
            
            if (power == 0 && !options.isSteepMark()) {
                MeshIO mesh = performer.isOnSurface() ? Server.surfaceMesh : Server.caveMesh;
                int x = performer.getTileX();
                int y = performer.getTileY();
                int southTile = mesh.getTile(x + 1, y);
                int eastTile = mesh.getTile(x, y + 1);
                int height = Tiles.decodeHeight(tile);
                
                if (Math.abs(Tiles.decodeHeight(eastTile) - height) > options.getMaxSlope() ||  Math.abs(Tiles.decodeHeight(southTile) - height) > options.getMaxSlope()) {
                    performer.getCommunicator().sendNormalServerMessage("This tile is too steep, the magic would flow away.");
                    return true;
                }
            }
            
            if (power == 0 && !options.isHolyMark() && Terraforming.isAltarBlocking(performer, performer.getTileX(), performer.getTileY())) {
                performer.getCommunicator().sendNormalServerMessage("The holy ground in this place makes it impossible for the rune's magic to work.");
                return true;
            }
            
            if (power == 0 && !options.isLavaMark() && Tiles.decodeType(tile) == Tiles.Tile.TILE_LAVA.id) {
                performer.getCommunicator().sendNormalServerMessage("This location can not be transcribed to the rune with benevolent magic.");
                return true;
            }
            
            if (power == 0 && !options.isVillageMark()) {
                Village village = Villages.getVillageWithPerimeterAt(performer.getTileX(), performer.getTileY(), true);

                if (village != null) {
                    if (!village.isActionAllowed(Actions.CAST, performer)) {
                        performer.getCommunicator().sendNormalServerMessage("This village emits an aura that prevents you from casting this spell here.");
                        return true;
                    }
                }
            }
            
            performer.getCommunicator().sendNormalServerMessage("You start using some clutter from the ground to scratch markings on the rune.");
            
            double mod = 1d - Math.max(0d, Math.min(100d, (target.getCurrentQualityLevel() + target.getRarity() * 10d) / 100d));
            int time = (int)Math.ceil(options.getMaxActionTime() - (options.getMaxActionTime() - options.getMinActionTime()) * mod);
            time = Math.max(options.getMinActionTime(), Math.min(options.getMaxActionTime(), time));
            
            if (power > 0)
                time = 1;
            
            action.setTimeLeft(time);
            performer.sendActionControl(action.getActionString(), true, time);
            
            if (power == 0 || performer.isVisible())
                Server.getInstance().broadCastAction(performer.getName() + " starts using some clutter form the ground to mark a rune.", performer, 5);
        }
        else {
            if (counter * 10f > action.getTimeLeft()) {
                if (options.isLogMark())
                    logger.info(String.format("%s marked a rune (%d) at %d, %d (%d).",
                        performer.getName(), target.getWurmId(), performer.getTileX(), performer.getTileY(), performer.getLayer()));
                
                if (power == 0 && options.getCastSkill() > 0) {
                    Skill skill = performer.getSkills().getSkillOrLearn(options.getCastSkill());
                    double skillpower = skill.skillCheck(options.getCastDifficulty(), 0d, false, counter);
                    double requiredpower = 30d;
                    
                    try { performer.depleteFavor(options.getCastFavour() / 10f, false); }
                    catch (Exception ex) { logger.log(Level.SEVERE, String.format("%s casted mark, but could not deplete favor", performer.getName()), ex); }
                    
                    if (skillpower < requiredpower) {
                        performer.getCommunicator().sendNormalServerMessage("The mark spell fizzles.");
                        performer.playPersonalSound("sound.combat.hit.croc");
                        return true;
                    }
                }
                target.setDataXY(performer.getTileX(), performer.getTileY());
                target.setData2(performer.getLayer() << 16 | performer.getFloorLevel());
                performer.getCommunicator().sendNormalServerMessage("You finish marking the rune based on your surroundings.");
                
                if (power == 0 || performer.isVisible())
                    Server.getInstance().broadCastAction(performer.getName() + " finishes marking a rune.", performer, 5);
                
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
