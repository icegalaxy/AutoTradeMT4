package net.icegalaxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

//Use the OPEN Line

public class XMLWatcher implements Runnable
{

	public static List<Stair> stairs = new CopyOnWriteArrayList<Stair>();


	static MyFile Stair = new MyFile("\\MT4\\stair.csv");
	
	static MyFile csvLog;
	
	public static ArrayList<MyFile> files = new ArrayList<MyFile>();
	
	public static double stair = 0;

	private int secCounter;

	public XMLWatcher()
	{

		files.add(Stair);

	}

	public void run()
	{

		// readStairs();
		try
		{
			resetStairs();
		} catch (Exception e1)
		{
			e1.printStackTrace();
			sleep(30000);
		}


		RuleSkyStair ss = new RuleSkyStair(true);
		RuleSkyStairNano na = new RuleSkyStairNano(true);
//		RuleSkyStair5Min pa = new RuleSkyStair5Min(true);
		RuleSkyStair1Min st1 = new RuleSkyStair1Min(true);
//		RuleSkyStair1Min2 st1v2 = new RuleSkyStair1Min2(true);
		RuleSkyStair5Min st5 = new RuleSkyStair5Min(true);
		RulePriceAction pa = new RulePriceAction(true);
		// RuleBreakOut breakOut = new RuleBreakOut(true);
//		Thread s = new Thread(sar);
//		s.start();
//		Thread r = new Thread(rr);
//		r.start();
//		Thread i = new Thread(ibt);
//		i.start();
//		Thread ran = new Thread(range);
//		ran.start();
//		Thread e = new Thread(m5ema);
//		e.start();
		// Thread b = new Thread(breakOut);
		// b.start();
		Thread ts = new Thread(ss);
		ts.start();
//		Thread p = new Thread(pa);
//		p.start();
		Thread nano = new Thread(na);
		nano.start();
		Thread tSt1 = new Thread(st1);
		tSt1.start();
//		Thread tSt1v2 = new Thread(st1v2);
//		tSt1v2.start();
		Thread tSt5 = new Thread(st5);
		tSt5.start();
		Thread sPa = new Thread(pa);
		sPa.start();

		while (Global.isRunning())
		{

			if (GetData.getTimeInt() > 91420 && Global.getOpen() == 0)
			{
				setOpenPrice();
				Global.addLog("Open: " + Global.getOpen());
			}

			if (secCounter >= 10)
			{
				secCounter = 0;


				if (Stair.isFileModified())
				{
					try{
						readStairs();
					}catch (Exception z)
					{
						z.printStackTrace();
						sleep(30000);
					}
				}

			}

			secCounter++;
			sleep(1000);
		}
	}

	public static void resetStairs() throws InterruptedException
	{

		try{
			readStairs();
		}catch (Exception z)
		{
			z.printStackTrace();
			Thread.sleep(30000);
		}

		if (stairs.size() <= 2)
		{
			Global.addLog("No stairs!!");
			return;
		}

		for (int s = 0; s < stairs.size(); s++)
		{
//			if (s < 2) //default value of EMAs should be false to avoid buying before checkingEMA
//			{
//				stairs.get(s).buying = false;
//				stairs.get(s).selling = false;
//				stairs.get(s).refHigh = 0;
//				stairs.get(s).refLow = 99999;
//				stairs.get(s).reActivateTime = 0;
//				stairs.get(s).shutdown = false;
//			} else
//			{
				stairs.get(s).buying = true;
				stairs.get(s).selling = true;
				stairs.get(s).refHigh = 0;
				stairs.get(s).refLow = 99999;
				stairs.get(s).reActivateTime = 0;
				stairs.get(s).shutdown = false;
//			}
		}
		Global.updateCSV();
	}

	private static void readStairs()
	{
		Scanner sc = null;
		try
		{
			sc = new Scanner(new File(Stair.pathName));
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		sc.useDelimiter("\r\n");

		ArrayList<String> lines = new ArrayList<String>();

		sc.next();

		while (sc.hasNext())
			lines.add(sc.next());

		sc.close();

		List<Stair> tempStairs = new CopyOnWriteArrayList<Stair>(); // avoid
																	// modifying
																	// the
																	// original
																	// list at
																	// this
																	// stage.

		for (int i = 0; i < lines.size(); i++)
		{
			Scanner sc2 = new Scanner(lines.get(i));
			sc2.useDelimiter(",");

			Stair st = new Stair();

			st.lineType = sc2.next();
			st.value = sc2.nextDouble();
			st.cutLoss = sc2.nextDouble();
			st.buying = sc2.nextBoolean();
			st.refLow = sc2.nextDouble();
			st.selling = sc2.nextBoolean();
			st.refHigh = sc2.nextDouble();
			st.tolerance = sc2.nextDouble();
			st.reActivateTime = sc2.nextInt();
			st.shutdown = sc2.nextBoolean();

			sc2.close();

			// Global.addLog("Stair: " + st.lineType + ", value: " + st.value);

			tempStairs.add(st);

		}

		stairs = tempStairs;

	}

//	private void setEMA()
//	{
//		ema.findElementOfToday();
//		// ema.findOHLC();
//
//		M5EMA50 = Boolean.parseBoolean(ema.getValueOfNode("M5EMA50"));
//		M5EMA250 = Boolean.parseBoolean(ema.getValueOfNode("M5EMA250"));
//		EMAbuying = Boolean.parseBoolean(ema.getValueOfNode("buying"));
//		EMAselling = Boolean.parseBoolean(ema.getValueOfNode("selling"));
//		EMAstair = Double.parseDouble(ema.getValueOfNode("stair"));
//		EMAstopEarn = Double.parseDouble(ema.getValueOfNode("stopEarn"));
//
//		Global.addLog("--------------------");
//		Global.addLog("EMA50: " + M5EMA50);
//		Global.addLog("EMA250: " + M5EMA250);
//		Global.addLog("StopEarn: " + EMAstopEarn);
//		Global.addLog("--------------------");
//	}

//	private boolean isFHIModified(String filePath)
//	{
//
//		if (FHIDataModifiedTime == new File(filePath).lastModified() / 60000)
//			return false;
//		else
//		{
//			FHIDataModifiedTime = new File(filePath).lastModified() / 60000;
//			Global.addLog("OHLC XML file updated");
//			return true;
//		}
//
//	}
//
//	private boolean isStairModified(String filePath)
//	{
//
//		if (stairModifiedTime == new File(filePath).lastModified() / 60000)
//			return false;
//		else
//		{
//			stairModifiedTime = new File(filePath).lastModified() / 60000;
//			Global.addLog("Stair.csv updated");
//			return true;
//		}
//
//	}
//
//	private boolean isIntraDayModified(String filePath)
//	{
//
//		if (intraDayModifiedTime == new File(filePath).lastModified() / 60000)
//			return false;
//		else
//		{
//			intraDayModifiedTime = new File(filePath).lastModified() / 60000;
//			Global.addLog("IntraDay XML file updated");
//			return true;
//		}
//
//	}
//
//	private boolean isEMAModified(String filePath)
//	{
//
//		if (EMAModifiedTime == new File(filePath).lastModified() / 60000)
//			return false;
//		else
//		{
//			EMAModifiedTime = new File(filePath).lastModified() / 60000;
//			Global.addLog("EMA XML file updated");
//			return true;
//		}
//
//	}

	private void setOpenPrice()
	{

		double openPrice = 0;

		SPApi.setOpenPrice();

		openPrice = Global.getOpen();

		if (openPrice == 0)
		{
			Global.addLog("Open = 0");
			
			SPApi.unSubscribePrice();
			
			sleep(10000);
			
			SPApi.subscribePrice();
			
			sleep(10000);

			if (TimePeriodDecider.nightOpened || TimePeriodDecider.dayClosed)
			{
				return;
			}
			if (GetData.getTimeInt() > 91500)
			{
				Global.addLog("Fail to set open b4 91500, try again later");
			}

//			setOpenPrice();
		}

//		ohlc.updateNode("open", String.valueOf(openPrice));

		// wait for open price to add them together
//		open.position = Global.getOpen();
		// pHigh.position = Global.getpHigh();
		// pLow.position = Global.getpLow();
		// pClose.position = Global.getpClose();
		//
		// mySupport.position = Global.getKkSupport();
		// myResist.position = Global.getKkResist();

		// return openPrice;

	}

//	public static void updateIntraDayXML(String node, String value)
//	{
//		intraDay.updateNode(node, value);
//		Global.addLog("Updated Node: " + node + ", value: " + value);
//	}
//
//	public static void updateEMAXML(String node, String value)
//	{
//		ema.updateNode(node, value);
//		Global.addLog("Updated Node: " + node + ", value: " + value);
//	}

	private void sleep(int miniSecond)
	{
		try
		{
			Thread.sleep(miniSecond);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}

//	private void setOHLC()
//	{
//
//		ohlc = new XMLReader("Today", OHLC.pathName);
//
//		try
//		{
//			ibtRise = Boolean.parseBoolean(ohlc.getValueOfNode("ibtRise"));
//			ibtDrop = Boolean.parseBoolean(ohlc.getValueOfNode("ibtDrop"));
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//		}
//
//		ohlcs = new OHLC[5];
//
//		ohlcs[0] = pHigh;
//		ohlcs[1] = pLow;
//		ohlcs[2] = pClose;
//		ohlcs[3] = mySupport;
//		ohlcs[4] = myResist;
//
//		for (int i = 0; i < 3; i++)
//		{
//			for (int j = 0; j < 5; j++)
//			{
//				switch (i)
//				{
//				case 0:
//					ohlcs[j].position = Double.parseDouble(ohlc.getValueOfChildNode(ohlcs[j].name, i));
//					break;
//				case 1:
//					ohlcs[j].stopEarn = Double.parseDouble(ohlc.getValueOfChildNode(ohlcs[j].name, i));
//					break;
//				case 2:
//					ohlcs[j].cutLoss = Double.parseDouble(ohlc.getValueOfChildNode(ohlcs[j].name, i));
//					break;
//				}
//
//			}
//
//		}
//
//		// XMLReader ohlc = new XMLReader(Global.getToday());
//		Global.setpHigh(ohlcs[0].position);
//		Global.setpLow(ohlcs[1].position);
//		Global.setpClose(ohlcs[2].position);
//		// Global.setpClose(ohlc.getpClose());
//		// Global.setpFluc(ohlc.getpFluc());
//		//
//		// Global.setKkResist(ohlc.getKkResist());
//		// Global.setKkSupport(ohlc.getKkSupport());
//
//		// if (pHigh.position != 0)
//		// {
//		Global.addLog("-------------------------------------");
//		Global.addLog("P.High: " + Global.getpHigh());
//		Global.addLog("P.Low: " + Global.getpLow());
//		Global.addLog("P.Close: " + Global.getpClose());
//		Global.addLog("IBT Rise: " + ibtRise);
//		Global.addLog("IBT Drop: " + ibtDrop);
//		Global.addLog("-------------------------------------");
//		// }
//
//	}

}
