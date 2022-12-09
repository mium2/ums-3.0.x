package kr.uracle.ums.core.util.amsoft.result;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultSqlThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private String ThreadName;
    private final ResultSqlMgr resultSqlMgr;
    private boolean isRun = true;

    public ResultSqlThread(String name, ResultSqlMgr _resultSqlMgr){
        super(name);
        this.ThreadName = getName();
        this.resultSqlMgr = _resultSqlMgr;
    }

    public void run(){
        while(isRun){
            try {
                // Step 1 : 처리일감 가져옴.
                ResultUmsLogBean resultUmsLogBean = resultSqlMgr.takeWork();
                if(resultUmsLogBean == null) {
                    continue;
                }
                execute(resultUmsLogBean);
                resultUmsLogBean = null;
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException 발생");
                break;
            }catch (Exception e){
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void execute(ResultUmsLogBean resultUmsLogBean) {
        try {
            // T_UMS_LOG테이블에 저장하는 로직 구현
            //amMsg.setTranType(prcsBean.getTRANS_TYPE()== TransType.REAL?"R":"B");
            TransType transType = TransType.REAL;
            if("B".equals(resultUmsLogBean.getTRANS_TYPE())){
                transType = TransType.BATCH;
            }
            SendType sendType = SendType.RCS_FREE;
            if("KKOALT".equals(resultUmsLogBean.getSEND_TYPE())){
                sendType = SendType.KKOALT;
            }else if("KKOFRT".equals(resultUmsLogBean.getSEND_TYPE())){
                sendType = SendType.KKOFRT;
            }else if("SMS".equals(resultUmsLogBean.getSEND_TYPE())){
                sendType = SendType.SMS;
            }else if("LMS".equals(resultUmsLogBean.getSEND_TYPE())){
                sendType = SendType.LMS;
            }else if("MMS".equals(resultUmsLogBean.getSEND_TYPE())){
                sendType = SendType.MMS;
            }

            UmsResultBaseBean umsResultBaseBean = new UmsResultBaseBean(transType,sendType);
            umsResultBaseBean.setSEND_TYPE_SEQCODE(resultUmsLogBean.getSEND_TYPE_SEQCODE());
            umsResultBaseBean.setPROVIDER(resultUmsLogBean.getPROVIDER());
            umsResultBaseBean.setERRCODE(resultUmsLogBean.getERRCODE());
            umsResultBaseBean.setRESULTMSG(resultUmsLogBean.getRESULTMSG());
            resultSqlMgr.getUmsDao().inUmsLog(umsResultBaseBean);

        }catch (Exception e){
            logger.error("T_UMS_LOG insert 중 에러 발생:"+e.getMessage());
        }
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }
}
