package kr.uracle.ums.core.vo.extension.edoc;

import java.util.List;
import java.util.Map;

public class KtEdocSendResultResponseVo {
    private String result_cd = "00";
    private String result_dt;
    private List<Map<String, String>> errors;
    
    public String getResult_cd() {return result_cd;}
    public void setResult_cd(String result_cd) {this.result_cd = result_cd;}
    public String getResult_dt() {return result_dt;}
    public void setResult_dt(String result_dt) {this.result_dt = result_dt;}
    public List<Map<String, String>> getErrors() {return errors;}
    public void setErrors(List<Map<String, String>> errors) {this.errors = errors;}
}
