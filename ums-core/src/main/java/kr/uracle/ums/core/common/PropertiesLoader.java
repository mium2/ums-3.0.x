package kr.uracle.ums.core.common;

import org.springframework.util.ResourceUtils;

import java.io.*;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: mium2(Yoo Byung Hee)
 * Date: 2014-06-24
 * Time: 오후 1:50
 * To change this template use File | Settings | File Templates.
 */
public class PropertiesLoader {
    private Properties cf = new Properties();
    private static final PropertiesLoader singleton = new PropertiesLoader();
    private PropertiesLoader(){
        initConfig();
    }
    public static PropertiesLoader getInstance(){
        return singleton;
    }
    public Properties getConfig(){
        return cf;
    }
    private void initConfig(){
        try {
            File ehcacheConfigFile = ResourceUtils.getFile("classpath:/config/receiver.properties");
            String configSrc = ehcacheConfigFile.getAbsolutePath();
            InputStream is = null;

            is = new FileInputStream(configSrc);
            try {
                cf.load(is);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
