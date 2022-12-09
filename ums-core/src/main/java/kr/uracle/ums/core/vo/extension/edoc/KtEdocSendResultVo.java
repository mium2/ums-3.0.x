package kr.uracle.ums.core.vo.extension.edoc;

import java.util.List;

public class KtEdocSendResultVo {
    
    // 필수 - 서비스 코드
    private String service_cd;
    
    // 필수 - 발송요청메시지구분 [0: 사전문자, 1: 본문자]
    private String req_msg_type_dvcd;
    private List<KtEdocSendResultDetailVo> reqs;

    public String getService_cd() {return service_cd;}
    public void setService_cd(String service_cd) {this.service_cd = service_cd;}
    public String getReq_msg_type_dvcd() {return req_msg_type_dvcd;}
    public void setReq_msg_type_dvcd(String req_msg_type_dvcd) {this.req_msg_type_dvcd = req_msg_type_dvcd;}
    public List<KtEdocSendResultDetailVo> getReqs() {return reqs;}
    public void setReqs(List<KtEdocSendResultDetailVo> reqs) {this.reqs = reqs;}

    @Override
    public String toString() {
        return "KtEdocSendResultVo{" +
                "service_cd='" + service_cd + '\'' +
                ", req_msg_type_dvcd='" + req_msg_type_dvcd + '\'' +
                ", reqs=" + reqs +
                '}';
    }
}
