package kr.uracle.ums.core.processor.edoc;

public class EdocProcessBean {
    private String PROVIDER = "";
    
    private String QUERY_TYPE = "INSERT";
    
    private String REQUEST_TRANID = "";
    private String DOCID = "";
    private String CI = "";
    private String DOCHASH = "";
    private String MSGTYPE = "";

    private String SENDMSG = "";

    private String RESULTCODE = "0000";
    private String RESULTMSG = "SUCCESS";

    public String getPROVIDER() {
        return PROVIDER;
    }

    public void setPROVIDER(String PROVIDER) {
        this.PROVIDER = PROVIDER;
    }

    public String getQUERY_TYPE() {return QUERY_TYPE;}
    public void setQUERY_TYPE(String QUERY_TYPE) {this.QUERY_TYPE = QUERY_TYPE;}
    
    public String getREQUEST_TRANID() {
        return REQUEST_TRANID;
    }

    public void setREQUEST_TRANID(String REQUEST_TRANID) {
        this.REQUEST_TRANID = REQUEST_TRANID;
    }

    public String getDOCID() {
        return DOCID;
    }

    public void setDOCID(String DOCID) {
        this.DOCID = DOCID;
    }

    public String getCI() {
        return CI;
    }

    public void setCI(String CI) {
        this.CI = CI;
    }

    public String getDOCHASH() {
        return DOCHASH;
    }

    public void setDOCHASH(String DOCHASH) {
        this.DOCHASH = DOCHASH;
    }

    public String getMSGTYPE() {
        return MSGTYPE;
    }

    public void setMSGTYPE(String MSGTYPE) {
        this.MSGTYPE = MSGTYPE;
    }

    public String getSENDMSG() {
        return SENDMSG;
    }

    public void setSENDMSG(String SENDMSG) {
        this.SENDMSG = SENDMSG;
    }

    public String getRESULTCODE() {
        return RESULTCODE;
    }

    public void setRESULTCODE(String RESULTCODE) {
        this.RESULTCODE = RESULTCODE;
    }

    public String getRESULTMSG() {
        return RESULTMSG;
    }

    public void setRESULTMSG(String RESULTMSG) {
        this.RESULTMSG = RESULTMSG;
    }
}
