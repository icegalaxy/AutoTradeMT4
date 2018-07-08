package net.icegalaxy;

import java.util.Date;

public class Candle {
	private long time;
	private double low;
	private double high;
	private double open;
	private double close;
	private int volume;
	
//	private double rsi;

	public Candle(long time, double high, double low, double open,
			double close, int volume) {
		this.time = time;
		this.high = high;
		this.low = low;
		this.open = open;
		this.close = close;
		this.volume = volume;
//		this.rsi = rsi;
	}

	public double getLow() {
		return this.low;
	}

	public double getHigh() {
		return this.high;
	}

	public double getOpen() {
		return this.open;
	}

	public double getClose() {
		return this.close;
	}

	public int getVolume() {
		return this.volume;
	}

	public long getTime() {
		return this.time;
	}
	
//	public double getRsi(){
//		return this.rsi;
//	}
	
	public boolean isYinCandle()
	{
		return getClose() < getOpen();
	
	}
	
	public boolean isYangCandle()
	{
		return getClose() > getOpen();
	
	}
	
	
	
}