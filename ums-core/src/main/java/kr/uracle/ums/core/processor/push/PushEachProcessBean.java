package kr.uracle.ums.core.processor.push;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 29..
 */
public class PushEachProcessBean extends PushBasicProcessBean implements Serializable{
	
	private static final long serialVersionUID = 1L;

    public PushEachProcessBean() {
        super.setPROVIDER(this.getClass().getSimpleName());
    }
    private String SPLIT_MSG_CNT = "";
    private String DELAY_SECOND = "";
    private String ALT_REPLACE_VARS = "";

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

    public String getALT_REPLACE_VARS() {
        return ALT_REPLACE_VARS;
    }

    public void setALT_REPLACE_VARS(String ALT_REPLACE_VARS) {
        this.ALT_REPLACE_VARS = ALT_REPLACE_VARS;
    }
}