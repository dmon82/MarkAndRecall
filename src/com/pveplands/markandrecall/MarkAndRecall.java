package com.pveplands.markandrecall;

import com.wurmonline.server.MiscConstants;
import com.wurmonline.server.items.CreationCategories;
import com.wurmonline.server.items.CreationEntryCreator;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplateCreator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.ItemTemplatesCreatedListener;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

public class MarkAndRecall implements WurmServerMod, ServerStartedListener, PreInitable, ItemTemplatesCreatedListener, Configurable { 
    private final static Logger logger = Logger.getLogger(getLoggerName(MarkAndRecall.class));
    
    private static MarkAction markAction;
    private static RecallAction recallAction;
    private static ReloadConfigAction reloadAction;
    private static ConfigInterfaceAction interfaceAction;
    
    private static Options options;
    
    public MarkAndRecall() {
        
    }

    @Override
    public void preInit() {
        ModActions.init();
        
        try {
            HookManager.getInstance().getClassPool().get("com.wurmonline.server.items.Item")
                .getMethod("sendEnchantmentStrings", "(Lcom/wurmonline/server/creatures/Communicator;)V")
                .insertAfter("{ if (getTemplateId() == " + options.getRuneTemplateId() + ") { if (getData1() == -1) comm.sendNormalServerMessage(\"This rune is blank.\"); else comm.sendNormalServerMessage(\"The rune has some markings on it.\"); } }");
            
            
            
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Could not inject code for recall rune examination.", e);
        }
        
        ModifyQuestionClass();
    }
    
    @Override
    public void configure(Properties properties) {
        logger.info("Loading properties...");
        options = new Options(properties, MarkAndRecall.class);
    }
    
    private int getProperty(Properties properties, String key, int defaultValue) {
        return getProperty(properties, key, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    private int getProperty(Properties properties, String key, int defaultValue, int min, int max) {
        return Math.min(max, Math.max(min, Integer.valueOf(properties.getProperty(key, String.valueOf(defaultValue)))));
    }

    private float getProperty(Properties properties, String key, float defaultValue) {
        return getProperty(properties, key, defaultValue, Float.MIN_VALUE, Float.MAX_VALUE);
    }
    
    private float getProperty(Properties properties, String key, float defaultValue, float min, float max) {
        return Math.min(max, Math.max(min, Float.valueOf(properties.getProperty(key, String.valueOf(defaultValue)))));
    }
    
    private boolean getProperty(Properties properties, String key, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    @Override
    public void onServerStarted() {
        ModActions.registerAction(markAction = new MarkAction(options));
        logger.info(String.format("Mark rune action added as ID: %d", markAction.getActionId()));
        
        ModActions.registerAction(recallAction = new RecallAction(options));
        logger.info(String.format("Recall rune action added as ID: %d", recallAction.getActionId()));
        
        ModActions.registerAction(reloadAction = new ReloadConfigAction(options));
        logger.info(String.format("Reload config action added as ID: %d", reloadAction.getActionId()));
        
        ModActions.registerAction(interfaceAction = new ConfigInterfaceAction(options));
        logger.info(String.format("Config interface action added as ID: %d", interfaceAction.getActionId()));
        
        ModActions.registerAction(new RuneBehaviour(options));
        logger.info("Registering rune behaviour provider.");
        
        CreationEntryCreator.createSimpleEntry(options.getCraftSkill(), ItemList.stoneChisel, ItemList.marbleBrick, MarkAndRecall.getRuneTemplateId(),
            false, true, 0f, false, false, CreationCategories.MAGIC);
        logger.info("Crafting entry created for recall rune.");
    }

    @Override
    public void onItemTemplatesCreated() {
        try {
            ItemTemplateCreator.createItemTemplate(options.getRuneTemplateId(), "recall rune", "recall runes", "magically superior", "magically strong", "magically weak", "faintly magical",
                "A magic rune with a malleable surface to scratch markings into.", new short[] { 6, 25, 44, 48, 108, 119, 157 }, (short)610, (short)1, 0, Long.MAX_VALUE, 5, 5, 10, -10, 
                MiscConstants.EMPTY_BYTE_PRIMITIVE_ARRAY, "model.resource.stone.rift.", options.getCraftDifficulty(), 10, (byte)0, 200, false);
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Item templates could not be created.", ex);
        }
    }
    
    private void ModifyQuestionClass() {
        try {
            CtClass ctClass = HookManager.getInstance().getClassPool().get("com.wurmonline.server.questions.Question");
            
            for (CtConstructor ctor : ctClass.getConstructors())
                ctor.setModifiers((ctor.getModifiers() & ~(java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.PROTECTED)) | java.lang.reflect.Modifier.PUBLIC);
            
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getName().startsWith("getBml") || method.getName().startsWith("create"))
                    method.setModifiers((method.getModifiers() & ~(java.lang.reflect.Modifier.PRIVATE | java.lang.reflect.Modifier.PROTECTED)) | java.lang.reflect.Modifier.PUBLIC);
            }
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not modify com.wurmonline.server.questions.Question.", ex);
        }
    }
    
    public static int getRuneTemplateId() {
        return options.getRuneTemplateId();
    }
    
    public static MarkAction getMarkAction() {
        return markAction;
    }
    
    public static RecallAction getRecallAction() {
        return recallAction;
    }
    
    public static ReloadConfigAction getReloadAction() {
        return reloadAction;
    }

    public static ConfigInterfaceAction getInterfaceAction() {
        return interfaceAction;
    }
    
    public static String getLoggerName(Class c) {
        return String.format("%s (v%s)", c.getName(), c.getPackage().getImplementationVersion());
    }
}
