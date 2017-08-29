package com.wurmonline.server.questions;

import com.pveplands.markandrecall.Options;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigQuestion extends Question {
    private Options options;
    
    public ConfigQuestion(Options ops, Creature performer, String title, String question, int type, long target) {
        super(performer, title, question, type, target);
    
        options = ops;
    }
    @Override
    public void answer(Properties properties) {
        Logger logger = Logger.getLogger("test");
        
        Iterator iter = properties.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next().toString();
            String value = properties.getProperty(key);
            
            logger.info(String.format("%s: %s", key, value));
        }
        
        logger.info("Saving new properties to file.");
        options.save(properties);
    }

    @Override
    public void sendQuestion() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getBmlHeader());
        
        for (Method method : options.getClass().getMethods()) {
            if (method.getName().startsWith("set")) {
                Type type = method.getParameters()[0].getType();
                String name = method.getName().substring(3);
                String value = "";
                
                try {
                    if (type.equals(boolean.class)) value = options.getClass().getMethod("is" + name).invoke(options).toString();
                    else value = options.getClass().getMethod("get" + name).invoke(options).toString();
                }
                catch (Exception ex) { logger.log(Level.WARNING, "Could not get value for " + name, ex); }
                
                logger.info(String.format("Adding element for %s with value %s.", name, value));
                
                if (type.equals(String.class)) {
                    sb.append("harray{label{text=\"").append(name).append("\"};input{id=\"").append(name).append("\";maxchars=\"200\";text=\"").append(value).append("\"};}");
                }
                else if (type.equals(boolean.class)) {
                    sb.append("harray{label{text=\"").append(name).append("\"};radio{group=\"").append(name).append("\";id=\"true\"");
                    if (value.equals("true"))
                        sb.append(";selected=\"true\"");
                    sb.append("};label{text=\"true\"};");
                    sb.append("radio{group=\"").append(name).append("\";id=\"false\"");
                    if (value.equals("false"))
                        sb.append(";selected=\"true\"");
                    sb.append("};label{text=\"false\"};}");
                }
                else {
                    sb.append("harray{label{text=\"").append(name).append("\"};input{id=\"").append(name).append("\";maxchars=\"10\";text=\"").append(value).append("\"};}");
                }
            }
        }
        
        sb.append(this.createOkAnswerButton());
        
        this.getResponder().getCommunicator().sendBml(300, 500, true, true, sb.toString(), 255, 255, 255, "MarkAndRecall options");
    }
}
