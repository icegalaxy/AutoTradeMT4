package net.icegalaxy;

import java.sql.Time;
import java.util.ArrayList;

import org.zeromq.ZMQ;

public abstract class MT4Puller implements Runnable {
	
	ZMQ.Context context;
	ZMQ.Socket reqSocket;
	ZMQ.Socket pullSocket;
	ZMQ.Socket subscriber; 
	
	String product = "GBPUSD";
	
	// Sample Commands for ZeroMQ MT4 EA
	byte[] eurusd_buy_order = "TRADE|OPEN|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
	byte[] eurusd_sell_order = "TRADE|OPEN|1|EURUSD|0|50|50|Python-to-MT4".getBytes();
	byte[] eurusd_closebuy_order = "TRADE|CLOSE|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
	//byte[] get_data = "DATA|GBPUSD|PERIOD_M5|D'2018.06.15 00:00:00'|D'2018.07.02 00:00:00'".getBytes();
	byte[] get_data = "DATA|GBPUSD|1|0|10".getBytes();
	
	/*
	byte[] get_dataH = "DATA|GBPUSD|1|H|0|2000".getBytes();
	byte[] get_dataL = "DATA|GBPUSD|1|L|0|2000".getBytes();
	byte[] get_dataC = "DATA|GBPUSD|1|C|0|2000".getBytes();
	byte[] get_dataT = "DATA|GBPUSD|1|T|0|2000".getBytes();
	byte[] get_dataV = "DATA|GBPUSD|1|V|0|2000".getBytes();
	*/
	
	byte[] get_rates = "RATES|GBPUSD".getBytes();
	
	
	private double currentBid;
	private double currentAsk;
	
	public static ArrayList<Candle> previousCandles;
	public static boolean MT4PullerReady;
	
	
	public static double[] RSIs;
	public static double[][] EMAs;
	public static double[] MACDs;
	
	
	
//	public static ArrayList<Candle> currentCandle;
	
	public MT4Puller()
	{
		 //Create ZMQ Context
		   context = ZMQ.context(1);
		   
		// Create REQ Socket
		    reqSocket = context.socket(ZMQ.REQ);
		    reqSocket.connect("tcp://localhost:5555");
		    
		    // Create PULL Socket
		    pullSocket = context.socket(ZMQ.PULL);
		    pullSocket.connect("tcp://localhost:5556");
		   
		    subscriber = context.socket(ZMQ.SUB);
		    subscriber.connect("tcp://localhost:5557");
		    
		    previousCandles = new ArrayList<Candle>();
		    
		    
		    RSIs = new double[3];
			EMAs = new double[3][6];
			MACDs = new double[3];
		    
	}
	
	
	@Override
	public void run()
	{
		
		String data = remote_send(reqSocket, get_data);
		
		/*
		String H = remote_send(reqSocket, get_dataH);
		String L = remote_send(reqSocket, get_dataL);
	    String C = remote_send(reqSocket, get_dataC);
	    String T = remote_send(reqSocket, get_dataT);
	    String V = remote_send(reqSocket, get_dataV);
	    */
	    
//	    System.out.print(O);
	    
	//    String[] Os = O.split("\\|");
		
		String[] candles = data.split("~");
		
	    
//	    System.out.println("Open 1000: " + Os[1000]);
		
		for (String candle : candles)
		{
			String[] ohlc = candle.split("\\|");
			Candle c = new Candle(Long.parseLong(ohlc[0]),
					Double.parseDouble(ohlc[1]),
					Double.parseDouble(ohlc[2]),
					Double.parseDouble(ohlc[3]),
					Double.parseDouble(ohlc[4]),
					Integer.parseInt(ohlc[5]));
			previousCandles.add(c);
		}
		
	    
		// Get all RSI;
		// Get all EMA;
		// Get all MACD;
		
		
		MT4PullerReady = true;
		
		while (true){
			try {
				
				if (TimePeriodDecider.getEpochSec() % 60 == 0)
				{

					//every minute
					GetData.getShortTB().addCandle(getLastCandle(product,1));
					
					if (TimePeriodDecider.getEpochSec() % 300 == 0)
					{
						//every 5 minute
						GetData.getLongTB().addCandle(getLastCandle(product,5));
						
						if (TimePeriodDecider.getEpochSec() % 900 == 0)
						{
							//every 15 minute
							GetData.getM15TB().addCandle(getLastCandle(product,15));
							
						}
					}
					
				}
				
				
				
				
				
				//get ticker
				remote_pull(pullSocket);
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	// Function to send commands to ZeroMQ MT4 EA
		String remote_send(ZMQ.Socket socket, byte[] data)
		{ 
		    try {
		        socket.send(data);
		        byte[] msg = socket.recv(0);
		       return new String(msg);
		    }
		    catch(Exception e)
		    {
		    	return "Waiting for PUSH from MetaTrader 4";
		    }
		}  
		
		
		// Function to retrieve data from ZeroMQ MT4 EA
		void remote_pull(ZMQ.Socket socket)
		{ 
		    try {
		        byte[] msg = socket.recv(ZMQ.NOBLOCK);
		        
		        String[] bidAsk = new String(msg).split("\\|");
		        
		        currentBid = Double.parseDouble(bidAsk[0]);
		        currentAsk = Double.parseDouble(bidAsk[1]);
		        
		        Global.setCurrentBid(currentBid);
		        Global.setCurrentBid(currentAsk);
		        
//		        System.out.println(new String(msg));
		    }catch (Exception e)
		    {
//		    	System.out.println("Waiting for PUSH from MetaTrader 4");
		    	
		    }
		    
		  
		}
		
		Candle getLastCandle(String product, int timeFrame)
		{
			String request = "TI|CANDLE|" + product + "|" + timeFrame;
			//byte[] get_candle = "TI|CANDLE|GBPUSD|5".getBytes();
			
			String candle = remote_send(reqSocket, request.getBytes());
			String[] ohlc = candle.split("\\|");
			
			long time = Long.parseLong(ohlc[0]);
			double open = Double.parseDouble(ohlc[1]);
			double high = Double.parseDouble(ohlc[2]);
			double low = Double.parseDouble(ohlc[3]);
			double close = Double.parseDouble(ohlc[4]);
			int volume = Integer.parseInt(ohlc[5]);
			
			return new Candle(time,open,high,low,close,volume);
			
		}
		
		public static double getRSI(int timeFrame)
		{
			
			double rsi = 0;
			
			switch(timeFrame)
			{
			case 1: rsi = RSIs[0];
				break;
			case 5: rsi = RSIs[1];
				break;
			case 15: rsi = RSIs[2];
				break;
			}
			
			return rsi;
						
		}
	
}
