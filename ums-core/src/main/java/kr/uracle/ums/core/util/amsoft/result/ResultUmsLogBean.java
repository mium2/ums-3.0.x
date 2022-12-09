package kr.uracle.ums.core.util.amsoft.result;

import java.io.Serializable;

public class ResultUmsLogBean implements Serializable {
    private String TRANS_TYPE = "";
    private String SEND_TYPE = "";
    private String SEND_TYPE_SEQCODE = "";
    private String PROVIDER = "";
    private String MOBILE_NUM = "";
    private String ERRCODE = "";
    private String RESULTMSG = "";

    public String getTRANS_TYPE() {
        return TRANS_TYPE;
    }

    public void setTRANS_TYPE(String TRANS_TYPE) {
        this.TRANS_TYPE = TRANS_TYPE;
    }

    public String getSEND_TYPE() {
        return SEND_TYPE;
    }

    public void setSEND_TYPE(String SEND_TYPE) {
        this.SEND_TYPE = SEND_TYPE;
    }

    public String getSEND_TYPE_SEQCODE() {
        return SEND_TYPE_SEQCODE;
    }

    public void setSEND_TYPE_SEQCODE(String SEND_TYPE_SEQCODE) {
        this.SEND_TYPE_SEQCODE = SEND_TYPE_SEQCODE;
    }

    public String getPROVIDER() {
        return PROVIDER;
    }

    public void setPROVIDER(String PROVIDER) {
        this.PROVIDER = PROVIDER;
    }

    public String getMOBILE_NUM() {
        return MOBILE_NUM;
    }

    public void setMOBILE_NUM(String MOBILE_NUM) {
        this.MOBILE_NUM = MOBILE_NUM;
    }

    public String getERRCODE() {
        return ERRCODE;
    }

    public void setERRCODE(String ERRCODE) {
        this.ERRCODE = ERRCODE;
    }

    public String getRESULTMSG() {
        return RESULTMSG;
    }

    public void setRESULTMSG(String RESULTMSG) {
        this.RESULTMSG = RESULTMSG;
    }
}
