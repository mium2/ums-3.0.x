package kr.uracle.ums.core.vo.setting;

public class ProviderVo {
	// 발송 채널명 
	private String CHANNEL;
	// 발송 비율(백분율)
	private Long RATIO=0l;
	// 발송 비율(/100)
	private double RATE=0;
	// 실제 발송비율(백분율)
	private double REALRATE=0;
	// 공급(중계)사
	private String PROVIDER;
	// 공급사 발송 건수
	private long SENTCNT=0l;
	
	public String getCHANNEL() { return CHANNEL; }
	public void setCHANNEL(String channel) { this.CHANNEL = channel; }
	public Long getRATIO() { return RATIO; }
	public void setRATIO(Long rate) { this.RATIO = rate;  this.RATE = (float)rate/100f;}
	public double getRATE() { return RATE; }
	public void setRATE(double rate) { this.RATE = rate;	}

	public double getREALRATE() { return REALRATE; }
	public void setREALRATE(long totalCnt) { this.REALRATE = ((double)SENTCNT/(double)totalCnt)*100; }

	public String getPROVIDER() { return PROVIDER; }
	public void setPROVIDER(String provider) { this.PROVIDER = provider; }
	public long getSENTCNT() { return SENTCNT; }
	public void setSENTCNT(long sentCnt) { this.SENTCNT = sentCnt; }
	public long increaseSentCnt(int cnt) {return (this.SENTCNT+=cnt);}

}
