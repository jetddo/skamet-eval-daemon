package kama.daemon.proc;

import java.util.Map;

import org.apache.commons.configuration2.Configuration;

public abstract class DaemonProcess {

	abstract public void process(Configuration config, Map<String, String> reqMap);


}