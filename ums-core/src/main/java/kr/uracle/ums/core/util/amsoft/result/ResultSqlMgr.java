package kr.uracle.ums.core.util.amsoft.result;

import com.google.gson.Gson;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.dao.ums.UmsDao;
import org.apache.commons.lang3.StringUtils;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultSqlMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static ResultSqlMgr instance=null;
    private ConcurrentLinkedQueue<ResultUmsLogBean> sqlQueue = new ConcurrentLinkedQueue<ResultUmsLogBean>();
    private final ResultSqlThread[] resultSqlThreads =  new ResultSqlThread[3];
    private int processCnt = 0;
    private long startMilSec = 0;

    private UmsDao umsDao;

    private final Gson gson = new Gson();
    private String FILEDB_PATH;
    private boolean useFileDB = false;
    private MVStore store = null;
    private final String FILEDB_NAME="UMS_AM.UQ";
    private final List<Integer> keyList = new ArrayList<Integer>();
    private Integer MAX_KEY_SIZE = 100000; // 10만
    private AtomicInteger atomicInteger;

    private ResultSqlMgr(){}

    public static ResultSqlMgr getInstance() {
        if(instance==null){
            instance = new ResultSqlMgr();
        }
        return instance;
    }

    public void initailize() {
        this.umsDao = UmsInitListener.wContext.getBean(UmsDao.class);
        String useYn = UmsInitListener.webProperties.getProperty("FILEDB.USEYN","N");
        useFileDB = useYn.trim().equalsIgnoreCase("Y");

        String path = UmsInitListener.webProperties.getProperty("FILEDB.PATH","");
        if(path.endsWith(File.separator) == false){
            path = path+File.separator;
        }
        FILEDB_PATH = path;

        //파일 DB 사용 여부
        if(StringUtils.isBlank(FILEDB_PATH)) useFileDB = false;
        try {
            // 기존 미처리 데이터가 있다면 KEYLIST 초기화 하고 SORT
            if(useFileDB) {
                atomicInteger = new AtomicInteger(-1);

                if(FILEDB_PATH.endsWith(File.separator) == false) {
                    FILEDB_PATH = FILEDB_PATH+File.separator+FILEDB_NAME;
                }else {
                    FILEDB_PATH = FILEDB_PATH+FILEDB_NAME;
                }
                Path p= Paths.get(FILEDB_PATH);
                if(Files.exists(p) == false) {
                    Files.createFile(p);
                }

                store = new MVStore.Builder().
                        fileName(FILEDB_PATH).
                        encryptionKey("URACLE".toCharArray()).
                        compress().
                        open();

                MVMap<Integer, ResultUmsLogBean> map = store.openMap("data");
                // keyList 초기화 및 sorting 필요
                if(map.size()>0) {
                    for(Integer k: map.keySet()) {
                        keyList.add(k);
                    }
                    Collections.sort(keyList);
                    if(keyList.size()>0) {
                        int lastKey = keyList.get(keyList.size() - 1);
                        atomicInteger.set(lastKey + 1); // 마지막 키 셋팅
                    }
                    logger.info("AM 실패메세지 파일큐에서 로드. 사이즈: "+map.size());
                }
            }
        } catch (IOException e) {
            logger.error("AM 실패메세지 처리 파일 DB 파일 생성 중 에러 발송:"+e);
            useFileDB = false;
        }

        // 요청 처리 Work 쓰레드 구동
        for(int i=0; i< resultSqlThreads.length; i++){
            resultSqlThreads[i] = new ResultSqlThread("ResultSqlThread-"+i, this);
            resultSqlThreads[i].start();
        }

        logger.info("####################################");
        logger.info("# "+(useFileDB?"파일큐 모드로":"")+" AM T_UMS_LOG 등록 Thread 구동!");
        logger.info("####################################");
    }

    public synchronized void putWork(ResultUmsLogBean _work){
        if(useFileDB) {
            if(store == null || store.isClosed()) {
                store = new MVStore.Builder().
                        fileName(FILEDB_PATH).
                        encryptionKey("URACLE".toCharArray()).
                        compress().
                        open();
            }
            MVMap<Integer, ResultUmsLogBean> map = store.openMap("data");
            int key = atomicInteger.incrementAndGet();
            // 키는 천만 순환구조로 만듬.
            if(key>MAX_KEY_SIZE){
                atomicInteger.set(0);
                key = atomicInteger.incrementAndGet();
            }
            map.put(key, _work);
            keyList.add(key);
        }else {
            sqlQueue.offer(_work);
        }
        notify();
    }

    public synchronized ResultUmsLogBean takeWork() throws InterruptedException {
        ResultUmsLogBean xWork= null;
        try {
            if(store == null || store.isClosed()) {
                store = new MVStore.Builder().
                        fileName(FILEDB_PATH).
                        encryptionKey("URACLE".toCharArray()).
                        compress().
                        open();
            }
            MVMap<Integer, ResultUmsLogBean> map = null;
            do {
                if(keyList.size()>0) {
                    map = store.openMap("data");
                    int key = keyList.remove(0);
                    xWork = map.get(key);
                    map.remove(key);
                }else {
                    try{
                        logger.trace("### [ResultSqlThread WAIT] WORK THREAD NAME : [" + Thread.currentThread().getName() + "]  WAITING!");
                        wait();
                    }catch(InterruptedException e){
                        logger.info("[ResultSqlThread]: InterruptedException");
                    }
                }
            }while(xWork ==null);
        }catch (Exception e){
            logger.error("AM 실패메세지 결과처리쓰레드 File Queue에서 일감 가져오던 중 에러 발생 : "+e.toString());
            Thread.sleep(3000);
        }
        infoPrint();
        return xWork;
    }

    public synchronized void workNotify(){
        notifyAll();
    }

    public void destory(){
        try {
            fileQueueCommit();
            if (resultSqlThreads != null) {
                for (int i = 0; i < resultSqlThreads.length; i++) {
                    if(resultSqlThreads[i]!=null){
                        resultSqlThreads[i].setRun(false);
                        resultSqlThreads[i].interrupt();
                    }
                }
            }
            if(store != null)store.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 정보를 주기적으로 출력하는 것이기 때문 syncronize를 잡아 줄 필요 없음. 중요하지 않음.
     */
    public void infoPrint(){
        processCnt++;
        if(processCnt==1){
            startMilSec = System.currentTimeMillis();
        }
        if(processCnt%1000==0){
            logger.info("## process cnt : {}",processCnt);
        }
        if(processCnt==10000){
            processCnt = 0; //0으로 초기화
            long endMilSec = System.currentTimeMillis();
            logger.info("## AM 실패메세지  processing. Elapsed time : {}, {}",endMilSec-startMilSec, (endMilSec-startMilSec)/1000);
        }
    }


    public UmsDao getUmsDao() {
        return umsDao;
    }


    public synchronized void fileQueueCommit(){
        try {
            if(store!=null) {
                store.commit();
                logger.info("AM 실패메세지 파일처리큐 commit 호출");
            }
        }catch (Exception e){
            logger.error(e.toString());
        }
    }
}
