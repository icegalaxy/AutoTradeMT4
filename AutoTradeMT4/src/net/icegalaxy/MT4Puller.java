package net.icegalaxy;

import java.sql.Time;
import java.util.ArrayList;

import org.zeromq.ZMQ;

public class MT4Puller implements Runnable {

	ZMQ.Context context;
	private static ZMQ.Socket reqSocket;
	ZMQ.Socket pullSocket;
	ZMQ.Socket subscriber;

	static String product = "GBPUSD";

	// Sample Commands for ZeroMQ MT4 EA
//	byte[] eurusd_buy_order = "TRADE|OPEN|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
//	byte[] eurusd_sell_order = "TRADE|OPEN|1|EURUSD|0|50|50|Python-to-MT4".getBytes();
//	byte[] eurusd_closebuy_order = "TRADE|CLOSE|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
	// byte[] get_data = "DATA|GBPUSD|PERIOD_M5|D'2018.06.15 00:00:00'|D'2018.07.02
	// 00:00:00'".getBytes();
	byte[] get_data = "DATA|GBPUSD|1|0|10".getBytes();

	/*
	 * byte[] get_dataH = "DATA|GBPUSD|1|H|0|2000".getBytes(); byte[] get_dataL =
	 * "DATA|GBPUSD|1|L|0|2000".getBytes(); byte[] get_dataC =
	 * "DATA|GBPUSD|1|C|0|2000".getBytes(); byte[] get_dataT =
	 * "DATA|GBPUSD|1|T|0|2000".getBytes(); byte[] get_dataV =
	 * "DATA|GBPUSD|1|V|0|2000".getBytes();
	 */

	byte[] get_rates = "RATES|GBPUSD".getBytes();

	private double currentBid;
	private double currentAsk;

	public static ArrayList<Candle> previousCandles;
	public static boolean MT4PullerReady;

	public static double[] RSIs;
	public static double[][] EMAs;
	public static double[] MACDs;

	// public static ArrayList<Candle> currentCandle;

	public MT4Puller() {
		// Create ZMQ Context
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
	public void run() {

		String data = remote_send(reqSocket, get_data);

		/*
		 * String H = remote_send(reqSocket, get_dataH); String L =
		 * remote_send(reqSocket, get_dataL); String C = remote_send(reqSocket,
		 * get_dataC); String T = remote_send(reqSocket, get_dataT); String V =
		 * remote_send(reqSocket, get_dataV);
		 */

		// System.out.print(O);

		// String[] Os = O.split("\\|");

		String[] candles = data.split("~");

		// System.out.println("Open 1000: " + Os[1000]);

		for (String candle : candles) {
			String[] ohlc = candle.split("\\|");
			Candle c = new Candle(Long.parseLong(ohlc[0]), Double.parseDouble(ohlc[1]), Double.parseDouble(ohlc[2]),
					Double.parseDouble(ohlc[3]), Double.parseDouble(ohlc[4]), Integer.parseInt(ohlc[5]));
			previousCandles.add(c);
		}

		// Get all RSI;
		// Get all EMA;
		// Get all MACD;

		requestData(1);
		requestData(5);
		requestData(15);

		MT4PullerReady = true;

		while (true) {

			long epochSec = TimePeriodDecider.getEpochSec();

			if (epochSec % 60 == 0) {
				// every minute
				requestData(1);

				if (epochSec % 300 == 0) {
					// every 5 minute
					requestData(5);

					if (epochSec % 900 == 0) {
						// every 15 minute
						requestData(15);

					}
				}

			}

			try {
				// get ticker
				remote_pull(pullSocket);
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void requestData(int timeFrame) {

		int pos = -1;

		switch (timeFrame) {
		case 1:
			pos = 0;
			break;
		case 5:
			pos = 1;
			break;
		case 15:
			pos = 2;
			break;
		}

		GetData.getShortTB().addCandle(getLastCandle(product, timeFrame));
		RSIs[pos] = requestRSI(timeFrame, 14);
		for (int i = 0; i < GetData.EMAList.length; i++)
			EMAs[pos][i] = requestEMA(timeFrame, GetData.EMAList[i]);
	}

	// Function to send commands to ZeroMQ MT4 EA
	private static synchronized String remote_send(ZMQ.Socket socket, byte[] data) {
		try {
			socket.send(data);
			byte[] msg = socket.recv(0);
			return new String(msg);
		} catch (Exception e) {
			return "Waiting for PUSH from MetaTrader 4";
		}
	}

	// Function to retrieve data from ZeroMQ MT4 EA
	void remote_pull(ZMQ.Socket socket) {
		try {
			byte[] msg = socket.recv(ZMQ.NOBLOCK);

			String[] bidAsk = new String(msg).split("\\|");

			currentBid = Double.parseDouble(bidAsk[0]);
			currentAsk = Double.parseDouble(bidAsk[1]);

			Global.setCurrentBid(currentBid);
			Global.setCurrentBid(currentAsk);

			// System.out.println(new String(msg));
		} catch (Exception e) {
			// System.out.println("Waiting for PUSH from MetaTrader 4");

		}

	}

	Candle getLastCandle(String product, int timeFrame) {
		String request = "TI|CANDLE|" + product + "|" + timeFrame;
		// byte[] get_candle = "TI|CANDLE|GBPUSD|5".getBytes();

		String candle = remote_send(reqSocket, request.getBytes());
		String[] ohlc = candle.split("\\|");

		long time = Long.parseLong(ohlc[0]);
		double open = Double.parseDouble(ohlc[1]);
		double high = Double.parseDouble(ohlc[2]);
		double low = Double.parseDouble(ohlc[3]);
		double close = Double.parseDouble(ohlc[4]);
		int volume = Integer.parseInt(ohlc[5]);

		return new Candle(time, open, high, low, close, volume);

	}

	private double requestRSI(int timeFrame, int period) {
		String request = "TI|RSI|" + product + "|" + timeFrame + "|" + period;

		String rsiString = remote_send(reqSocket, request.getBytes());

		return Double.parseDouble(rsiString);

	}

	private double requestEMA(int timeFrame, int period) {
		String request = "TI|EMA|" + product + "|" + timeFrame + "|" + period;

		String rsiString = remote_send(reqSocket, request.getBytes());

		return Double.parseDouble(rsiString);

	}

	public static double getRSI(int timeFrame) {

		double rsi = 0;

		switch (timeFrame) {
		case 1:
			rsi = RSIs[0];
			break;
		case 5:
			rsi = RSIs[1];
			break;
		case 15:
			rsi = RSIs[2];
			break;
		}

		return rsi;

	}

	public static double getEMA(int timeFrame, int period) {

		int x = 0;
		int y = 0;

		switch (timeFrame) {
		case 1:
			x = 0;
			break;
		case 5:
			x = 1;
			break;
		case 15:
			x = 3;
			break;
		}

		switch (period) {
		case 5:
			y = 0;
			break;
		case 25:
			y = 1;
			break;
		case 50:
			y = 2;
			break;
		case 100:
			y = 3;
			break;
		case 250:
			y = 4;
			break;
		case 1200:
			y = 5;
			break;
		}

		return EMAs[x][y];
	}
	
	public static int addOrder(int action, double lots, double price)
	{
//		byte[] eurusd_buy_order = "TRADE|OPEN|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
		
		String order = "TRADE|OPEN|" + product + "|" + action + "|" + lots + "|" + price + "|" + 10 + "|0|0|";
		
		String ticket = remote_send(reqSocket, order.getBytes());
		
		return Integer.parseInt(ticket);
		
	}
	
	public static boolean closeOrder(int ticket, double lots, double price)
	{
		
		//"TRADE|CLOSE|0|EURUSD|0|50|50|Python-to-MT4".getBytes();
		String order = "TRADE|CLOSE|" + ticket + "|" + lots + "|" + price + "|" + 10;
		
		String status = remote_send(reqSocket, order.getBytes());
		
		return Boolean.parseBoolean(status);
		
		
	}

}
