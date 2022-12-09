package kr.uracle.ums.core.service.send.thread;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 25..
 * 해당 클래스는 로그인 사용자가 발송요청처리한 쓰레드 프로세스를 관리 하는 매니저이다.
 * 진행상태를 UI상으로 명확하게 표현하기 위해....
 */
public class ThreadSessionManager {
    private static ThreadSessionManager ourInstance = new ThreadSessionManager();

    public static ThreadSessionManager getInstance() {
        return ourInstance;
    }
    // 세션아이디 : 실행ThreadObject reference값
    private ConcurrentMap<String,Thread> sessionThreadMap = new ConcurrentHashMap<String,Thread>();

    private ThreadSessionManager() {
    }

    public void putSessionThread(String loginID, Thread processThread){
        sessionThreadMap.put(loginID,processThread);
    }

    public Thread getSessionThread(String loginID){
        return sessionThreadMap.get(loginID);
    }
}
