package com.laccan.senseApp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import net.tinyos.message.MoteIF;
import net.tinyos.packet.BuildSource;
import net.tinyos.packet.PhoenixSource;
import net.tinyos.util.PrintStreamMessenger;

import com.digi.xbee.api.DigiMeshDevice;
import com.digi.xbee.api.RemoteXBeeDevice;
import com.digi.xbee.api.XBeeNetwork;
import com.digi.xbee.api.exceptions.XBeeException;
import com.digi.xbee.api.utils.HexUtils;


class Sense implements MessageListener  {

	/***********Load native libraries *************/
	private static String nativeLibraryPath = 
			System.getProperty("user.dir") + "/sense_lib/native/Linux/x86_64-unknown-linux-gnu/";

	static {			
		System.load(nativeLibraryPath + "librxtxSerial.so");
		System.load(nativeLibraryPath + "libgetenv.so");
		System.load(nativeLibraryPath + "libtoscomm.so");
	}

	/**************** MICAz's constants ****************/

	private PhoenixSource phoenix;
	private MoteIF mif;
	private String[] environments = {"lab_15", "lab_16", "lab_17", 
			"prof_1", "prof_2", "prof_3",
			"prof_4", "prof_5", "non","reun"};
	private SimpleDateFormat dt;
	private String date;

	/**************** XBEE's constants ****************/
	
	//Replace with the serial port where your receiver module is connected.
	private static final String PORT = "/dev/ttyUSB2";
	//Replace with the baud rate of you receiver module.
	private static final int BAUD_RATE = 9600;

	private static final String DATA_TO_SEND = "1";
	private static final String DATA_TO_SEND1 = "0";
	private static final String REMOTE_NODE_IDENTIFIER = "env_16_tem01";

	//Indicate if the light is on or off
	private boolean lightsOn = false;

	// Examples of endpoints, cluster ID and profile ID.
	private static final int SOURCE_ENDPOINT = 0xA0;
	private static final int DESTINATION_ENDPOINT = 0xA1;
	private static final int CLUSTER_ID = 0x1554;
	private static final int PROFILE_ID = 0x1234;


	public Sense(final String source){
		phoenix = BuildSource.makePhoenix(source, PrintStreamMessenger.err);
		mif = new MoteIF(phoenix);
		mif.registerListener(new SenseMsg(),this);
		//		Example "2016-06-24T21:58:19.000Z"
		dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000Z");

	}

	public void messageReceived(int dest_addr, Message msg) {

		//Get current date
		date = dt.format(System.currentTimeMillis());
		//Display sensor data
		showData(msg);

		//insert data into database
//		saveData(msg);
	}
	

	private int[] calculateTaos(int VisibleLight,int InfraredLight) {
		final int CHORD_VAL[]={0,16,49,115,247,511,1039,2095};
		final int STEP_VAL[]={1,2,4,8,16,32,64,128};
		int chordVal,stepVal;
		int[] lightVal=new int[2];

		chordVal=(VisibleLight>>4) & 7;
		stepVal=VisibleLight & 15;
		lightVal[0]=CHORD_VAL [chordVal]+stepVal*STEP_VAL[chordVal];
		chordVal=(InfraredLight>>4)&7;
		stepVal=VisibleLight & 15;
		lightVal[1]=CHORD_VAL[chordVal]+stepVal*STEP_VAL[chordVal];
		return lightVal;
	}

	private double[] calculateSensirion(int Temperature,int Humidity){
		double [] converted = new double[2]; 

		converted[0]=-39.4+(0.01*(double)Temperature);
		converted[1]=(-2.0468+0.0367*(double)Humidity-0.0000015955*Math.pow((double)Humidity,(double )2))+(converted[0]-25)*(0.01+0.00008*(double)Humidity);

		return converted;
	}

	private void sendCommand(String command) {

		System.out.println("Turning lights " + command+'.');				
		DigiMeshDevice myDevice = new DigiMeshDevice(PORT, BAUD_RATE);
		//		byte[] dataToSend = DATA_TO_SEND.getBytes();
		byte[] dataToSend = command.getBytes();

		try {
			myDevice.open();
			// Obtain the remote XBee device from the XBee network.
			XBeeNetwork xbeeNetwork = myDevice.getNetwork();
			RemoteXBeeDevice remoteDevice = xbeeNetwork.discoverDevice(REMOTE_NODE_IDENTIFIER);
			if (remoteDevice == null) {
				System.out.println("Device: '" + REMOTE_NODE_IDENTIFIER + "' not found.");
				System.exit(1);
			}

			System.out.format("Sending explicit data asynchronously to %s [%s - %s - %s - %s] >> %s | %s... ", 
					remoteDevice.get64BitAddress(), 
					HexUtils.integerToHexString(SOURCE_ENDPOINT, 1), 
					HexUtils.integerToHexString(DESTINATION_ENDPOINT, 1), 
					HexUtils.integerToHexString(CLUSTER_ID, 2),
					HexUtils.integerToHexString(PROFILE_ID, 2), 
					HexUtils.prettyHexString(HexUtils.byteArrayToHexString(dataToSend)), 
					new String(dataToSend));

			myDevice.sendExplicitDataAsync(remoteDevice, SOURCE_ENDPOINT,DESTINATION_ENDPOINT, 
					CLUSTER_ID, PROFILE_ID, dataToSend);

			System.out.println("Success");

		} catch (XBeeException e) {
			System.out.println("Error");
			e.printStackTrace();
			System.exit(1);
		} finally {
			myDevice.close();
		}
	}

	private void showData(Message message) {
		if(message instanceof SenseMsg) {
			SenseMsg tempMessage = (SenseMsg) message;

			//Calibrate sensor values
			int[] taosCalcData = 
					calculateTaos(tempMessage.get_VisLight_data(),tempMessage.get_InfLight_data());

			double[] sensirionCalcData = 
					calculateSensirion(tempMessage.get_Temp_data(),tempMessage.get_Hum_data());

			int voltage = 
					(1223 * 1024)/tempMessage.get_Voltage_data();

			System.out.println("The measured results are:");
			System.out.println();
			System.out.println("Node:                   "+tempMessage.get_nodeid());
			System.out.printf("Sensirion temperature:  %.2f\n",sensirionCalcData[0]);
			System.out.printf("Sensirion humidity:     %.2f\n",sensirionCalcData[1]);
			System.out.println("Intersema temperature:  "+tempMessage.getElement_Intersema_data(0)/10);
			System.out.println("Intersema pressure:     "+tempMessage.getElement_Intersema_data(1)/10);
			System.out.println("Taos visible light:     "+taosCalcData[0]);
			System.out.println("Taos infrared light:    "+taosCalcData[1]);
			System.out.println("Accelerometer X axis:   "+tempMessage.get_AccelX_data());
			System.out.println("Accelerometer Y axis:   "+tempMessage.get_AccelY_data());
			System.out.println("Voltage:                "+voltage);
//			System.out.println("Environment id:\t\t"+environments[(tempMessage.get_nodeid() - 1)/ 5]);
			System.out.println("Environment id:\t\ttest");
			System.out.println("Country:                "+"Brazil");
			System.out.println("State:                	"+"Alagoas");
			System.out.println("City:                	"+"Maceio");
			System.out.println("Latitude:               "+"-9.555032");
			System.out.println("Longitude:              "+"-35.774708");
			System.out.println("date:\t\t\t" + date);
			System.out.println();
		}
	}
	
	private void connectAndSaveToDatabase(String databaseName, String data) {
		try {			

			//Set URL address
			//192.168.200.242
			URL url = new URL("http://192.168.200.242/api/v1/"+databaseName+'/');

			//Request connection
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();

			//Set request type
			conn.setRequestMethod("POST");
			//Set request properties
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setRequestProperty("Accept-Language", "pt-br");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "UTF-8");
			conn.setDoOutput(true);

			//Write data to server
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(data);
			wr.flush();

			//Close connection
			wr.close();

			int responseCode = conn.getResponseCode();					
			System.out.println("\nSending 'POST' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);


		} catch (MalformedURLException e) {
			System.out.println("A malformed URL exception has occurred"+ e.getMessage());
		} catch (IOException e) {
			System.out.println("Error:"+ e.getMessage());
		}
	}

	private void saveData(Message message) {
		if(message instanceof SenseMsg) {	
			//Get packet
			SenseMsg tempMessage = (SenseMsg) message;

			//Calibrate sensor values
			int[] taosCalcData = 
					calculateTaos(tempMessage.get_VisLight_data(),tempMessage.get_InfLight_data());

			/** ####################### TEMPORARY CODE ####################### **/

			
			if(environments[(tempMessage.get_nodeid() - 1)/ 5].compareTo("lab_16") == 0) {
				System.out.println("This is env 16!");
				//				Low environment luminosity
				//				send a "turn on" lights command
				//				to the Xbee network
				if(taosCalcData[0] <= 400 && !lightsOn) {
					sendCommand(DATA_TO_SEND);
					lightsOn = true;
					System.out.println("Lights: "+taosCalcData[0]+" Send on");
				}
				else if(taosCalcData[0] <= 400 && lightsOn) {
					lightsOn = true;
				}

				//Turn on lights
				else if(taosCalcData[0] > 400 && lightsOn) {
					sendCommand(DATA_TO_SEND1);
					System.out.println("Lights: "+taosCalcData[0]+" Send off");
					lightsOn = false;
				}
				else if(taosCalcData[0] > 400 && !lightsOn) {
					lightsOn = false;
				}
			}
			

			/** ########################################################################## **/


			double[] sensirionCalcData = 
					calculateSensirion(tempMessage.get_Temp_data(),tempMessage.get_Hum_data());

			int voltage = 
					(1223 * 1024)/tempMessage.get_Voltage_data();

			if(voltage >= 2100) {

				//Save sensor data to a formatted string					
				String data = String.format("nodeID=%s&sensirion_temp=%s&"
						+ "sensirion_hum=%s&intersema_temp=%s&"
						+ "intersema_press=%s&infrared_light=%s&"
						+ "light=%s&accel_x=%s&"
						+ "accel_y=%s&voltage=%s&"								
						+ "country=%s&state=%s&"
						+ "city=%s&latitude=%s&"
						+ "longitude=%s&env_id=%s&"
						+ "date=%s", tempMessage.get_nodeid(), sensirionCalcData[0],
						sensirionCalcData[1], tempMessage.getElement_Intersema_data(0)/10, 
						tempMessage.getElement_Intersema_data(1)/10, taosCalcData[1], 
						taosCalcData[0], tempMessage.get_AccelX_data(), 
						tempMessage.get_AccelY_data(), voltage,
						"Brazil", "Alagoas", "Maceió",
						"-9.555032", "-35.774708", environments[(tempMessage.get_nodeid() - 1)/ 5], date);
				
				//Save data to a remote server
				connectAndSaveToDatabase("sensors", data);
				connectAndSaveToDatabase("sensors0", data);
			}
			else {
				System.out.println("Voltage is too low, package rejected.\n");
			}
		}
		else {
			System.out.println("Invalid");
		}
	}
}
