package kr.uracle.ums.core.processor.bean;

import kr.uracle.ums.codec.redis.enums.TransType;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 3..
 */
public class StatisticsBean implements Serializable {
    public TransType TRANS_TYPE;
    private String ROOT_CHANNEL_YN = "Y"; // 발송요청 첫번째 채널일 경우:Y, 대체발송 채널일 경우:N
    public String SENDERSYSTEM = "";
    public String SENDDATE = "";
    private String SENDERID = "";
    private String SENDERGROUP = "";
    private String SENDTYPE = "";
    private String DETAIL_SENDTYPE = "";
    private String OPTIONAL = "";
    private int RS_CNT = 0; // 발송요청카운트
    private int SS_CNT = 0; // 발송성공카운트
    private int FS_CNT = 0; // 대체발송카운트
    private int FF_CNT = 0; // 실패카운트

    public TransType getTRANS_TYPE() {
        return TRANS_TYPE;
    }

    public void setTRANS_TYPE(TransType TRANS_TYPE) {
        this.TRANS_TYPE = TRANS_TYPE;
        this.SENDERSYSTEM = TRANS_TYPE.toString();
    }

    public String getROOT_CHANNEL_YN() {
        return ROOT_CHANNEL_YN;
    }

    public void setROOT_CHANNEL_YN(String ROOT_CHANNEL_YN) {
        this.ROOT_CHANNEL_YN = ROOT_CHANNEL_YN;
    }

    public String getSENDERSYSTEM() {
        return SENDERSYSTEM;
    }

    public String getSENDDATE() {
        return SENDDATE;
    }

    public void setSENDDATE(String SENDDATE) {
        this.SENDDATE = SENDDATE;
    }

    public String getSENDERID() {
        return SENDERID;
    }

    public void setSENDERID(String SENDERID) {
        this.SENDERID = SENDERID;
    }

    public String getSENDERGROUP() {
        return SENDERGROUP;
    }

    public void setSENDERGROUP(String SENDERGROUP) {
        this.SENDERGROUP = SENDERGROUP;
    }

    public String getSENDTYPE() {
        return SENDTYPE;
    }

    public void setSENDTYPE(String SENDTYPE) {
        this.SENDTYPE = SENDTYPE;
    }

    public String getDETAIL_SENDTYPE() {return DETAIL_SENDTYPE;}
    public void setDETAIL_SENDTYPE(String DETAIL_SENDTYPE) {this.DETAIL_SENDTYPE = DETAIL_SENDTYPE;}
    
    public String getOPTIONAL() {
        return OPTIONAL;
    }

    public void setOPTIONAL(String OPTIONAL) {
        this.OPTIONAL = OPTIONAL;
    }

    public int getRS_CNT() {
        return RS_CNT;
    }

    public void setRS_CNT(int RS_CNT) {
        this.RS_CNT = RS_CNT;
    }

    public int getSS_CNT() {
        return SS_CNT;
    }

    public void setSS_CNT(int SS_CNT) {
        this.SS_CNT = SS_CNT;
    }

    public int getFS_CNT() {
        return FS_CNT;
    }

    public void setFS_CNT(int FS_CNT) {
        this.FS_CNT = FS_CNT;
    }

    public int getFF_CNT() {
        return FF_CNT;
    }

    public void setFF_CNT(int FF_CNT) {
        this.FF_CNT = FF_CNT;
    }
}
