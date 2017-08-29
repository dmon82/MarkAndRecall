package com.pveplands.markandrecall;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import javax.annotation.Nonnull;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;
import com.wurmonline.server.questions.ConfigQuestion;

public class ConfigInterfaceAction implements ActionPerformer, ModAction {
    private short actionId;
    private ActionEntry actionEntry;
    private Options options;
    
    public ConfigInterfaceAction(Options options) {
        actionId = (short)ModActions.getNextActionId();
        actionEntry = ActionEntry.createEntry(actionId, "Configure", "configuring", new int[0]);
        ModActions.registerAction(actionEntry);
        this.options = options;
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
        if (performer.getPower() < options.getConfigInterfacePower()) {
            performer.getCommunicator().sendAlertServerMessage("You do not have the required account power.");
            return true;
        }
        
        new ConfigQuestion(options, performer, "Mark and Recall options", "Edit options", 135, -10).sendQuestion();
        return true;
    }
    
    @Override
    public short getActionId() {
        return actionId;
    }
    
    public ActionEntry getActionEntry() {
        return actionEntry;
    }
}
