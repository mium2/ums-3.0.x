package kr.uracle.ums.core.service.bean;

import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2020. 1. 13..
 */
public class PreventUserBean {

    private long USERSEQNO         = 0;
    private String USERID          = "";
    private String USERNAME        = "";
    private String MOBILE          = "";
    private String APPID           = "";
    private String REJECTPUSH      = "";
    private String REJECTCHANNEL   = "";
    private String REG_DT          = "";
    private String MOD_DT          = "";
    private String REG_ID          = "";
    private int TOT_CNT            = 0;

    private Map<String,String> REJECTCHANNEL_MAP;

    public long getUSERSEQNO() {
        return USERSEQNO;
    }

    public void setUSERSEQNO(long USERSEQNO) {
        this.USERSEQNO = USERSEQNO;
    }

    public String getUSERID() {
        return USERID;
    }

    public void setUSERID(String USERID) {
        this.USERID = USERID;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public void setUSERNAME(String USERNAME) {
        this.USERNAME = USERNAME;
    }

    public String getMOBILE() {
        return MOBILE;
    }

    public void setMOBILE(String MOBILE) {
        this.MOBILE = MOBILE;
    }

    public String getAPPID() {
        return APPID;
    }

    public void setAPPID(String APPID) {
        this.APPID = APPID;
    }

    public String getREJECTPUSH() {
        return REJECTPUSH;
    }

    public void setREJECTPUSH(String REJECTPUSH) {
        this.REJECTPUSH = REJECTPUSH;
    }

    public String getREJECTCHANNEL() {
        return REJECTCHANNEL;
    }

    public void setREJECTCHANNEL(String REJECTCHANNEL) {
        this.REJECTCHANNEL = REJECTCHANNEL;
    }

    public String getREG_DT() {
        return REG_DT;
    }

    public void setREG_DT(String REG_DT) {
        this.REG_DT = REG_DT;
    }

    public String getMOD_DT() {
        return MOD_DT;
    }

    public void setMOD_DT(String MOD_DT) {
        this.MOD_DT = MOD_DT;
    }

    public String getREG_ID() {
        return REG_ID;
    }

    public void setREG_ID(String REG_ID) {
        this.REG_ID = REG_ID;
    }

    public Map<String, String> getREJECTCHANNEL_MAP() {
        return REJECTCHANNEL_MAP;
    }

    public int getTOT_CNT() {
        return TOT_CNT;
    }

    public void setTOT_CNT(int TOT_CNT) {
        this.TOT_CNT = TOT_CNT;
    }

    public void setREJECTCHANNEL_MAP(Map<String, String> REJECTCHANNEL_MAP) {
        this.REJECTCHANNEL_MAP = REJECTCHANNEL_MAP;
    }
}
