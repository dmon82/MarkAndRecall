package com.pveplands.markandrecall;

import com.wurmonline.server.skills.SkillList;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for option loading and storing/boxing.
 */
public class Options {
    private static final Logger logger = Logger.getLogger(MarkAndRecall.getLoggerName(Options.class));

    private int runeTemplateId = 4205;
    private boolean undergroundRecall = false;
    private boolean undergroundMark = false;
    private boolean waterRecall = true;
    private boolean waterMark = false;
    private boolean lavaRecall = true;
    private boolean lavaMark = false;
    private boolean holyRecall = false;
    private boolean holyMark = false;
    private boolean villageRecall = false;
    private boolean villageMark = false;
    private boolean steepRecall = false;
    private boolean steepMark = true;
    private int maxSlope = 24;
    private float minUsableQuality = 10f;
    private boolean loseQuality = true;
    private boolean takeDamage = false;
    private boolean qualityDistance = true;
    private int minActionTime = 40;
    private int maxActionTime = 100;
    private int minRecallDistance = 0;
    private int maxRecallDistance = 0;
    private boolean actionTimeDistance = false;
    private int tilesPerSecond = 100;
    private boolean allowRemark = false;
    private boolean lightningEffect = true;
    private boolean playSound = true;
    private String sound = "sound.combat.hit.zombie";
    private boolean allowLeading = true;
    private boolean logMark = false;
    private boolean logRecall = false;
    private int craftSkill = SkillList.CHANNELING;
    private float craftDifficulty = 40f;
    private int castSkill = 0; //SkillList.CHANNELING;
    private float castFavour = 30f;
    private double castDifficulty = 40f;
    private int reloadConfigPower = 5;
    private int configInterfacePower = 6;
    
    private String modName;

    public Options(Properties properties, Class modClass) {
        modName = modClass.getSimpleName();
        load(properties);
    }
    
    public void load(Properties properties) {
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                String stringValue = properties.getProperty(field.getName(), field.get(this).toString());
                Type type = field.getType();
                
                if (type.equals(int.class))
                    field.setInt(this, Integer.valueOf(stringValue));
                else if (type.equals(boolean.class))
                    field.setBoolean(this, Boolean.valueOf(stringValue));
                else if (type.equals(float.class))
                    field.setFloat(this, Float.valueOf(stringValue));
                else if (type.equals(double.class))
                    field.setDouble(this, Double.valueOf(stringValue));
                else if (type.equals(short.class))
                    field.setShort(this, Short.valueOf(stringValue));
                else if (type.equals(long.class))
                    field.setLong(this, Long.valueOf(stringValue));
                else if (type.equals(byte.class))
                    field.setByte(this, Byte.valueOf(stringValue));
                else if (type.equals(String.class))
                    field.set(this, stringValue);
                
                logger.info(String.format("%s: %s", field.getName(), field.get(this).toString()));
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, String.format("Could not load option for field %s, default value will be used.", field.getName()), e);
            }
        }
        
        runeTemplateId = limit(runeTemplateId, 4200, 32767);
        maxSlope = limit(maxSlope, 0, 32767);
        minUsableQuality = limit(minUsableQuality, 0f, 100f);
        minActionTime = limit(minActionTime, 10, Integer.MAX_VALUE);
        maxActionTime = limit(maxActionTime, 10, Integer.MAX_VALUE);
        minRecallDistance = limit(minRecallDistance, 0, Integer.MAX_VALUE);
        maxRecallDistance = limit(maxRecallDistance, 0, Integer.MAX_VALUE);
        tilesPerSecond = limit(tilesPerSecond, 1, Integer.MAX_VALUE);
        castFavour = limit(castFavour, 0f, 300f);
        castDifficulty = limit(castDifficulty, 1f, 300f);
        craftDifficulty = limit(craftDifficulty, 1f, 300f);
    }
    
    public void reload() {
        Path file = Paths.get("mods", modName + ".properties");
        
        if (!Files.exists(file)) {
            logger.warning(String.format("Reloading config failed, no file %s for %s.", file.toString(), modName));
            return;
        }
        
        try {
            Properties properties = new Properties();
            InputStream stream = Files.newInputStream(file);
            
            try {
                logger.info(String.format("Reloading configuration for %s.", modName));
                properties.load(stream);
                load(properties);
            }
            finally {
                if (stream != null)
                    stream.close();
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Could not reload properties.", e);
        }
    }
    
    public void save(Properties properties) {
        load(properties);
        
        try {
            properties.store(Files.newOutputStream(Paths.get("mods", modName + ".properties")), "No comments");
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not update properties file.", ex);
        }
    }
    
    private int limit(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
    
    private float limit(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }
    
    private double limit(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
    
    private short limit(short value, short min, short max) {
        return (short)Math.min(max, Math.max(min, value));
    }
    
    private long limit(long value, long min, long max) {
        return Math.min(max, Math.max(min, value));
    }
    
    private short getProperty(Properties properties, String key, short defaultValue) {
        return (short)getProperty(properties, key, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
    
    private short getProperty(Properties properties, String key, short defaultValue, short min, short max) {
        return (short)Math.min(max, Math.max(min, Short.valueOf(properties.getProperty(key, String.valueOf(defaultValue)))));
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
    
    private double getProperty(Properties properties, String key, double defaultValue) {
        return getProperty(properties, key, defaultValue, Float.MIN_VALUE, Float.MAX_VALUE);
    }
    
    private double getProperty(Properties properties, String key, double defaultValue, double min, double max) {
        return Math.min(max, Math.max(min, Double.valueOf(properties.getProperty(key, String.valueOf(defaultValue)))));
    }
    
    private boolean getProperty(Properties properties, String key, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public int getRuneTemplateId() {
        return runeTemplateId;
    }
    
    public boolean isUndergroundRecall() {
        return undergroundRecall;
    }

    public boolean isUndergroundMark() {
        return undergroundMark;
    }

    public boolean isWaterRecall() {
        return waterRecall;
    }

    public boolean isWaterMark() {
        return waterMark;
    }

    public boolean isLavaRecall() {
        return lavaRecall;
    }

    public boolean isLavaMark() {
        return lavaMark;
    }

    public boolean isHolyRecall() {
        return holyRecall;
    }

    public boolean isHolyMark() {
        return holyMark;
    }

    public boolean isVillageRecall() {
        return villageRecall;
    }

    public boolean isVillageMark() {
        return villageMark;
    }

    public boolean isSteepRecall() {
        return steepRecall;
    }

    public boolean isSteepMark() {
        return steepMark;
    }

    public int getMaxSlope() {
        return maxSlope;
    }

    public float getMinUsableQuality() {
        return minUsableQuality;
    }

    public boolean isLoseQuality() {
        return loseQuality;
    }

    public boolean isTakeDamage() {
        return takeDamage;
    }

    public boolean isQualityDistance() {
        return qualityDistance;
    }

    public int getMinActionTime() {
        return minActionTime;
    }

    public int getMaxActionTime() {
        return maxActionTime;
    }

    public int getMinRecallDistance() {
        return minRecallDistance;
    }

    public int getMaxRecallDistance() {
        return maxRecallDistance;
    }

    public boolean isActionTimeDistance() {
        return actionTimeDistance;
    }

    public int getTilesPerSecond() {
        return tilesPerSecond;
    }
    
    public boolean isAllowRemark() {
        return allowRemark;
    }
    
    public boolean isLightningEffect() {
        return lightningEffect;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public String getSound() {
        return sound;
    }

    public boolean isAllowLeading() {
        return allowLeading;
    }

    public boolean isLogMark() {
        return logMark;
    }

    public boolean isLogRecall() {
        return logRecall;
    }

    public int getCraftSkill() {
        return craftSkill;
    }

    public int getCastSkill() {
        return castSkill;
    }

    public float getCastFavour() {
        return castFavour;
    }

    public float getCraftDifficulty() {
        return craftDifficulty;
    }

    public double getCastDifficulty() {
        return castDifficulty;
    }

    public void setUndergroundRecall(boolean undergroundRecall) {
        this.undergroundRecall = undergroundRecall;
    }

    public void setUndergroundMark(boolean undergroundMark) {
        this.undergroundMark = undergroundMark;
    }

    public void setWaterRecall(boolean waterRecall) {
        this.waterRecall = waterRecall;
    }

    public void setWaterMark(boolean waterMark) {
        this.waterMark = waterMark;
    }

    public void setLavaRecall(boolean lavaRecall) {
        this.lavaRecall = lavaRecall;
    }

    public void setLavaMark(boolean lavaMark) {
        this.lavaMark = lavaMark;
    }

    public void setHolyRecall(boolean holyRecall) {
        this.holyRecall = holyRecall;
    }

    public void setHolyMark(boolean holyMark) {
        this.holyMark = holyMark;
    }

    public void setVillageRecall(boolean villageRecall) {
        this.villageRecall = villageRecall;
    }

    public void setVillageMark(boolean villageMark) {
        this.villageMark = villageMark;
    }

    public void setSteepRecall(boolean steepRecall) {
        this.steepRecall = steepRecall;
    }

    public void setSteepMark(boolean steepMark) {
        this.steepMark = steepMark;
    }

    public void setMaxSlope(int maxSlope) {
        this.maxSlope = maxSlope;
    }

    public void setMinUsableQuality(float minUsableQuality) {
        this.minUsableQuality = minUsableQuality;
    }

    public void setLoseQuality(boolean loseQuality) {
        this.loseQuality = loseQuality;
    }

    public void setTakeDamage(boolean takeDamage) {
        this.takeDamage = takeDamage;
    }

    public void setQualityDistance(boolean qualityDistance) {
        this.qualityDistance = qualityDistance;
    }

    public void setMinActionTime(int minActionTime) {
        this.minActionTime = minActionTime;
    }

    public void setMaxActionTime(int maxActionTime) {
        this.maxActionTime = maxActionTime;
    }

    public void setMinRecallDistance(int minRecallDistance) {
        this.minRecallDistance = minRecallDistance;
    }

    public void setMaxRecallDistance(int maxRecallDistance) {
        this.maxRecallDistance = maxRecallDistance;
    }

    public void setActionTimeDistance(boolean actionTimeDistance) {
        this.actionTimeDistance = actionTimeDistance;
    }

    public void setTilesPerSecond(int tilesPerSecond) {
        this.tilesPerSecond = tilesPerSecond;
    }

    public void setAllowRemark(boolean allowRemark) {
        this.allowRemark = allowRemark;
    }

    public void setLightningEffect(boolean lightningEffect) {
        this.lightningEffect = lightningEffect;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public void setAllowLeading(boolean allowLeading) {
        this.allowLeading = allowLeading;
    }

    public void setLogMark(boolean logMark) {
        this.logMark = logMark;
    }

    public void setLogRecall(boolean logRecall) {
        this.logRecall = logRecall;
    }

    public void setCraftSkill(int craftSkill) {
        this.craftSkill = craftSkill;
    }

    public void setCraftDifficulty(float craftDifficulty) {
        this.craftDifficulty = craftDifficulty;
    }

    public void setCastSkill(int castSkill) {
        this.castSkill = castSkill;
    }

    public void setCastFavour(float castFavour) {
        this.castFavour = castFavour;
    }

    public void setCastDifficulty(double castDifficulty) {
        this.castDifficulty = castDifficulty;
    }

    public int getReloadConfigPower() {
        return reloadConfigPower;
    }

    public void setReloadConfigPower(int reloadConfigPower) {
        this.reloadConfigPower = reloadConfigPower;
    }

    public int getConfigInterfacePower() {
        return configInterfacePower;
    }

    public void setConfigInterfacePower(int configInterfacePower) {
        this.configInterfacePower = configInterfacePower;
    }
}
