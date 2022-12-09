package kr.uracle.ums.core.service.bean;

public class MacroChannelCheckBean {
    private String SEND_MACRO_CODE = "";
    private String PUSH_MSG = "";
    private String WPUSH_MSG = "";
    private String ALLIMTALK_MSG = "";
    private String FRIENDTOLK_MSG = "";
    private String RCS_TYPE = "";
    private String RCS_TEMPL_ID = "";
    private String RCS_MSGBASE_ID = "";
    private int RCS_IMG_CNT = 0;
    private int RCS_BTN_CNT = 0;
    private String SMS_TITLE = "";
    private String SMS_MSG = "";
    private String MMS_IMGURL = "";
    private String NAVER_MSG = "";

    private String VAR9 ="";
    
    public String getSEND_MACRO_CODE() { return SEND_MACRO_CODE; }
    public void setSEND_MACRO_CODE(String SEND_MACRO_CODE) { this.SEND_MACRO_CODE = SEND_MACRO_CODE; }

    public String getPUSH_MSG() { return PUSH_MSG; }
    public void setPUSH_MSG(String PUSH_MSG) { this.PUSH_MSG = PUSH_MSG; }

    public String getWPUSH_MSG() {
        return WPUSH_MSG;
    }

    public void setWPUSH_MSG(String WPUSH_MSG) {
        this.WPUSH_MSG = WPUSH_MSG;
    }

    public String getALLIMTALK_MSG() {
        return ALLIMTALK_MSG;
    }

    public void setALLIMTALK_MSG(String ALLIMTALK_MSG) {
        this.ALLIMTALK_MSG = ALLIMTALK_MSG;
    }

    public String getFRIENDTOLK_MSG() { return FRIENDTOLK_MSG; }
    public void setFRIENDTOLK_MSG(String FRIENDTOLK_MSG) { this.FRIENDTOLK_MSG = FRIENDTOLK_MSG; }

    public String getRCS_TYPE() { return RCS_TYPE; }
	public void setRCS_TYPE(String rCS_TYPE) { RCS_TYPE = rCS_TYPE;	}
	
	public String getRCS_TEMPL_ID() { return RCS_TEMPL_ID; }
    public void setRCS_TEMPL_ID(String RCS_TEMPL_ID) { this.RCS_TEMPL_ID = RCS_TEMPL_ID; }
    
    public String getRCS_MSGBASE_ID() { return RCS_MSGBASE_ID; }
    public void setRCS_MSGBASE_ID(String RCS_MSGBASE_ID) { this.RCS_MSGBASE_ID = RCS_MSGBASE_ID; }
    public int getRCS_IMG_CNT() { return RCS_IMG_CNT; }
    public void setRCS_IMG_CNT(int RCS_IMG_CNT) { this.RCS_IMG_CNT = RCS_IMG_CNT; }

    public int getRCS_BTN_CNT() { return RCS_BTN_CNT; }
    public void setRCS_BTN_CNT(int RCS_BTN_CNT) { this.RCS_BTN_CNT = RCS_BTN_CNT; }

    public String getSMS_TITLE() { return SMS_TITLE; }
    public void setSMS_TITLE(String SMS_TITLE) { this.SMS_TITLE = SMS_TITLE; }
    
    public String getSMS_MSG() { return SMS_MSG; }
    public void setSMS_MSG(String SMS_MSG) { this.SMS_MSG = SMS_MSG; }

    public String getMMS_IMGURL() { return MMS_IMGURL; }
    public void setMMS_IMGURL(String MMS_IMGURL) { this.MMS_IMGURL = MMS_IMGURL; }

    public String getNAVER_MSG() {
        return NAVER_MSG;
    }

    public void setNAVER_MSG(String NAVER_MSG) {
        this.NAVER_MSG = NAVER_MSG;
    }

    public String getVAR9() {return VAR9;}
    public void setVAR9(String VAR9) {this.VAR9 = VAR9;}
}
