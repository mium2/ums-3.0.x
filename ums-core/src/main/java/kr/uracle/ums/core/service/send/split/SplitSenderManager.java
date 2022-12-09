package kr.uracle.ums.core.service.send.split;

import kr.uracle.ums.core.common.UmsInitListener;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Y.B.H(mium2)
 */
public class SplitSenderManager {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static SplitSenderManager instance = null;
    //Key : SEQNO+FileName , Value : 보낼날자
    private Map<String,Long> chkDelayFileInfoMap = new ConcurrentHashMap<String, Long>();
    private SplitSenderThread splitSenderThread = null;

    private SplitSendLogUtil splitSendLogUtil = new SplitSendLogUtil();

    private String mLogDir;

    // 분할발송시 발송취소가 들어온 메세지 원장 번호 관리하는 Set
    private Map<String,Long> chkCancelSplitSendMap = new HashMap<String, Long>();

    private SplitSenderManager(){
    }

    public static SplitSenderManager getInstance() {
        if(instance==null){
            instance = new SplitSenderManager();
        }
        return instance;
    }

    /**
     * 서버가 재 구동시 혹시 파일로 저장 하고 있는 분할 메세지 정보를 로드 한다
     */
    private void init(){
        String shutDownNotSendMsgSave = UmsInitListener.webProperties.getProperty("SHUTDOWN_SPLIT_MSG_SAVE_YN","Y");
        // 버그 패치
        mLogDir = UmsInitListener.webProperties.getProperty("UMS.TEMPDIR", "/home/uracle/split/");
        mLogDir = mLogDir+File.separator+SplitSendLogUtil.SPLIT_FOLDER;
        logger.debug("####### mLogDir:" + mLogDir);

        if(!shutDownNotSendMsgSave.equals("Y")) {
            // 셧다운 후 보내지 않은 메세지 발송하지 않겠다고 설정한 경우. 분할 발송 파일 다 지운 후 분할 발송파일을 로드하지 않는다.
            removeAllSplitFoler();
            return;
        }

        FileReader fr = null;
        BufferedReader br = null;

        File dir = new File(mLogDir);
        File[] fileList = dir.listFiles();
        if(fileList==null){
            return;
        }
        for(int i=0; i<fileList.length; i++){
            File file = fileList[i];
            if(file.isDirectory()){
                String seqNo = file.getName();
                File dirInFile = new File(mLogDir+File.separator+seqNo);
                File[] msgFiles = dirInFile.listFiles();
                for(int j=0; j<msgFiles.length; j++) {
                    File splitFile = msgFiles[j];
                    logger.debug("##### splitFile:" + splitFile.getName());
                    if (splitFile.isFile() && !splitFile.getName().startsWith(".")) {
                        String fileName = splitFile.getName();
                        try {
                            fr = new FileReader(splitFile.getAbsolutePath());
                            br = new BufferedReader(fr);
                            String str = null;
                            if ((str = br.readLine()) != null) {
                                String sendMilTime = str;
                                String mapKey = seqNo+"|"+fileName;

                                logger.info("##### mapkey:" + mapKey + "  sendMilTime:" + sendMilTime);
                                chkDelayFileInfoMap.put(mapKey,Long.parseLong(sendMilTime));
                            }
                        } catch (Exception e) {
                            logger.error("[INFO] INDEX NO LINE: " + e.getMessage());
                        } finally {
                            if (br != null) try {
                                br.close();
                            } catch (IOException e) {
                            }
                            if (fr != null) try {
                                fr.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        }
        logger.debug("####### chkDelayFileInfoMap size:" + chkDelayFileInfoMap.size());
    }

    public void startWork(){
        init();
        splitSenderThread = new SplitSenderThread("splitWorkThread",this);
        splitSenderThread.start();
    }
    /**
     * Delay 발송 파일 리스트 구함
     * @return
     */
    public synchronized List<String> getSplitSendFile(){
        List<String> delayFileList = new ArrayList<String>();
        Set<Map.Entry<String, Long>> mapSet = chkDelayFileInfoMap.entrySet();
        for(Map.Entry<String,Long> mapEntry : mapSet){
            if(System.currentTimeMillis() >= mapEntry.getValue()){
                delayFileList.add(mapEntry.getKey());
            }
        }

        return delayFileList;
    }

    public synchronized void removeChkDelayFileInfo(String seqNo, String fileName){
        String mapDelKey = seqNo+"|"+fileName;
        chkDelayFileInfoMap.remove(mapDelKey);
    }
    public synchronized int getChkDelayFileInfoMapSize(){
        return chkDelayFileInfoMap.size();
    }

    public synchronized void putDelayFileInfo(String seqno, String fileName, Long sendMilTime){
        String mapKey = seqno+"|"+fileName;
        chkDelayFileInfoMap.put(mapKey, sendMilTime);
        logger.info("### 분할 발송:"+seqno+  " 발송예정시간:"+sendMilTime);
    }

    public void destory(){
        try {
            if (splitSenderThread != null) {
                splitSenderThread.setIsRun(false);
                splitSenderThread.interrupt();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 분할 지연발송 취소요청이 들어왔을 때 처리
     * @param seqno
     */
    public void cancelSplitSend(String seqno){
        //해당 메세지원장의 분할지연발송이 큐에 더 이상 등록되지 못하게 취소감시맵에 등록
        synchronized (this) {
            chkCancelSplitSendMap.put(seqno, System.currentTimeMillis());
        }
        logger.debug("######[PUT After] chkCancleSplitSendMap size: "+chkCancelSplitSendMap.size());
        //분할발송 체크리스트맵에서 삭제처리
        List<String> removeKeys = new ArrayList<String>();
        Set<Map.Entry<String, Long>> mapSet = chkDelayFileInfoMap.entrySet();
        for(Map.Entry<String,Long> mapEntry : mapSet){
            String mapKey = mapEntry.getKey();
            if(mapKey.startsWith(seqno+"|")){
                removeKeys.add(mapKey);
            }
        }
        synchronized (this) {
            for (String mapkey : removeKeys) {
                logger.debug("##### chkDelayFileInfoMap removeKey : "+mapkey);
                chkDelayFileInfoMap.remove(mapkey);
            }
        }

        //발송하지 못한 분할 발송파일 삭제처리
        SplitSendLogUtil splitSendLogUtil = new SplitSendLogUtil();
        removeLogFolder(seqno);

        //이미 BaseSend.java splitSend메소드에서 분할지연발송 파일을 다 만든 후에 분할지연발송 취소가 들어온 경우는
        //chkCancelSplitSendSet에 추가만 되고 삭제가 되지 않는다. 따라서 현재는 지속적으로 메모리가 증가되는걸 원치 않기 때문에 2시간이상으로 체크하여 삭제처리.

        List<String> removeChkCanCelKeys = new ArrayList<String>();
        Set<Map.Entry<String,Long>> chkCancelMapEntrySet = chkCancelSplitSendMap.entrySet();
        for(Map.Entry<String,Long> chkCancelMapEntry : chkCancelMapEntrySet){
            // 현재는 분할지연발송 취소요청이 들어온 SEQNO를 2시간지난 요청은 삭제처리. 이유는 적어도 분할지연발송파일을 만드는데 2시간이상은 걸리지 않을 것 으로 판단
            long effectiveMilTime = 2*60*60*1000;
//            long effectiveMilTime = 1*60*1000;  //테스트를 위해 1분으로 설정
            Long chkRemoveMilTime = chkCancelMapEntry.getValue()+effectiveMilTime;
            if(System.currentTimeMillis()>chkRemoveMilTime){
                removeChkCanCelKeys.add(chkCancelMapEntry.getKey());
            }
        }
        for(String removeSeqNo : removeChkCanCelKeys){
            chkCancelSplitSendMap.remove(removeSeqNo);
        }
        logger.debug("######[CLEAN After] chkCancleSplitSendMap size: "+chkCancelSplitSendMap.size());
    }

    /**
     * 분할 발송 후 취소요청이 들어왔는지 여부 리턴
     * @param seqno
     * @return
     */
    public synchronized boolean isCancelSplitMsg(String seqno){
        logger.debug("##### isCancelSplitMsg ");
        return chkCancelSplitSendMap.containsKey(seqno);

    }

    /**
     * 분할 발송 취소요청 받음
     * @param seqno
     */
    public synchronized void addCancelSplitMsg(String seqno){
        logger.debug("##### addCancelSplitMsg ");
        chkCancelSplitSendMap.put(seqno, System.currentTimeMillis());
    }

    /**
     * 분할 발송 취소요청 처리후 삭제
     * @param seqno
     */
    public synchronized void removeCancelSplitMsg(String seqno){
        chkCancelSplitSendMap.remove(seqno);
        logger.debug("##### [REMOVE After]chkCancelSplitSendMap : size:" + chkCancelSplitSendMap.size());
    }

    public Map<String,String> getSplitSenderManagerStatus(){
        Map<String,String> statusMap = new HashMap<String, String>();

        try {
            if(splitSenderThread.isAlive()) {
                statusMap.put("threadAlive", "TRUE");
                statusMap.put("status",""+splitSenderThread.getState().toString());
            }else{
                statusMap.put("threadAlive", "FALSE");
                statusMap.put("status","TERMINATION");
            }
            statusMap.put("splitMsgCnt",""+chkDelayFileInfoMap.size());
            if(mLogDir==null){
                mLogDir = UmsInitListener.webProperties.getProperty("UMS.TEMPDIR", "/home/uracle/split/");
                mLogDir = mLogDir+File.separator+SplitSendLogUtil.SPLIT_FOLDER;
            }
            statusMap.put("saveDirSrc",mLogDir);

        }catch (Exception e){
            logger.error(e.toString());
        }
        return statusMap;
    }

    public String getmLogDir() {
        return mLogDir;
    }


    public synchronized void removeLogFile(String seqno, String fileName){
        try {
            String logFilPath = mLogDir + File.separator + seqno + File.separator + fileName;
            File rmFile = new File(logFilPath);
            FileUtils.forceDelete(rmFile);

            removeCompletSplitFoler(seqno);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    /**
     * 해당 원장번호로 저장된 분할지연발송 폴더 전체 삭제
     * @param seqno
     */
    public synchronized void removeLogFolder(String seqno){
        try {
            String logFilPath = mLogDir + File.separator + seqno;
            File dir = new File(logFilPath);
            FileUtils.forceDelete(dir);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    public void removeAllSplitFoler(){
        File dir = new File(mLogDir);
        File[] fileList = dir.listFiles();
        if(fileList!=null){
            for(File file : fileList){
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void removeCompletSplitFoler(String seqno){
        try {
            String splitSeqSrc = mLogDir + File.separator + seqno;
            File dir = new File(splitSeqSrc);
            File[] fileList = dir.listFiles();
            if (fileList == null || fileList.length==0) {
                removeLogFolder(seqno);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
