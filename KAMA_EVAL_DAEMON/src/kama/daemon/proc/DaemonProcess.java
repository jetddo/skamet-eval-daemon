package kama.daemon.proc;

import org.apache.commons.configuration2.Configuration;

public abstract class DaemonProcess {

	abstract public void process(Configuration config);


}