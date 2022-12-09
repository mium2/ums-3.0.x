package kr.uracle.ums.core.util;

/**
 * Created by Y.B.H(mium2) on 2014-06-23
 */
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil implements Serializable {
    private static final long serialVersionUID = 2541140426147500192L;

    public static String getSysDateTime(){
        // 현재시간을 구할 수 있는 객체 생성
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        StringBuffer buffer = new StringBuffer();

        String year = StringUtil.toZoroString( calendar.get(java.util.Calendar.YEAR) , 4 );
        String month = StringUtil.toZoroString( calendar.get(java.util.Calendar.MONTH) + 1 , 2 );
        String day = StringUtil.toZoroString( calendar.get(java.util.Calendar.DATE) , 2 );
        String hour = StringUtil.toZoroString( calendar.get(java.util.Calendar.HOUR_OF_DAY) , 2 );
        String min = StringUtil.toZoroString( calendar.get(java.util.Calendar.MINUTE) , 2 );
        String second = StringUtil.toZoroString( calendar.get(java.util.Calendar.SECOND) , 2 );
        buffer.append(year);
        buffer.append(month);
        buffer.append(day);
        buffer.append(hour);
        buffer.append(min);
        buffer.append(second);
        return buffer.toString();
    }

    public static String getSysDay(){
        // 현재시간을 구할 수 있는 객체 생성
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        StringBuffer buffer = new StringBuffer();

        String year = StringUtil.toZoroString(calendar.get(java.util.Calendar.YEAR), 4);
        String month = StringUtil.toZoroString(calendar.get(java.util.Calendar.MONTH) + 1, 2);
        String day = StringUtil.toZoroString( calendar.get(java.util.Calendar.DATE) , 2 );

        buffer.append(year);
        buffer.append(month);
        buffer.append(day);

        return buffer.toString();
    }

    public static String getDashFormatSysDay(){
        // 현재시간을 구할 수 있는 객체 생성
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String sysNowDate = formatter.format(calendar.getTime());

        return sysNowDate;
    }

    public static String getCusDay(){
        Calendar calendar = Calendar.getInstance();
        StringBuffer buffer = new StringBuffer();

        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        String sysNowDate = formatter.format(calendar.getTime());

        int weekInt = calendar.get(Calendar.DAY_OF_WEEK);
        weekInt = weekInt-1;
        if(weekInt==0){
            weekInt = 7;
        }
        buffer.append("#"+weekInt);
        buffer.append(sysNowDate);
        return buffer.toString();
    }

    ///특정일에서 일정 기간후의 날짜 구하기
    public static String getDate ( int iDay ,String strChar) {
        Calendar temp=Calendar.getInstance ();
        StringBuffer sbDate=new StringBuffer ();
        temp.add ( Calendar.DAY_OF_MONTH, iDay );
        int nYear = temp.get ( Calendar.YEAR );
        int nMonth = temp.get ( Calendar.MONTH ) + 1;
        int nDay = temp.get ( Calendar.DAY_OF_MONTH );

        sbDate.append ( nYear );
        if(!strChar.equals("")){sbDate.append (strChar);}

        if (nMonth < 10){sbDate.append ( "0" );}
        sbDate.append(nMonth);

        if(!strChar.equals("")){sbDate.append (strChar);}

        if (nDay < 10){sbDate.append ( "0" );}
        sbDate.append ( nDay );

        return sbDate.toString ();
    }
    ///오늘날짜 기준 요구달의 날짜 구하기
    public static String getReqMonth ( int iMon ,String strChar) {
        Calendar cal = Calendar.getInstance ();//오늘 날짜를 기준으루..
        StringBuffer sbDate = new StringBuffer();
        cal.add (Calendar.MONTH, iMon ); //iMon 요구달 (-1일경우 일개월 전....)
        int nYear = cal.get ( Calendar.YEAR );
        int nMonth = cal.get ( Calendar.MONTH ) + 1;
        int nDay = cal.get ( Calendar.DATE );

        sbDate.append ( nYear );
        if(!strChar.equals("")){sbDate.append (strChar);}

        if (nMonth < 10){sbDate.append ( "0" );}
        sbDate.append(nMonth);
        if(!strChar.equals("")){sbDate.append (strChar);}

        if (nDay < 10){sbDate.append ( "0" );}
        sbDate.append ( nDay );

        return sbDate.toString ();
    }

    ///오늘 날짜와 비교
    public static int CompareDate(int year, int month, int day, int time, int minute, int second){    /// 0 작고 1일면 같고 2이면 크다
        int returnValue=0;
        Calendar adate = Calendar.getInstance();
        Calendar bdate = Calendar.getInstance();
        bdate.set(year, month, day, time, minute, second);
        if(adate.after(bdate)){
            returnValue = 2;
        }else if(adate.before(bdate)){
            returnValue = 0;
        }else{
            returnValue = 1;
        }
        return returnValue;
    }

    ///날짜와 비교
    public static int CompareDate2(int year, int month, int day, int time, int minute, int second,Calendar b){    /// 0 : A날보다 B날자가 전, 1일면 같고 2 : A날보다 B날자가 후
        int returnValue=0;
        Calendar adate = Calendar.getInstance();
        Calendar bdate = b;
        adate.set(year, month - 1, day, time, minute, second);

        if(adate.after(bdate)){
            returnValue = 2;
        }else if(adate.before(bdate)){
            returnValue = 0;
        }else{
            returnValue = 1;
        }
        return returnValue;
    }

    // 만료제한시간 타임스템프 만들기.
    // 주의 넘어온 시간이 현재 시간보다 작을 경우 익일로 세팅한다.(하루 밀리세컨드 24*60*60*1000)
    public static long getMakeTimeStamp(int time, int minute){
        Calendar adate = Calendar.getInstance();
        int now_year = adate.get(java.util.Calendar.YEAR);
        int now_month = adate.get(java.util.Calendar.MONTH);
        int now_day = adate.get(java.util.Calendar.DATE);
        int now_hour = adate.get(Calendar.HOUR_OF_DAY);
        int now_minute = adate.get(Calendar.MINUTE);
        adate.set(now_year, now_month, now_day, time, minute, 0);
        long returnTimeMilis = adate.getTimeInMillis();
        if(now_hour>time){ // 요청시간이 현재시간보다 작을 경우 다음날로 처리해야함
            returnTimeMilis = returnTimeMilis+(24*60*60*1000);
        }else if(now_hour==time){
            if(now_minute>minute){
                returnTimeMilis = returnTimeMilis+(24*60*60*1000);
            }
        }
        return returnTimeMilis;
    }

    // 예약발송일 경우 제한시간 타임스템프 만들기.
    // 주의 넘어온 시간이 현재 시간보다 작을 경우 익일로 세팅한다.(하루 밀리세컨드 24*60*60*1000)
    public static long getMakeTimeStampFromReserve(int time, int minute, String req_reserveDate) throws Exception{
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date reserveDate = format.parse(req_reserveDate);

        Calendar adate = Calendar.getInstance();
        adate.setTime(reserveDate);
        int now_year = adate.get(java.util.Calendar.YEAR);
        int now_month = adate.get(java.util.Calendar.MONTH);
        int now_day = adate.get(java.util.Calendar.DATE);
        int now_hour = adate.get(Calendar.HOUR_OF_DAY);
        int now_minute = adate.get(Calendar.MINUTE);

        // 발송제한 시간 셋팅
        adate.set(now_year, now_month, now_day, time, minute, 0);
        long returnTimeMilis = adate.getTimeInMillis();

        if(now_hour>time){ // 요청시간이 현재시간보다 작을 경우 다음날로 처리해야함
            returnTimeMilis = returnTimeMilis+(24*60*60*1000);
        }else if(now_hour==time){
            if(now_minute>minute){
                returnTimeMilis = returnTimeMilis+(24*60*60*1000);
            }
        }
        return returnTimeMilis;
    }

    public static int GetDifferenceOfDate ( int nYear1, int nMonth1, int nDate1, int nYear2, int nMonth2, int nDate2 ){
        Calendar cal = Calendar.getInstance ( );
        int nTotalDate1 = 0, nTotalDate2 = 0, nDiffOfYear = 0, nDiffOfDay = 0;
        if ( nYear1 > nYear2 ){
            for ( int i = nYear2; i < nYear1; i++ ) {
                cal.set ( i, 12, 0 );
                nDiffOfYear += cal.get ( Calendar.DAY_OF_YEAR );
            }
            nTotalDate1 += nDiffOfYear;
        }else if ( nYear1 < nYear2 ){
            for ( int i = nYear1; i < nYear2; i++ ){
                cal.set ( i, 12, 0 );
                nDiffOfYear += cal.get ( Calendar.DAY_OF_YEAR );
            }
            nTotalDate2 += nDiffOfYear;
        }
        cal.set ( nYear1, nMonth1-1, nDate1 );
        nDiffOfDay = cal.get ( Calendar.DAY_OF_YEAR );
        nTotalDate1 += nDiffOfDay;
        cal.set ( nYear2, nMonth2-1, nDate2 );
        nDiffOfDay = cal.get ( Calendar.DAY_OF_YEAR );
        nTotalDate2 += nDiffOfDay;
        return nTotalDate1-nTotalDate2;
    }

    public static long getElapsedDay (String startDate) throws Exception{
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate = formatter.parse(startDate);
        Date endDate = new Date();

        // 시간차이를 시간,분,초를 곱한 값으로 나누면 하루 단위가 나옴
        long diff = endDate.getTime() - beginDate.getTime();
        long diffDays = diff / (24 * 60 * 60 * 1000);

        return diffDays;
    }
}