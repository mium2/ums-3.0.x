package kr.uracle.ums.core.vo.status;

/**
 * Created by Y.B.H(mium2) on 2019. 6. 10..
 */
public class SendMsgVo {
    private int TOT_CNT = 0;
    private long UMS_SEQNO = 0;
    private String APP_ID= "";
    private String PUSH_MSG= "";
    private String START_SEND_KIND= "";
    private String MSG_TYPE= "";

    private int PUSH_SEND_CNT= 0;
    private int PUSH_FAIL_CNT= 0;
    private int ALT_SEND_CNT= 0;
    private int ALT_FAIL_CNT= 0;
    private int FRT_SEND_CNT= 0;
    private int FRT_FAIL_CNT= 0;
    private int SMS_CNT= 0;
    private int SMS_FAIL_CNT= 0;
    private int FAIL_CNT= 0;
    private String SENDERID= "";
    private String PLUS_ID= "";
    private String TEMPLATECONTENTS= "";
    private String BUTTONS= "";
    private String SMS_MSG= "";
    private String ALLIMTOLK_TEMPLCODE= "";
    private String FRIENDTOLK_MSG= "";
    private String TITLE= "";
    private String REGDATE= "";
    private String KKOALT_SVCID = "";
    private String KKOFRT_SVCID = "";

    private int TOTAL_SEND_CNT = 0;
    private String REGDATE_YMD = "";
    private String REGDATE_HM = "";
    private String PUSH_SEND_CNT_TXT = "-";
    private String ALT_SEND_CNT_TXT = "-";
    private String FRT_SEND_CNT_TXT = "-";
    private String SMS_CNT_TXT = "-";
    private String SENDER = "";

    public int getTOT_CNT() {
        return TOT_CNT;
    }

    public void setTOT_CNT(int TOT_CNT) {
        this.TOT_CNT = TOT_CNT;
    }

    public long getUMS_SEQNO() {
        return UMS_SEQNO;
    }

    public void setUMS_SEQNO(long UMS_SEQNO) {
        this.UMS_SEQNO = UMS_SEQNO;
    }

    public String getAPP_ID() {
        return APP_ID;
    }

    public void setAPP_ID(String APP_ID) {
        this.APP_ID = APP_ID;
    }

    public String getPUSH_MSG() {
        return PUSH_MSG;
    }

    public void setPUSH_MSG(String PUSH_MSG) {
        this.PUSH_MSG = PUSH_MSG;
    }

    public String getSTART_SEND_KIND() {
        return START_SEND_KIND;
    }

    public void setSTART_SEND_KIND(String START_SEND_KIND) {
        this.START_SEND_KIND = START_SEND_KIND;
    }

    public String getMSG_TYPE() {
        return MSG_TYPE;
    }

    public void setMSG_TYPE(String MSG_TYPE) {
        this.MSG_TYPE = MSG_TYPE;
    }

    public int getPUSH_SEND_CNT() {
        return PUSH_SEND_CNT;
    }

    public void setPUSH_SEND_CNT(int PUSH_SEND_CNT) {
        this.PUSH_SEND_CNT = PUSH_SEND_CNT;
    }

    public int getPUSH_FAIL_CNT() {
        return PUSH_FAIL_CNT;
    }

    public void setPUSH_FAIL_CNT(int PUSH_FAIL_CNT) {
        this.PUSH_FAIL_CNT = PUSH_FAIL_CNT;
    }

    public int getALT_SEND_CNT() {
        return ALT_SEND_CNT;
    }

    public void setALT_SEND_CNT(int ALT_SEND_CNT) {
        this.ALT_SEND_CNT = ALT_SEND_CNT;
    }

    public int getALT_FAIL_CNT() {
        return ALT_FAIL_CNT;
    }

    public void setALT_FAIL_CNT(int ALT_FAIL_CNT) {
        this.ALT_FAIL_CNT = ALT_FAIL_CNT;
    }

    public int getFRT_SEND_CNT() {
        return FRT_SEND_CNT;
    }

    public void setFRT_SEND_CNT(int FRT_SEND_CNT) {
        this.FRT_SEND_CNT = FRT_SEND_CNT;
    }

    public int getFRT_FAIL_CNT() {
        return FRT_FAIL_CNT;
    }

    public void setFRT_FAIL_CNT(int FRT_FAIL_CNT) {
        this.FRT_FAIL_CNT = FRT_FAIL_CNT;
    }

    public int getSMS_CNT() {
        return SMS_CNT;
    }

    public void setSMS_CNT(int SMS_CNT) {
        this.SMS_CNT = SMS_CNT;
    }

    public int getSMS_FAIL_CNT() {
        return SMS_FAIL_CNT;
    }

    public void setSMS_FAIL_CNT(int SMS_FAIL_CNT) {
        this.SMS_FAIL_CNT = SMS_FAIL_CNT;
    }

    public int getFAIL_CNT() {
        return FAIL_CNT;
    }

    public void setFAIL_CNT(int FAIL_CNT) {
        this.FAIL_CNT = FAIL_CNT;
    }

    public String getSENDERID() {
        return SENDERID;
    }

    public void setSENDERID(String SENDERID) {
        this.SENDERID = SENDERID;
        this.SENDER = SENDERID;
    }

    public String getPLUS_ID() {
        return PLUS_ID;
    }

    public void setPLUS_ID(String PLUS_ID) {
        this.PLUS_ID = PLUS_ID;
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

    public String getSMS_MSG() {
        return SMS_MSG;
    }

    public void setSMS_MSG(String SMS_MSG) {
        this.SMS_MSG = SMS_MSG;
    }

    public String getALLIMTOLK_TEMPLCODE() {
        return ALLIMTOLK_TEMPLCODE;
    }

    public void setALLIMTOLK_TEMPLCODE(String ALLIMTOLK_TEMPLCODE) {
        this.ALLIMTOLK_TEMPLCODE = ALLIMTOLK_TEMPLCODE;
    }

    public String getFRIENDTOLK_MSG() {
        return FRIENDTOLK_MSG;
    }

    public void setFRIENDTOLK_MSG(String FRIENDTOLK_MSG) {
        this.FRIENDTOLK_MSG = FRIENDTOLK_MSG;
    }

    public String getTITLE() {
        return TITLE;
    }

    public void setTITLE(String TITLE) {
        this.TITLE = TITLE;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    public String getKKOALT_SVCID() {
        return KKOALT_SVCID;
    }

    public void setKKOALT_SVCID(String KKOALT_SVCID) {
        this.KKOALT_SVCID = KKOALT_SVCID;
    }

    public String getKKOFRT_SVCID() {
        return KKOFRT_SVCID;
    }

    public void setKKOFRT_SVCID(String KKOFRT_SVCID) {
        this.KKOFRT_SVCID = KKOFRT_SVCID;
    }

    public int getTOTAL_SEND_CNT() {
        return TOTAL_SEND_CNT;
    }

    public void setTOTAL_SEND_CNT(int TOTAL_SEND_CNT) {
        this.TOTAL_SEND_CNT = TOTAL_SEND_CNT;
    }

    public String getREGDATE_YMD() {
        return REGDATE_YMD;
    }

    public void setREGDATE_YMD(String REGDATE_YMD) {
        this.REGDATE_YMD = REGDATE_YMD;
    }

    public String getREGDATE_HM() {
        return REGDATE_HM;
    }

    public void setREGDATE_HM(String REGDATE_HM) {
        this.REGDATE_HM = REGDATE_HM;
    }

    public String getPUSH_SEND_CNT_TXT() {
        return PUSH_SEND_CNT_TXT;
    }

    public void setPUSH_SEND_CNT_TXT(String PUSH_SEND_CNT_TXT) {
        this.PUSH_SEND_CNT_TXT = PUSH_SEND_CNT_TXT;
    }

    public String getALT_SEND_CNT_TXT() {
        return ALT_SEND_CNT_TXT;
    }

    public void setALT_SEND_CNT_TXT(String ALT_SEND_CNT_TXT) {
        this.ALT_SEND_CNT_TXT = ALT_SEND_CNT_TXT;
    }

    public String getFRT_SEND_CNT_TXT() {
        return FRT_SEND_CNT_TXT;
    }

    public void setFRT_SEND_CNT_TXT(String FRT_SEND_CNT_TXT) {
        this.FRT_SEND_CNT_TXT = FRT_SEND_CNT_TXT;
    }

    public String getSMS_CNT_TXT() {
        return SMS_CNT_TXT;
    }

    public void setSMS_CNT_TXT(String SMS_CNT_TXT) {
        this.SMS_CNT_TXT = SMS_CNT_TXT;
    }

    public String getSENDER() {
        return SENDER;
    }

    public void setSENDER(String SENDER) {
        this.SENDER = SENDER;
    }
}
