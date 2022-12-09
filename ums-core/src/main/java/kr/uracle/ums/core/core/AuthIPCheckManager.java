package kr.uracle.ums.core.core;

import kr.uracle.ums.core.common.UmsInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 22..
 */
public class AuthIPCheckManager {
    private static final Logger logger = LoggerFactory.getLogger("kr.uracle.ums");
    private Map<String,String> ignoreTargets = new HashMap<String, String>();
    private static AuthIPCheckManager ourInstance = new AuthIPCheckManager();
    private boolean isSecurityUse = true;

    public static AuthIPCheckManager getInstance() {
        return ourInstance;
    }

    public void init(){
        // 인증키를 사용하지 않을 미리 세팅된 Legacy Server IP 세팅
        String ignorTargetIps = UmsInitListener.webProperties.getProperty("LEGACY.ALLOW.IP","");

        if("any".equals(ignorTargetIps.trim())){
            isSecurityUse = false;
            return;
        }

        List<String> ignoreTargetIpList = new ArrayList<String>();
        if("".equals(ignorTargetIps)){
            return;
        }
        if(ignorTargetIps.indexOf(",")>0){
            String[] ignorTargetIpsArr = ignorTargetIps.split(",");
            for(int i=0; i<ignorTargetIpsArr.length; i++){
                ignoreTargetIpList.add(ignorTargetIpsArr[i]);
            }
        }else{
            ignoreTargetIpList.add(ignorTargetIps);
        }

        ignoreTargetIpList.add("127.0.0.1");
        ignoreTargetIpList.add("0:0:0:0:0:0:0:1");
        ignoreTargetIpList.add("0:0:0:0:0:0:0:1%0");

        for(String ignoreTargetIp : ignoreTargetIpList){
            try{
                String org_setIP = ignoreTargetIp.trim();
                if(org_setIP.indexOf(".")>-1){
                    String[] settingIPClass = org_setIP.split("\\.");
                    if(settingIPClass.length==4){
                        String ABC_classIP = settingIPClass[0]+"."+settingIPClass[1]+"."+settingIPClass[2]+".";
                        String D_classIP = settingIPClass[3];
                        if(D_classIP.indexOf("*")>-1){
                            for(int j=0; j<=255; j++){
                                String setIP = ABC_classIP+j;
                                ignoreTargets.put(setIP,setIP);
                            }
                            continue;
                        }else if(D_classIP.indexOf("-")>-1){
                            String[] gapIP = D_classIP.split("\\-");
                            int startIP = Integer.parseInt(gapIP[0].trim());
                            int endIP = Integer.parseInt(gapIP[1].trim());
                            if(startIP>endIP){
                                for(int j=endIP; j<=startIP; j++){
                                    String setIP = ABC_classIP+j;
                                    ignoreTargets.put(setIP,setIP);
                                }
                            }else if(startIP==endIP){
                                String setIP = ABC_classIP+startIP;
                                ignoreTargets.put(setIP,setIP);
                            }else if(startIP<endIP){
                                for(int j=startIP; j<=endIP; j++){
                                    String setIP = ABC_classIP+j;
                                    ignoreTargets.put(setIP,setIP);
                                }
                            }
                            continue;
                        }
                    }
                }

                ignoreTargets.put(org_setIP,org_setIP);
            }catch(Exception e){
                logger.error("!![IP 인증 셋팅 에러] : {}",e.toString());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                System.exit(-1);
                break;
            }
        }

        logger.info("#################################################################");
        logger.info("#########         SETTING IP                           ##########");
        logger.info("#################################################################");

        Set<Map.Entry<String,String>> entrySet = ignoreTargets.entrySet();
        for(Map.Entry<String,String> mapEntry : entrySet){
            logger.info("# Registration IP :" + mapEntry.getKey());
        }
    }

    public boolean isAllowIp(String connectIP){
        logger.trace("## connect client IP : {}",connectIP);
        if(isSecurityUse) {
            return ignoreTargets.containsKey(connectIP);
        }else{
            // any로 모든 아이피 open.
            return true;
        }
    }
}
