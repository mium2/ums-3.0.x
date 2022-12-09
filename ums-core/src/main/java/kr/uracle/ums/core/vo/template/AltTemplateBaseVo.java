package kr.uracle.ums.core.vo.template;

import java.io.Serializable;

public class AltTemplateBaseVo implements Serializable {

    private String SENDERKEYTYPE ="S";
    private String KKOBIZCODE="";
    private String TEMPLATECODE=""; //카카오에 등록된 템플릿코드
    private String TEMPLATECONTENTS="";
    private String BUTTONS="";

    private String TITLE="";
    private String SUBTITLE="";
    
    private String IMAGE;

    public String getSENDERKEYTYPE() {
        return SENDERKEYTYPE;
    }

    public void setSENDERKEYTYPE(String SENDERKEYTYPE) {
        this.SENDERKEYTYPE = SENDERKEYTYPE;
    }

    public String getKKOBIZCODE() {
        return KKOBIZCODE;
    }

    public void setKKOBIZCODE(String KKOBIZCODE) {
        this.KKOBIZCODE = KKOBIZCODE;
    }

    public String getTEMPLATECODE() {
        return TEMPLATECODE;
    }

    public void setTEMPLATECODE(String TEMPLATECODE) {
        this.TEMPLATECODE = TEMPLATECODE;
    }

    public String getTITLE() {
        return TITLE;
    }

    public void setTITLE(String TITLE) {
        this.TITLE = TITLE;
    }

    public String getSUBTITLE() {
        return SUBTITLE;
    }

    public void setSUBTITLE(String SUBTITLE) {
        this.SUBTITLE = SUBTITLE;
    }

    public String getTEMPLATECONTENTS() {
        return TEMPLATECONTENTS;
    }

    public void setTEMPLATECONTENTS(String TEMPLATECONTENTS) {
        this.TEMPLATECONTENTS = TEMPLATECONTENTS;
    }

    public String getBUTTONS() {
        return BUTTONS;
    }
    public void setBUTTONS(String BUTTONS) {
        this.BUTTONS = BUTTONS;
    }

    public String getIMAGE() {return IMAGE;}
    public void setIMAGE(String IMAGE) {this.IMAGE = IMAGE;}
}
