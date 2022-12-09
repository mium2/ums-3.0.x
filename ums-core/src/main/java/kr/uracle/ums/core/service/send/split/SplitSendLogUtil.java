package kr.uracle.ums.core.service.send.split;

import com.google.gson.Gson;
import kr.uracle.ums.core.common.UmsInitListener;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by Y.B.H(mium2)
 */
public class SplitSendLogUtil {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private String mLogDir;
    private FileOutputStream fio = null;
    public static final String SPLIT_FOLDER = "split";
    private final long DEL_CHK_MILLISECOND = 43200000;  // 12시간
    private Gson gson = new Gson();

    public SplitSendLogUtil(){
        mLogDir = UmsInitListener.webProperties.getProperty("UMS.TEMPDIR", "/home/uracle/split/");
        mLogDir = mLogDir+File.separator+SPLIT_FOLDER;
    }

    public SplitSendLogUtil(final String seqno, final String fileName){
        try {
            mLogDir = UmsInitListener.webProperties.getProperty("UMS.TEMPDIR", "/home/uracle/split/");
            mLogDir = mLogDir+File.separator+SPLIT_FOLDER;
            Date currentTime = new Date();
            new File(mLogDir + File.separatorChar + seqno).mkdirs();
            logger.debug("### split file src:"+mLogDir + File.separatorChar + seqno);
            final String logFilePath = mLogDir + File.separatorChar + seqno + File.separator + fileName;
            final File f = new File(logFilePath);
            if (!f.exists()) {
                f.createNewFile();
                logger.debug("### spliet create file :"+f.getAbsolutePath());
            }

            fio = new FileOutputStream(logFilePath, true);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void write(String message) throws IOException{
        fio.write((message + LINE_SEPARATOR).getBytes());
        fio.flush();
    }

    public void nowFileClose(){
        if(fio!=null){
            try {
                fio.close();
            } catch (IOException e) {
//				e.printStackTrace();
            }
        }
    }

    public void changeWriteFile(final String seqno, final String fileName) throws Exception{
        if(fio!=null){
            fio.close();
        }
        final String seqnoPath = mLogDir + File.separatorChar + seqno;
        final String logFilePath = mLogDir + File.separatorChar + seqno + File.separator + fileName;

        logger.debug("#### seqnoPath:"+seqnoPath+"   logFilePath: "+logFilePath);
        final File seqnoDir = new File(seqnoPath);
        if(!seqnoDir.exists()){
            seqnoDir.mkdirs();
        }

        final File f = new File(logFilePath);
        if(!f.exists()) {
            f.createNewFile();
        }
        fio = new FileOutputStream(logFilePath, true);
    }



    public void removeAllSplitFolerAfterDateCheck(){
        File dir = new File(mLogDir);
        File[] splitDirList = dir.listFiles();
        if(splitDirList!=null){
            for(File splitDir : splitDirList){
                try {
                    if(splitDir.isDirectory()){
                        File[] splitFiles = splitDir.listFiles();
                        if(splitFiles!=null && splitFiles.length>0){
                            continue;
                        }
                        long modifiedTimeStamp = splitDir.lastModified();
                        // 삭제 체크 날자 (12시간)이 지난 폴더들만 삭제 처리.
                        if((System.currentTimeMillis()-modifiedTimeStamp) > DEL_CHK_MILLISECOND){
                            FileUtils.forceDelete(splitDir);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


