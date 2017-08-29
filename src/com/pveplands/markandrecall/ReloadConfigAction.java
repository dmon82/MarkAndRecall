package com.pveplands.markandrecall;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import javax.annotation.Nonnull;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class ReloadConfigAction implements ActionPerformer, ModAction {
    private short actionId;
    private ActionEntry actionEntry;
    private Options options;
    
    public ReloadConfigAction(Options ops) {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, "Reload config", "reloading configuration", new int[] { });
        ModActions.registerAction(actionEntry);
        options = ops;
    }
        
    @Override
    public short getActionId() {
        return actionId;
    }
    
    public ActionEntry getActionEntry() {
        return actionEntry;
    }
    
    @Override
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item target, short num, float counter) {
        return performMyAction(performer);
    }
    
    @Override
    public boolean action(@Nonnull Action action, @Nonnull Creature performer, @Nonnull Item source, @Nonnull Item target, short num, float counter) {
        return performMyAction(performer);
    }
    
    private boolean performMyAction(Creature performer) {
        if (performer.getPower() < options.getReloadConfigPower()) {
            performer.getCommunicator().sendAlertServerMessage("You do not have the required account power to do this.");
            return true;
        }
        
        performer.getCommunicator().sendAlertServerMessage("Reloading configuration file.");
        options.reload();
        
        return true;
    }
}
