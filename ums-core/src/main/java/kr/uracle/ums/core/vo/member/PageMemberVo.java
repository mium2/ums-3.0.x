package kr.uracle.ums.core.vo.member;


/**
 * Created by Y.B.H(mium2) on 2019. 2. 28..
 */
public class PageMemberVo extends MemberVo {

    private long RNUM = 0;
    private long TOT_CNT = 0;

    public long getRNUM() {
        return RNUM;
    }

    public void setRNUM(long RNUM) {
        this.RNUM = RNUM;
    }

    public long getTOT_CNT() {
        return TOT_CNT;
    }

    public void setTOT_CNT(long TOT_CNT) {
        this.TOT_CNT = TOT_CNT;
    }

    @Override
    public String toString() {
        return "PageMemberVo{" +
            "MEMBERID='" + super.getMEMBERID() + '\'' +
            ", MEMBERNAME='" + super.getMEMBERNAME() + '\'' +
            ", MOBILE='" + super.getMOBILE() + '\'' +
            ", ORGANID='" + super.getORGANID() + '\'' +
            ", VAR1='" + super.getVAR1() + '\'' +
            ", VAR2='" + super.getVAR2() + '\'' +
            ", VAR3='" + super.getVAR3() + '\'' +
            ", VAR4='" + super.getVAR4() + '\'' +
            ", VAR5='" + super.getVAR5() + '\'' +
            ", RNUM=" + RNUM +
            ", TOT_CNT=" + TOT_CNT +
            '}';
    }
}
