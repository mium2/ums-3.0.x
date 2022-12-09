package kr.uracle.ums.core.common;

import kr.uracle.ums.codec.redis.enums.SendType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Y.B.H(mium2) on 2019. 1. 22..
 */
public class Constants {

    public final static Locale LOCALE_KR = Locale.KOREA;
    public final static Locale LOCALE_EN = Locale.ENGLISH;
    public final static String PUSH_GUEST = "GUEST";
    public final static String REDIS_CUID_TABLE = ":CUID";
    public final static String REDIS_PUSHUSERINFO = ":PUSHUSERINFO";
    public final static String REDIS_UMS_MEMBER_TABLE = "UMS_MEMBER";
    public final static String REDIS_PUSH_SEND_QUEUE_RT = "PUSH_SEND_RT";
    public static final String REDIS_FATIGUE = "UMS_FATIGUE";

    public static final String RESULT_CODE =  "RESULTCODE";
    public static final String RESULT_MSG = "RESULTMSG";
    public static final String SUCCESS = "OK";

    public static final String TASKID_PUSH_ZK = "PUSH";
    public static final String TASKID_ALT_ZK = "ALT";
    public static final String TASKID_FRT_ZK = "FRT";
    public static final String TASKID_SMS_ZK = "SMS";

    public static enum SMS_PROVIDER {KT,LGU}
    public static enum RCS_PROVIDER {KT,LGU}
    public static enum KKO_PROVIDER {LGCNS,LOTTE,AM}
    public static enum NAVER_PROVIDER {MTS}
    public static enum TARGET_USER_TYPE {AU, AP, OU, MU, NM, NC}

    public static Map<String,String> SEND_KIND_MAP = new HashMap<String, String>();
    static {
        //PUSH, KKOALT, KKOFRT, SMS, MMS;
        SEND_KIND_MAP.put("PUSH", "PUSH");
        SEND_KIND_MAP.put("KKOALT", "알림톡");
        SEND_KIND_MAP.put("KKOFRT", "친구톡");
        SEND_KIND_MAP.put("SMS", "SMS");
        SEND_KIND_MAP.put("LMS", "LMS");
        SEND_KIND_MAP.put("MMS", "MMS");
        SEND_KIND_MAP.put("RCS_SMS","RCS_SMS");
        SEND_KIND_MAP.put("RCS_LMS","RCS_LMS");
        SEND_KIND_MAP.put("RCS_MMS","RCS_MMS");
        SEND_KIND_MAP.put("RCS_TMPLT","RCS 스타일 템플릿");
        SEND_KIND_MAP.put("RCS_FB_SMS","RCS 기본 템플릿 ");
        SEND_KIND_MAP.put("RCS_FB_LMS","RCS 서술 템플릿");
        SEND_KIND_MAP.put("NAVERT","네이버톡");
    }

    public static Map<String,String> MSG_TYPE_MAP = new HashMap<String, String>();
    static {
        //A, I;
        MSG_TYPE_MAP.put("A", "광고");
        MSG_TYPE_MAP.put("I", "정보");
    }

    public static Map<String,String> RESERVE_STATUS_MAP = new HashMap<String, String>();
    static {
        RESERVE_STATUS_MAP.put("R", "예약");
        RESERVE_STATUS_MAP.put("H", "보류");
        RESERVE_STATUS_MAP.put("P", "처리중");
        RESERVE_STATUS_MAP.put("F", "실패");
        RESERVE_STATUS_MAP.put("S", "성공");
    }

    public static Set<String> TARGET_USER_TYPE_SET = new HashSet<>();
    static {
        TARGET_USER_TYPE_SET.add("AU"); //전체UMS회원
        TARGET_USER_TYPE_SET.add("AP"); //전체푸시유저
        TARGET_USER_TYPE_SET.add("OU"); //조직도 등록 전체유저
        TARGET_USER_TYPE_SET.add("MU"); //UMS 등록회원 정보를 이용 아이디 만으로 발송. 1개발송 : 아이디, 여러개 발송 ["아이디1","아이디2"...]
        TARGET_USER_TYPE_SET.add("MP"); //푸시등록된 아이디로 타겟팅. 푸시가입시 핸드폰번호가 등록되지 않았을 경우는 사용불가.
        TARGET_USER_TYPE_SET.add("NM"); //비회원. CUIDS파라미터 필수. {"아이디":["핸드폰번호","이름"},...}
        TARGET_USER_TYPE_SET.add("MC"); //UMS등록된 회원아이디로 CSV 발송. CSV파일 헤더 #{아이디} 반드시 필요
        TARGET_USER_TYPE_SET.add("NC"); //비회원 CSV발송. CSV파일 헤더 #{아이디}, #{핸드폰번호}, #{이름} 필수
    }
    
	public final static Map<String, String> rcsTypeMap = new ConcurrentHashMap<String, String>(12);
	static {
		rcsTypeMap.put("SS000000",   "standalone");
		rcsTypeMap.put("SL000000",   "standalone");
		rcsTypeMap.put("SMwThT00",   "standalone media top");
		rcsTypeMap.put("SMwLhX00", 	 "standalone media top");
		rcsTypeMap.put("CMwShS0300", "carousel small");
		rcsTypeMap.put("CMwShS0400", "carousel small");
		rcsTypeMap.put("CMwShS0500", "carousel small");
		rcsTypeMap.put("CMwShS0600", "carousel small");
		rcsTypeMap.put("CMwMhM0300", "carousel medium");
		rcsTypeMap.put("CMwMhM0400", "carousel medium");
		rcsTypeMap.put("CMwMhM0500", "carousel medium");
		rcsTypeMap.put("CMwMhM0600", "carousel medium");
    }		
			    
	public final static Map<String, String> rcsServiceMap = new ConcurrentHashMap<String, String>(12);
	static {
		rcsServiceMap.put("FF001", "승인");
		rcsServiceMap.put("FF002", "입금");
		rcsServiceMap.put("FF003", "출금");
		rcsServiceMap.put("FF004", "취소");
		rcsServiceMap.put("FF005", "명세서");
		rcsServiceMap.put("FF005", "명세서");
		rcsServiceMap.put("GG001", "회원가입");
		rcsServiceMap.put("GG002", "인증");
		rcsServiceMap.put("EE001", "예약안내");
		rcsServiceMap.put("CC001", "출고");
		rcsServiceMap.put("CC002", "주문");
		rcsServiceMap.put("CC003", "배송");
	}
	
	public final static Map<String, String> lguSendTypeMap = new ConcurrentHashMap<String, String>(12); 
	static {
		lguSendTypeMap.put("10",	SendType.RCS_SMS.toString());
		lguSendTypeMap.put("11",	SendType.RCS_LMS.toString());
		lguSendTypeMap.put("12",	SendType.RCS_MMS.toString());
		lguSendTypeMap.put("20",	SendType.RCS_DESC.toString());
		lguSendTypeMap.put("21",	SendType.RCS_CELL.toString());
		lguSendTypeMap.put("22",	SendType.RCS_FREE.toString());
		lguSendTypeMap.put(SendType.RCS_SMS.toString(),	"10");
		lguSendTypeMap.put(SendType.RCS_LMS.toString(),	"11");
		lguSendTypeMap.put(SendType.RCS_MMS.toString(),	"12");
		lguSendTypeMap.put(SendType.RCS_DESC.toString(), 	"20");
		lguSendTypeMap.put(SendType.RCS_CELL.toString(),	"21");
		lguSendTypeMap.put(SendType.RCS_FREE.toString(),	"22");

    }

	public final static Map<String, String> lotteSendTypeMap = new ConcurrentHashMap<String, String>(12);
	static {
		lotteSendTypeMap.put("10",	SendType.RCS_SMS.toString());
		lotteSendTypeMap.put("11",	SendType.RCS_LMS.toString());
		lotteSendTypeMap.put("12",	SendType.RCS_MMS.toString());
		lotteSendTypeMap.put("13",	SendType.RCS_DESC.toString());

		lotteSendTypeMap.put(SendType.RCS_SMS.toString(),	"10");
		lotteSendTypeMap.put(SendType.RCS_LMS.toString(),	"11");
		lotteSendTypeMap.put(SendType.RCS_MMS.toString(),	"12");
		lotteSendTypeMap.put(SendType.RCS_DESC.toString(), 	"13");
		lotteSendTypeMap.put(SendType.RCS_CELL.toString(),	"13");
		lotteSendTypeMap.put(SendType.RCS_FREE.toString(),	"13");

	}

	public final static Map<String, Integer> rcsMMSImageCountMap  = new ConcurrentHashMap<String, Integer>(12);
	static {

		rcsMMSImageCountMap.put("SMwThT00",   1);
		rcsMMSImageCountMap.put("SMwLhX00",   1);
		rcsMMSImageCountMap.put("CMwShS0300", 3);
		rcsMMSImageCountMap.put("CMwShS0400", 4);
		rcsMMSImageCountMap.put("CMwShS0500", 5);
		rcsMMSImageCountMap.put("CMwShS0600", 6);
		rcsMMSImageCountMap.put("CMwMhM0300", 3);
		rcsMMSImageCountMap.put("CMwMhM0400", 4);
		rcsMMSImageCountMap.put("CMwMhM0500", 5);
		rcsMMSImageCountMap.put("CMwMhM0600", 6);
    }		
	
	
	public final static Map<String , SendType> rcsSendTypeMap = new ConcurrentHashMap<String, SendType>(12);
	static {
		rcsSendTypeMap.put("SS000000",   SendType.RCS_SMS);
		rcsSendTypeMap.put("SL000000",   SendType.RCS_LMS);
		rcsSendTypeMap.put("SMwThT00",   SendType.RCS_MMS);
		rcsSendTypeMap.put("SMwLhX00",   SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwShS0300", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwShS0400", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwShS0500", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwShS0600", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwMhM0300", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwMhM0400", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwMhM0500", SendType.RCS_MMS);
		rcsSendTypeMap.put("CMwMhM0600", SendType.RCS_MMS);
    }		
	
	public final static Map<SendType , String> rcsChargeTypeMap = new ConcurrentHashMap<SendType, String>(6);
	static {
		rcsChargeTypeMap.put(SendType.RCS_SMS,  "RCS_SMS");
		rcsChargeTypeMap.put(SendType.RCS_LMS,	"RCS_LMS");
		rcsChargeTypeMap.put(SendType.RCS_MMS,  "RCS_MMS");
		rcsChargeTypeMap.put(SendType.RCS_FREE, "RCS_TMPL");
		rcsChargeTypeMap.put(SendType.RCS_CELL, "RCS_TMPL");
		rcsChargeTypeMap.put(SendType.RCS_DESC, "RCS_TMPL");
    }

	public static SendType getSendType(String key) {
		return rcsSendTypeMap.get(key);
	}
	
	public static int getRcsMMSImageCount(String messageBaseId) {
		return rcsMMSImageCountMap.get(messageBaseId);
	}
	
	public static String getLguSendType(String key) {
		return lguSendTypeMap.get(key);
	}

	public static String getLotteSendType(String key) {
		return lotteSendTypeMap.get(key);
	}

}

