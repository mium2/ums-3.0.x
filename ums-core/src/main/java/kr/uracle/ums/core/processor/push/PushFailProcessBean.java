package kr.uracle.ums.core.processor.push;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 21..
 */
public class PushFailProcessBean extends PushBasicProcessBean implements Serializable{
	
	private static final long serialVersionUID = 1L;
    public PushFailProcessBean() {
        super.setPROVIDER(this.getClass().getSimpleName());
    }
    private String ERRCODE = "0000";
    private String RESULTMSG = "SUCCESS";
    private boolean PROCESS_END = false;

    public String getERRCODE() {
        return ERRCODE;
    }

    public void setERRCODE(String ERRCODE) {
        this.ERRCODE = ERRCODE;
    }

    public String getRESULTMSG() {
        return RESULTMSG;
    }

    public void setRESULTMSG(String RESULTMSG) {
        this.RESULTMSG = RESULTMSG;
    }

    public boolean isPROCESS_END() {
        return PROCESS_END;
    }

    public void setPROCESS_END(boolean PROCESS_END) {
        this.PROCESS_END = PROCESS_END;
    }
}
