package kr.uracle.ums.core.vo.extension.edoc;

public class KtEdocSendResultDetailVo {
    // 필수 - 관리키
    private String src_key;
    
    // 필수 - MMS 발송 결과 상태 순번 [1: 수신시(결과), 2: 열람시(결과)]
    private int mms_sndg_rslt_sqno;
    
    // 필수 - 처리일자, 통신사 플랫폼에서 처리한 일자
    private String prcs_dt;
    
    // 필수 - 문서코드, 메시지 발송 기관/법인의 메시지 식별 코드
    private String mms_bsns_dvcd;
    
    // 필수 - 모바일사업자구분, 발송 통신사(플랫폼) 구분 코드 값 [01:KT, 02:SKT, 03:LG U+]
    private String mbl_bzowr_dvcd;
    
    // 옵션 - 실제발송번호(일부), 고객휴대전화번호 뒤 4자리 값
    private String rl_mms_sndg_telno;
    
    // 필수 - 발송결과코드
    private String mms_sndg_rslt_dvcd;
    
    // 필수 - 발송타임스탬프, 발송일시의 타임스탬프 (YYYYMMDDHH24MISS)
    private String mms_sndg_tmst;
    
    // 상황적 필수 - 수신타임스탬프, 수신일시의 타임스탬프 (YYYYMMDDHH24MISS)
    private String mms_rcv_tmst;
    
    // 상황적 필수 - 열람타임스탬프, 열람일시의 타임스탬프 (YYYYMMDDHH24MISS)
    private String mms_rdg_tmst;
    
    // 옵션 - 기동의발송여부, 메시지 수신 시 수신자가 기존에 동의를 했는지에 대한 여부 값 [Y, N]
    private String prev_approve_yn;
    
    // 필수 - 발송 메시지 타입, [1:RCS, 2:xMS]
    private String msg_type;
    
    // 옵션 - 클릭일시, 수신동의상태전송(BG-AG-SN-015) API 사용을 신청한 기관의 발송 메시지 별 열람 클릭 일시(최초 클릭일시)
    private String click_dt;
    
    // 옵션 - 동의일시, 수신동의상태전송(BG-AG-SN-015) API 사용을 신청한 기관의 최초 동의 일시
    private String approve_dt;

    public String getSrc_key() {return src_key;}
    public void setSrc_key(String src_key) {this.src_key = src_key;}
    public int getMms_sndg_rslt_sqno() {return mms_sndg_rslt_sqno;}
    public void setMms_sndg_rslt_sqno(int mms_sndg_rslt_sqno) {this.mms_sndg_rslt_sqno = mms_sndg_rslt_sqno;}
    public String getPrcs_dt() {return prcs_dt;}
    public void setPrcs_dt(String prcs_dt) {this.prcs_dt = prcs_dt;}
    public String getMms_bsns_dvcd() {return mms_bsns_dvcd;}
    public void setMms_bsns_dvcd(String mms_bsns_dvcd) {this.mms_bsns_dvcd = mms_bsns_dvcd;}
    public String getMbl_bzowr_dvcd() {return mbl_bzowr_dvcd;}
    public void setMbl_bzowr_dvcd(String mbl_bzowr_dvcd) {this.mbl_bzowr_dvcd = mbl_bzowr_dvcd;}
    public String getRl_mms_sndg_telno() {return rl_mms_sndg_telno;}
    public void setRl_mms_sndg_telno(String rl_mms_sndg_telno) {this.rl_mms_sndg_telno = rl_mms_sndg_telno;}
    public String getMms_sndg_rslt_dvcd() {return mms_sndg_rslt_dvcd;}
    public void setMms_sndg_rslt_dvcd(String mms_sndg_rslt_dvcd) {this.mms_sndg_rslt_dvcd = mms_sndg_rslt_dvcd;}
    public String getMms_sndg_tmst() {return mms_sndg_tmst;}
    public void setMms_sndg_tmst(String mms_sndg_tmst) {this.mms_sndg_tmst = mms_sndg_tmst;}
    public String getMms_rcv_tmst() {return mms_rcv_tmst;}
    public void setMms_rcv_tmst(String mms_rcv_tmst) {this.mms_rcv_tmst = mms_rcv_tmst;}
    public String getMms_rdg_tmst() {return mms_rdg_tmst;}
    public void setMms_rdg_tmst(String mms_rdg_tmst) {this.mms_rdg_tmst = mms_rdg_tmst;}
    public String getPrev_approve_yn() {return prev_approve_yn;}
    public void setPrev_approve_yn(String prev_approve_yn) {this.prev_approve_yn = prev_approve_yn;}
    public String getMsg_type() {return msg_type;}
    public void setMsg_type(String msg_type) {this.msg_type = msg_type;}
    public String getClick_dt() {return click_dt;}
    public void setClick_dt(String click_dt) {this.click_dt = click_dt;}
    public String getApprove_dt() {return approve_dt;}
    public void setApprove_dt(String approve_dt) {this.approve_dt = approve_dt;}

    @Override
    public String toString() {
        return "KtEdocSendResultDetailVo{" +
                "src_key='" + src_key + '\'' +
                ", mms_sndg_rslt_sqno=" + mms_sndg_rslt_sqno +
                ", prcs_dt='" + prcs_dt + '\'' +
                ", mms_bsns_dvcd='" + mms_bsns_dvcd + '\'' +
                ", mbl_bzowr_dvcd='" + mbl_bzowr_dvcd + '\'' +
                ", rl_mms_sndg_telno='" + rl_mms_sndg_telno + '\'' +
                ", mms_sndg_rslt_dvcd='" + mms_sndg_rslt_dvcd + '\'' +
                ", mms_sndg_tmst='" + mms_sndg_tmst + '\'' +
                ", mms_rcv_tmst='" + mms_rcv_tmst + '\'' +
                ", mms_rdg_tmst='" + mms_rdg_tmst + '\'' +
                ", prev_approve_yn='" + prev_approve_yn + '\'' +
                ", msg_type='" + msg_type + '\'' +
                ", click_dt='" + click_dt + '\'' +
                ", approve_dt='" + approve_dt + '\'' +
                '}';
    }
}
