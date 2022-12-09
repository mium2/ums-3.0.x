package kr.uracle.ums.core.vo.template;

import java.io.Serializable;

public class AltTemplateLotteVo extends AltTemplateBaseVo implements Serializable {
    private String SENDER_KEY = "";
    private String TEM_STAT_CODE = "";
    private String TEMPLATE_MESSAGE_TYPE = "";

    public String getSENDER_KEY() {
        return SENDER_KEY;
    }

    public void setSENDER_KEY(String SENDER_KEY) {
        this.SENDER_KEY = SENDER_KEY;
    }

    public String getTEM_STAT_CODE() {
        return TEM_STAT_CODE;
    }

    public void setTEM_STAT_CODE(String TEM_STAT_CODE) {
        this.TEM_STAT_CODE = TEM_STAT_CODE;
    }

    public String getTEMPLATE_MESSAGE_TYPE() {
        return TEMPLATE_MESSAGE_TYPE;
    }

    public void setTEMPLATE_MESSAGE_TYPE(String TEMPLATE_MESSAGE_TYPE) {
        this.TEMPLATE_MESSAGE_TYPE = TEMPLATE_MESSAGE_TYPE;
    }
    
}
