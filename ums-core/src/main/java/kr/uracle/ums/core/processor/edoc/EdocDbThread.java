package kr.uracle.ums.core.processor.edoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EdocDbThread extends Thread{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private String ThreadName;
    private final EdocDbMgr edocDbMgr;
    private boolean isRun = true;

    public EdocDbThread(String name, EdocDbMgr _edocDbMgr){
        super(name);
        this.ThreadName = getName();
        this.edocDbMgr = _edocDbMgr;
    }

    public void run(){
        while(isRun){
            try {
                // Step 1 : 처리일감 가져옴.
                EdocProcessBean edocProcessBean = edocDbMgr.takeWork();
                if(edocProcessBean == null) {
                    continue;
                }
                execute(edocProcessBean);
                edocProcessBean = null;
            }catch(InterruptedException ex){
                logger.info("### ["+ThreadName+"] InterruptedException 의한 종료");
//                break;
            }catch (Exception e){
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
        logger.info("EdocThread 종료됨");
    }

    private void execute(EdocProcessBean edocProcessBean) {
        // 마켓팅 이벤티페이지에서 들어온 정보 업데이트 로직 처리.
        try {
            // EDOC 처리결과 정보 DB 처리
            if(edocProcessBean.getQUERY_TYPE().equals("INSERT")){
                edocDbMgr.getSqlSessionTemplate().insert("mybatis.edoc.inEdocLog", edocProcessBean);
            }else{
                edocDbMgr.getSqlSessionTemplate().update("mybatis.edoc.upEdocLog", edocProcessBean);
            }
        }catch (Exception e){
            logger.error("전자문서 결과정보 DB 등록 중 에러 발생:"+e.getMessage());
        }
    }

    public boolean isRun() { return isRun; }
    public void setRun(boolean isRun) { this.isRun = isRun; }
}
