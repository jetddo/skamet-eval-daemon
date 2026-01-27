package kama.daemon.main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import kama.daemon.proc.DaemonProcess;

public class DaemonMain {
	
	private static Map<String, String> getReqMap(String[] args) {
		
		String[] _args = new String[args.length-1];
		
		for(int i=0 ; i<args.length-1 ; i++) {
			_args[i] = args[i+1];
		}
		
		if(_args.length < 4) {
			return null;
		}
		
		String[] orders = new String[(int)(_args.length/2)];
		String[] params = new String[(int)(_args.length/2)];
		
		Map<String, String> reqMap = new HashMap<String, String>();
		
		for(int i=0 ; i<(int)(_args.length/2) ; i++) {
			reqMap.put( _args[i*2], _args[i*2+1]);
		}
		
		return reqMap;
	}
	
	public static void main(String[] args) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Date start_date = new Date(System.currentTimeMillis());

		System.out.println(sdf.format(start_date) + " -> START");

		if (args.length == 0) {

			System.out.println("Error : DaemonMain.main -> args.length is 0");
			System.exit(1);
		}

		String procName = "kama.daemon.proc." + args[0].trim() + "_Process";

		DaemonProcess proc = null;

		try {

			proc = (DaemonProcess) Class.forName(procName).newInstance();

		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoClassDefFoundError e) {

			System.out.println("Error : DaemonMain.main -> " + e);
			System.exit(1);
		}

		Configurations configs = new Configurations();

		try {

			String confPath = DaemonMain.class.getClassLoader().getResource("").getPath();

			Configuration config = configs.properties(new File(confPath.replaceAll("bin", "conf") + "config.properties"));

			Map<String, String> reqMap = getReqMap(args);
			
			if (proc != null) {
				proc.process(config, reqMap);
			}

		} catch (ConfigurationException e) {
			System.out.println("Error : DaemonMain.main -> " + e);
		}

		Date end_date = new Date(System.currentTimeMillis());

		System.out.println(sdf.format(end_date) + " -> END ( Elapsed Time : " + (float)(end_date.getTime() - start_date.getTime()) / 1000 + " sec )");
	}
}