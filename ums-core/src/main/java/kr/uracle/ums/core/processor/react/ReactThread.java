package kr.uracle.ums.core.processor.react;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private String ThreadName;
    private final ReactMgr reactMgr;
    private boolean isRun = true;

    public ReactThread(String name, ReactMgr _reactMgr){
        super(name);
        this.ThreadName = getName();
        this.reactMgr = _reactMgr;
    }

    public void run(){
        while(isRun){
            try {
                // Step 1 : 처리일감 가져옴.
                ReactProcessBean reactProcessBean = reactMgr.takeWork();
                if(reactProcessBean == null) {
                    continue;
                }
                execute(reactProcessBean);
                reactProcessBean = null;
            }catch(InterruptedException ex){
                logger.info("######## ["+ThreadName+"] InterruptedException 의한 종료");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
        logger.info("ReactThread 종료됨");
    }

    private void execute(ReactProcessBean reactProcessBean) {
        // 마켓팅 이벤티페이지에서 들어온 정보 업데이트 로직 처리.
        try {
            int applyRow = reactMgr.getUmsDao().upUmsReactCountBatch(reactProcessBean);
            if(applyRow==0){
                logger.warn("반응률 업데이트 적용카운트 0. CUST_TRANSKEY: "+reactProcessBean.getCUST_TRANSKEY());
            }
        }catch (Exception e){
            logger.error("반응률 업데이트 중 에러 발생:"+e.getMessage());
        }
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }
}
