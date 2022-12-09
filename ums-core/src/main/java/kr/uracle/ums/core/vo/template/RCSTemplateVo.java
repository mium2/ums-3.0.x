package kr.uracle.ums.core.vo.template;

public class RCSTemplateVo {
	
    private static final long serialVersionUID = 1L;

    // 내부 템플릿 관리 ID
    private String TMPLT_ID;
    
    // 템플릿 코드
    private String MESSAGEBASE_ID;
     
    // 템플릿 유형 ID - TYPE + SERVICE = GG003C : 스타일 - 승인
    private String MESSAGEBASEFORM_ID= "";
    
    // 템플릿 명 
    private String TMPLT_NAME= "";
    
    // 브랜드 ID
    private String BRAND_ID ="";
    
    // 비고
    private String NOTE; 
    
    // 템플릿 제목
    private String TITLE;
    
    // 템플릿 내용
    private String CONTENT;
    
    // 템플릿 버튼 - json array string {"buttons":[]}
    private String BUTTONS;
    
    // 템플릿 버튼 처리 타입
    private String BTYPE;
    
    // 템플릿 타입 - RCS_SMS, RCS_LMS, RCS_MMS, RCS_FREE, RCS_CELL, RCS_DESC
    private String RCS_TYPE;
    
    // 템플릿 카테고리 - 일반, 금융, 유통/커머스, 교통, 엔터테인먼트, IT서비스, 공공
    private String CATEGORY;
    
    // 템플렛 카테고리 세부 - 승인, 입금, 출금, 출고, 주문, 배송, 예약, 회원가입, 인증
    private String SERVICE;
    
    // 템플렛 승인 상태 - 저장, 승인대기, 검수시작, 승인, 반려, 검수완료, 승인대기(수정), 검수시작(수정), 반려(수정), 검수완료(수정)
    private String STATUS;
    
    // 템플릿 정산 유형 - sms, lms, mms tmplt
    private String PRODUCT_CODE;
    
    // 템플릿 최초 등록일시
    private String REGISTER_DATE;
    
    // 템플릿 승인완료 일시
    private String APPROVAL_DATE;
    
    // 템플릿 수정 일시 - TABLE IO 일시
    private String UPDATE_DATE;
    
    // 템플릿 최초 등록자 ID
    private String REGISTER_ID;
    
    // 템플릿 수정자 ID
    private String UPDATE_ID;

	public String getTMPLT_ID() { return TMPLT_ID; }
	public void setTMPLT_ID(String TMPLT_ID) { this.TMPLT_ID = TMPLT_ID; }

	public String getMESSAGEBASE_ID() { return MESSAGEBASE_ID; }
	public void setMESSAGEBASE_ID(String MESSAGEBASE_ID) { this.MESSAGEBASE_ID = MESSAGEBASE_ID; }

	public String getMESSAGEBASEFORM_ID() { return MESSAGEBASEFORM_ID;	}
	public void setMESSAGEBASEFORM_ID(String MESSAGEBASEFORM_ID) { this.MESSAGEBASEFORM_ID = MESSAGEBASEFORM_ID; }

	public String getTMPLT_NAME() { return TMPLT_NAME;	}
	public void setTMPLT_NAME(String TMPLT_NAME) { this.TMPLT_NAME = TMPLT_NAME; }

	public String getBRAND_ID() { return BRAND_ID;	}
	public void setBRAND_ID(String BRAND_ID) { this.BRAND_ID = BRAND_ID; }

	public String getNOTE() { return NOTE; }
	public void setNOTE(String NOTE) { this.NOTE = NOTE; }

	public String getTITLE() { return TITLE; }
	public void setTITLE(String TITLE) { this.TITLE = TITLE; }

	public String getCONTENT() { return CONTENT; }
	public void setCONTENT(String CONTENT) { this.CONTENT = CONTENT; }

	public String getBUTTONS() { return BUTTONS; }
	public void setBUTTONS(String BUTTONS) { this.BUTTONS = BUTTONS; }

	public String getBTYPE() { return BTYPE; }
	public void setBTYPE(String BTYPE) { this.BTYPE = BTYPE; }	
	
	public String getRCS_TYPE() { return RCS_TYPE; }
	public void setRCS_TYPE(String RCS_TYPE) { this.RCS_TYPE = RCS_TYPE; }

	public String getCATEGORY() { return CATEGORY; }
	public void setCATEGORY(String CATEGORY) { this.CATEGORY = CATEGORY; }

	public String getSERVICE() { return SERVICE; }
	public void setSERVICE(String SERVICE) { this.SERVICE = SERVICE; }

	public String getSTATUS() { return STATUS; }
	public void setSTATUS(String STATUS) { this.STATUS = STATUS; }

	public String getPRODUCT_CODE() { return PRODUCT_CODE; }
	public void setPRODUCT_CODE(String PRODUCT_CODE) { this.PRODUCT_CODE = PRODUCT_CODE; }

	public String getREGISTER_DATE() { return REGISTER_DATE; }
	public void setREGISTER_DATE(String REGISTER_DATE) { this.REGISTER_DATE = REGISTER_DATE; }

	public String getAPPROVAL_DATE() { return APPROVAL_DATE; }
	public void setAPPROVAL_DATE(String APPROVAL_DATE) { this.APPROVAL_DATE = APPROVAL_DATE; }

	public String getUPDATE_DATE() { return UPDATE_DATE; }
	public void setUPDATE_DATE(String UPDATE_DATE) { this.UPDATE_DATE = UPDATE_DATE; }

	public String getREGISTER_ID() { return REGISTER_ID; }
	public void setREGISTER_ID(String REGISTER_ID) { this.REGISTER_ID = REGISTER_ID; }

	public String getUPDATE_ID() { return UPDATE_ID; }
	public void setUPDATE_ID(String UPDATE_ID) { this.UPDATE_ID = UPDATE_ID; }	
  
}
