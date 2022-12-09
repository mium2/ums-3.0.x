package kr.uracle.ums.core.vo.template;

import java.io.Serializable;

public class AltTemplateLgcnsVo extends AltTemplateBaseVo implements Serializable {
    private String TEMPL_ADD_CONTENT="";
    private String VARS="";
    private String APPROVAL="";
    private String PLUS_ID="";
    private String C_TYPE="";
    private String IMAGE="";


    public String getTEMPL_ADD_CONTENT() {
        return TEMPL_ADD_CONTENT;
    }

    public void setTEMPL_ADD_CONTENT(String TEMPL_ADD_CONTENT) {
        this.TEMPL_ADD_CONTENT = TEMPL_ADD_CONTENT;
    }

    public String getVARS() {
        return VARS;
    }

    public void setVARS(String VARS) {
        this.VARS = VARS;
    }

    public String getAPPROVAL() {
        return APPROVAL;
    }

    public void setAPPROVAL(String APPROVAL) {
        this.APPROVAL = APPROVAL;
    }

    public String getPLUS_ID() {
        return PLUS_ID;
    }

    public void setPLUS_ID(String PLUS_ID) {
        this.PLUS_ID = PLUS_ID;
    }

    public String getC_TYPE() {
        return C_TYPE;
    }

    public void setC_TYPE(String c_TYPE) {
        C_TYPE = c_TYPE;
    }

    public String getIMAGE() {
        return IMAGE;
    }

    public void setIMAGE(String IMAGE) {
        this.IMAGE = IMAGE;
    }
}