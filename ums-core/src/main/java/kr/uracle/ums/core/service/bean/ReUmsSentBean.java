package kr.uracle.ums.core.service.bean;

/**
 * Created by Y.B.H(mium2) on 2021-04-01.
 */
public class ReUmsSentBean {
    private String UMS_SEQNO="";
    private String MSG_TYPE="";
    private String TITLE="";
    private String RESERVEDATE="";
    private String APP_ID="";
    private String ATTACHFILE="";
    private String PUSH_MSG = "";
    private String SOUNDFILE="";
    private String BADGENO="";
    private String PRIORITY="";
    private String EXT="";
    private String SENDERCODE="";
    private String SERVICECODE="";
    private String TARGET_USER_TYPE="";
    private String DB_IN="";
    private String PUSH_FAIL_SMS_SEND="";
    private String SPLIT_MSG_CNT="";
    private String DELAY_SECOND="";
    private String ALLIMTOLK_TEMPLCODE="";
    private String REPLACE_VARS="";
    private String KKOALT_SVCID="";
    private String FRIENDTOLK_MSG="";
    private String KKOFRT_SVCID="";
    private String SMS_MSG="";
    private String SENDERID="";
    private String SENDGROUPCODE="";
    private String PLUS_ID="";
    private String FRT_TEMPL_ID="";
    private String SMS_TEMPL_ID="";
    private String CALLBACK_NUM="";
    private long PUSH_SEND_CNT=0;
    private long PUSH_FAIL_CNT=0;
    private long ALT_SEND_CNT=0;
    private long ALT_FAIL_CNT=0;
    private long FRT_SEND_CNT=0;
    private long FRT_FAIL_CNT=0;
    private long SMS_CNT=0;
    private long SMS_FAIL_CNT=0;
    private long FAIL_CNT=0;
    private String START_SEND_KIND="";
    private String STATUS,REGDATE="";
    private String RESERVE_SEQNO="";
    private String KKO_BTNS="";
    private String KKO_IMG_PATH="";
    private String KKO_IMG_LINK_URL="";
    private String MMS_IMGURL="";
    private String SMS_TITLE="";
    private String PUSH_FAIL_WAIT_MIN="";

    public String getUMS_SEQNO() {
        return UMS_SEQNO;
    }

    public void setUMS_SEQNO(String UMS_SEQNO) {
        this.UMS_SEQNO = UMS_SEQNO;
    }

    public String getMSG_TYPE() {
        return MSG_TYPE;
    }

    public void setMSG_TYPE(String MSG_TYPE) {
        this.MSG_TYPE = MSG_TYPE;
    }

    public String getTITLE() {
        return TITLE;
    }

    public void setTITLE(String TITLE) {
        this.TITLE = TITLE;
    }

    public String getRESERVEDATE() {
        return RESERVEDATE;
    }

    public void setRESERVEDATE(String RESERVEDATE) {
        this.RESERVEDATE = RESERVEDATE;
    }

    public String getAPP_ID() {
        return APP_ID;
    }

    public void setAPP_ID(String APP_ID) {
        this.APP_ID = APP_ID;
    }

    public String getATTACHFILE() {
        return ATTACHFILE;
    }

    public void setATTACHFILE(String ATTACHFILE) {
        this.ATTACHFILE = ATTACHFILE;
    }

    public String getPUSH_MSG() {
        return PUSH_MSG;
    }

    public void setPUSH_MSG(String PUSH_MSG) {
        this.PUSH_MSG = PUSH_MSG;
    }

    public String getSOUNDFILE() {
        return SOUNDFILE;
    }

    public void setSOUNDFILE(String SOUNDFILE) {
        this.SOUNDFILE = SOUNDFILE;
    }

    public String getBADGENO() {
        return BADGENO;
    }

    public void setBADGENO(String BADGENO) {
        this.BADGENO = BADGENO;
    }

    public String getPRIORITY() {
        return PRIORITY;
    }

    public void setPRIORITY(String PRIORITY) {
        this.PRIORITY = PRIORITY;
    }

    public String getEXT() {
        return EXT;
    }

    public void setEXT(String EXT) {
        this.EXT = EXT;
    }

    public String getSENDERCODE() {
        return SENDERCODE;
    }

    public void setSENDERCODE(String SENDERCODE) {
        this.SENDERCODE = SENDERCODE;
    }

    public String getSERVICECODE() {
        return SERVICECODE;
    }

    public void setSERVICECODE(String SERVICECODE) {
        this.SERVICECODE = SERVICECODE;
    }

    public String getTARGET_USER_TYPE() {
        return TARGET_USER_TYPE;
    }

    public void setTARGET_USER_TYPE(String TARGET_USER_TYPE) {
        this.TARGET_USER_TYPE = TARGET_USER_TYPE;
    }

    public String getDB_IN() {
        return DB_IN;
    }

    public void setDB_IN(String DB_IN) {
        this.DB_IN = DB_IN;
    }

    public String getPUSH_FAIL_SMS_SEND() {
        return PUSH_FAIL_SMS_SEND;
    }

    public void setPUSH_FAIL_SMS_SEND(String PUSH_FAIL_SMS_SEND) {
        this.PUSH_FAIL_SMS_SEND = PUSH_FAIL_SMS_SEND;
    }

    public String getSPLIT_MSG_CNT() {
        return SPLIT_MSG_CNT;
    }

    public void setSPLIT_MSG_CNT(String SPLIT_MSG_CNT) {
        this.SPLIT_MSG_CNT = SPLIT_MSG_CNT;
    }

    public String getDELAY_SECOND() {
        return DELAY_SECOND;
    }

    public void setDELAY_SECOND(String DELAY_SECOND) {
        this.DELAY_SECOND = DELAY_SECOND;
    }

    public String getALLIMTOLK_TEMPLCODE() {
        return ALLIMTOLK_TEMPLCODE;
    }

    public void setALLIMTOLK_TEMPLCODE(String ALLIMTOLK_TEMPLCODE) {
        this.ALLIMTOLK_TEMPLCODE = ALLIMTOLK_TEMPLCODE;
    }

    public String getREPLACE_VARS() {
        return REPLACE_VARS;
    }

    public void setREPLACE_VARS(String REPLACE_VARS) {
        this.REPLACE_VARS = REPLACE_VARS;
    }

    public String getKKOALT_SVCID() {
        return KKOALT_SVCID;
    }

    public void setKKOALT_SVCID(String KKOALT_SVCID) {
        this.KKOALT_SVCID = KKOALT_SVCID;
    }

    public String getFRIENDTOLK_MSG() {
        return FRIENDTOLK_MSG;
    }

    public void setFRIENDTOLK_MSG(String FRIENDTOLK_MSG) {
        this.FRIENDTOLK_MSG = FRIENDTOLK_MSG;
    }

    public String getKKOFRT_SVCID() {
        return KKOFRT_SVCID;
    }

    public void setKKOFRT_SVCID(String KKOFRT_SVCID) {
        this.KKOFRT_SVCID = KKOFRT_SVCID;
    }

    public String getSMS_MSG() {
        return SMS_MSG;
    }

    public void setSMS_MSG(String SMS_MSG) {
        this.SMS_MSG = SMS_MSG;
    }

    public String getSENDERID() {
        return SENDERID;
    }

    public void setSENDERID(String SENDERID) {
        this.SENDERID = SENDERID;
    }

    public String getSENDGROUPCODE() {
        return SENDGROUPCODE;
    }

    public void setSENDGROUPCODE(String SENDGROUPCODE) {
        this.SENDGROUPCODE = SENDGROUPCODE;
    }

    public String getPLUS_ID() {
        return PLUS_ID;
    }

    public void setPLUS_ID(String PLUS_ID) {
        this.PLUS_ID = PLUS_ID;
    }

    public String getFRT_TEMPL_ID() {
        return FRT_TEMPL_ID;
    }

    public void setFRT_TEMPL_ID(String FRT_TEMPL_ID) {
        this.FRT_TEMPL_ID = FRT_TEMPL_ID;
    }

    public String getSMS_TEMPL_ID() {
        return SMS_TEMPL_ID;
    }

    public void setSMS_TEMPL_ID(String SMS_TEMPL_ID) {
        this.SMS_TEMPL_ID = SMS_TEMPL_ID;
    }

    public String getCALLBACK_NUM() {
        return CALLBACK_NUM;
    }

    public void setCALLBACK_NUM(String CALLBACK_NUM) {
        this.CALLBACK_NUM = CALLBACK_NUM;
    }

    public long getPUSH_SEND_CNT() {
        return PUSH_SEND_CNT;
    }

    public void setPUSH_SEND_CNT(long PUSH_SEND_CNT) {
        this.PUSH_SEND_CNT = PUSH_SEND_CNT;
    }

    public long getPUSH_FAIL_CNT() {
        return PUSH_FAIL_CNT;
    }

    public void setPUSH_FAIL_CNT(long PUSH_FAIL_CNT) {
        this.PUSH_FAIL_CNT = PUSH_FAIL_CNT;
    }

    public long getALT_SEND_CNT() {
        return ALT_SEND_CNT;
    }

    public void setALT_SEND_CNT(long ALT_SEND_CNT) {
        this.ALT_SEND_CNT = ALT_SEND_CNT;
    }

    public long getALT_FAIL_CNT() {
        return ALT_FAIL_CNT;
    }

    public void setALT_FAIL_CNT(long ALT_FAIL_CNT) {
        this.ALT_FAIL_CNT = ALT_FAIL_CNT;
    }

    public long getFRT_SEND_CNT() {
        return FRT_SEND_CNT;
    }

    public void setFRT_SEND_CNT(long FRT_SEND_CNT) {
        this.FRT_SEND_CNT = FRT_SEND_CNT;
    }

    public long getFRT_FAIL_CNT() {
        return FRT_FAIL_CNT;
    }

    public void setFRT_FAIL_CNT(long FRT_FAIL_CNT) {
        this.FRT_FAIL_CNT = FRT_FAIL_CNT;
    }

    public long getSMS_CNT() {
        return SMS_CNT;
    }

    public void setSMS_CNT(long SMS_CNT) {
        this.SMS_CNT = SMS_CNT;
    }

    public long getSMS_FAIL_CNT() {
        return SMS_FAIL_CNT;
    }

    public void setSMS_FAIL_CNT(long SMS_FAIL_CNT) {
        this.SMS_FAIL_CNT = SMS_FAIL_CNT;
    }

    public long getFAIL_CNT() {
        return FAIL_CNT;
    }

    public void setFAIL_CNT(long FAIL_CNT) {
        this.FAIL_CNT = FAIL_CNT;
    }

    public String getSTART_SEND_KIND() {
        return START_SEND_KIND;
    }

    public void setSTART_SEND_KIND(String START_SEND_KIND) {
        this.START_SEND_KIND = START_SEND_KIND;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    public String getREGDATE() {
        return REGDATE;
    }

    public void setREGDATE(String REGDATE) {
        this.REGDATE = REGDATE;
    }

    public String getRESERVE_SEQNO() {
        return RESERVE_SEQNO;
    }

    public void setRESERVE_SEQNO(String RESERVE_SEQNO) {
        this.RESERVE_SEQNO = RESERVE_SEQNO;
    }

    public String getKKO_BTNS() {
        return KKO_BTNS;
    }

    public void setKKO_BTNS(String KKO_BTNS) {
        this.KKO_BTNS = KKO_BTNS;
    }

    public String getKKO_IMG_PATH() {
        return KKO_IMG_PATH;
    }

    public void setKKO_IMG_PATH(String KKO_IMG_PATH) {
        this.KKO_IMG_PATH = KKO_IMG_PATH;
    }

    public String getKKO_IMG_LINK_URL() {
        return KKO_IMG_LINK_URL;
    }

    public void setKKO_IMG_LINK_URL(String KKO_IMG_LINK_URL) {
        this.KKO_IMG_LINK_URL = KKO_IMG_LINK_URL;
    }

    public String getMMS_IMGURL() {
        return MMS_IMGURL;
    }

    public void setMMS_IMGURL(String MMS_IMGURL) {
        this.MMS_IMGURL = MMS_IMGURL;
    }

    public String getSMS_TITLE() {
        return SMS_TITLE;
    }

    public void setSMS_TITLE(String SMS_TITLE) {
        this.SMS_TITLE = SMS_TITLE;
    }

    public String getPUSH_FAIL_WAIT_MIN() {
        return PUSH_FAIL_WAIT_MIN;
    }

    public void setPUSH_FAIL_WAIT_MIN(String PUSH_FAIL_WAIT_MIN) {
        this.PUSH_FAIL_WAIT_MIN = PUSH_FAIL_WAIT_MIN;
    }
}
