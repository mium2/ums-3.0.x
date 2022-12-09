package kr.uracle.ums.core.vo.monitor;

public class MonitorServerVo {
    private String SERVERID= "";
    private String GROUPID= "UMS";
    private String SERVERTYPE= "UMS";
    private String SERVERNAME= "";
    private String MONITOR_URL= "";
    private String ISACTIVE= "Y";

    public String getSERVERID() {
        return SERVERID;
    }

    public void setSERVERID(String SERVERID) {
        this.SERVERID = SERVERID;
    }

    public String getGROUPID() {
        return GROUPID;
    }

    public void setGROUPID(String GROUPID) {
        this.GROUPID = GROUPID;
    }

    public String getSERVERTYPE() {
        return SERVERTYPE;
    }

    public void setSERVERTYPE(String SERVERTYPE) {
        this.SERVERTYPE = SERVERTYPE;
    }

    public String getSERVERNAME() {
        return SERVERNAME;
    }

    public void setSERVERNAME(String SERVERNAME) {
        this.SERVERNAME = SERVERNAME;
    }

    public String getMONITOR_URL() {
        return MONITOR_URL;
    }

    public void setMONITOR_URL(String MONITOR_URL) {
        this.MONITOR_URL = MONITOR_URL;
    }

    public String getISACTIVE() {
        return ISACTIVE;
    }

    public void setISACTIVE(String ISACTIVE) {
        this.ISACTIVE = ISACTIVE;
    }
}
