import BulkCmIRPSystem.*;
import BulkCmIRPConstDefs.*;
import CommonIRPConstDefs.*;
import AlarmIRPSystem.*;
import AlarmIRPConstDefs.*;
import CosNotification.*;
import TimeBase.*;
import ManagedGenericIRPSystem.*;
import ManagedGenericIRPConstDefs.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;
import java.util.Properties;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import BasicCmIRPSystem.*;
import BasicCmNotifDefs.*;
import BasicCmNRMDefs.*;
import NotificationIRPConstDefs.*;
import NotificationIRPSystem.*;
import org.omg.PortableServer.*;
import java.lang.Thread;


public class TgppAdapter{
	
	private final long UNIX_OFFSET = 122192928000000000L;
	private org.omg.CORBA.ORB notificationOrb = null;
	private class notificationThread extends Thread {
		public void run() {
			//Code you want to get executed seperately then main thread.
			notificationOrb.run();
			}
	}


	public static void main(String args[]){
		//System.out.println(args[0] + " " + args[1]);
		//getAlarms(args[0],args[1],true);//"10.10.10.10","'SubNetwork=ONRM_ROOT_MO,SubNetwork=RNC01'~$f",true);
		//getAlarms("10.10.10.10","'SubNetwork=ONRM_ROOT_MO,SubNetwork=RNC01'~$f",true);
		//(new TgppAdapter()).getCmParam("10.10.10.10", true);
		(new TgppAdapter()).UploadConfW("10.10.10.10");
	}

	private BasicCmIrpOperations getBasicCm(String ipAddress){
		org.omg.CORBA.Object rootObj = null;
		NamingContextExt rootNameCon = null;
		org.omg.CORBA.Object corbaObjectBasicCmIRP= null;
		BasicCmIrpOperations _BasicCmIRP = null;
		
		try{
			int i = 0;
			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitRef", "NameService=corbaloc:iiop:1.2@" + ipAddress + ":49254/NameService");
			org.omg.CORBA.ORB orb = ORB.init(new String[0], props);
			// Resolve the CORBA Naming Service 
			rootObj = orb.resolve_initial_references("NameService");
			//rootObj = orb.string_to_object (iorBas);
			System.out.println("NS... " + rootObj);
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			System.out.println("NC... " + rootNameCon);
			String s = "com/ericsson/nms/agents/BasicCMIRP";
			//Locate IRP
			corbaObjectBasicCmIRP = rootNameCon.resolve(rootNameCon.to_name(s));
			System.out.println("IRP... " + corbaObjectBasicCmIRP);
			_BasicCmIRP = BasicCmIrpOperationsHelper.narrow(corbaObjectBasicCmIRP);
			System.out.println("Basic... " + _BasicCmIRP);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return _BasicCmIRP;
	}
	
	public String[] getCmParam(String ipAddress, boolean printConsole){
		String[] result = null;
		try{
			BasicCmIrpOperations _BasicCmIRP = null;
			String baseObj = "G3SubNetwork=ONRM_ROOT_MO,G3SubNetwork=BSC"; //,G3ManagedElement=BSC01";
			BasicCmIRPSystem.SearchControl sctrl = new BasicCmIRPSystem.SearchControl(BasicCmIRPSystem.ScopeType.BASE_ALL, 5, "", ResultContents.NAMES);
			String[] nsarr = {""};
			AttributeNameSetHolder nameset = new AttributeNameSetHolder(nsarr);
			_BasicCmIRP = getBasicCm(ipAddress);
			BasicCmIRPSystem.BasicCmInformationIterator iter = _BasicCmIRP.find_managed_objects(baseObj, sctrl, nameset.value);
			boolean haveMoreParams= false;
			short paramSize = 100;
		    List<BasicCmIRPSystem.Result> params = new ArrayList();
			BasicCmIRPSystem.ResultSetHolder felem = new BasicCmIRPSystem.ResultSetHolder();
			do{
				haveMoreParams = iter.next_basicCmInformations(paramSize, felem);
				params.addAll(Arrays.asList(felem.value));
				//System.out.println("Current alarm size:" + params.size());
			}while (haveMoreParams);
			result = new String[params.size()];
			int i = 0;
			for (BasicCmIRPSystem.Result param: params) {
				result[i] = paramPrint(param, printConsole);
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
		return result;
	}
	
	private String paramPrint(BasicCmIRPSystem.Result param, boolean print){
		
		String delimiter = ";";
		String result = param.mo;
		
		for (BasicCmIRPSystem.MOAttribute attr : param.attributes){
			if (attr.value.type().toString().contains("string")){
				result = result + delimiter + attr.name + "=" + attr.value.extract_string();
			}
		}
		
		if (print){
			System.out.println(result);
		}
		return result;

	}
	
	private BulkCmIRP getBulkCmWran(String ipAddress){
		org.omg.CORBA.Object rootObj = null;
		NamingContextExt rootNameCon = null;
		StructuredEvent[] filterableDataValues = null;
		org.omg.CORBA.Object corbaObjectBulkCmIRP= null;
		BulkCmIRP _BulkCmIRP = null;
		try{
			int i = 0;
			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitRef", "NameService=corbaloc:iiop:1.2@" + ipAddress + ":49254/NameService");
			org.omg.CORBA.ORB orb = ORB.init(new String[0], props);
			// Resolve the CORBA Naming Service 
			rootObj = orb.resolve_initial_references("NameService");
			System.out.println("NS... " + rootObj);
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			System.out.println("NC... " + rootNameCon);
			String s = "com/ericsson/nms/umts/ranos/BulkConfigService_R1";
			//Locate Alarm IRP
			corbaObjectBulkCmIRP = rootNameCon.resolve(rootNameCon.to_name(s));
			System.out.println("IRP... " + corbaObjectBulkCmIRP);
			_BulkCmIRP = BulkCmIRPHelper.narrow(corbaObjectBulkCmIRP);
			System.out.println("Bulk... " + _BulkCmIRP);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return _BulkCmIRP;
	}
	
	public void UploadConfW(String ipAddress){
		try{
			BulkCmIRP _bulkcm = null;
			String session = "sessw";
			String hostname = ipAddress;//java.net.InetAddress.getLocalHost().getHostName();
			String fileDest = "sftp://USER:PASS@" + hostname + "/var/opt/ericsson/oss_bulkdata/export/sinkirp.xml";
			String baseObj = "SubNetwork=ONRM_ROOT_MO_R,SubNetwork=RNC01";
			//String baseObj = "ftp://" + hostname + "/home/USER/baseobj.txt";
//			ba - Base All: This scope includes the starting point MO and all MOs below it. With this argument, any specified level argument is ignored.
//			bo - Base Only: This scope includes only the starting point MO. With this argument, any specified level argument is ignored.
//			bn - Base N: This scope only includes MOs that are exactly n levels below the starting point MO, n being the value specified in the level argument.
//			bs - Base Sub-Tree: This scope includes the starting point MO and all MOs below it down to the specified level from Sub-tree level 1.
//			level - An integer value representing the number of levels to be included in the scope.
			//BulkCmIRPConstDefs.SearchControl sctrl = new BulkCmIRPConstDefs.SearchControl(BulkCmIRPConstDefs.ScopeType.BaseAll, 5, "Both");
			BulkCmIRPConstDefs.SearchControl sctrl = new BulkCmIRPConstDefs.SearchControl(BulkCmIRPConstDefs.ScopeType.BaseAll, 1, "BCR_GsmRelation.xml");///opt/ericsson/nms_umts_bcg_meta/dat/customfilters/
			_bulkcm = getBulkCmWran(ipAddress);
			String[] sessids = _bulkcm.get_session_ids();
			for (String sessid : sessids){
				if (sessid.equals(session)){
					_bulkcm.end_session(session);
				}
			}
			_bulkcm.start_session(session);
			System.out.println(fileDest);
			//_bulkcm.set_configuration();//<--------------------------------------------
			_bulkcm.upload(session, fileDest, baseObj, sctrl);
/*			BulkCmIRPConstDefs.SessionState sess_status = null;
			BulkCmIRPConstDefs.SessionState sess_status_prev = null;
			org.omg.CORBA.StringHolder ErrInfo = null;
			do{
				sess_status_prev = sess_status;
				sess_status = _bulkcm.get_session_status(session, ErrInfo);
				System.out.println("Session status... " + sess_status);
			} while (sess_status != sess_status_prev);
			
java.lang.NullPointerException
        at BulkCmIRPSystem._BulkCmIRPStub.get_session_status(_BulkCmIRPStub.java:250)
        at TgppAdapter.UploadConfW(TgppAdapter.java:187)
        at TgppAdapter.main(TgppAdapter.java:46)
*/
			_bulkcm.end_session(session);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	private BulkCmIRP getBulkCmGeran(String ipAddress){
		org.omg.CORBA.Object rootObj = null;
		NamingContextExt rootNameCon = null;
		org.omg.CORBA.Object corbaObjectBulkCmIRP= null;
		BulkCmIRP _BulkCmIRP = null;
		try{
			int i = 0;
			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitRef", "NameService=corbaloc:iiop:1.2@" + ipAddress + ":49254/NameService");
			org.omg.CORBA.ORB orb = ORB.init(new String[0], props);
			// Resolve the CORBA Naming Service 
			rootObj = orb.resolve_initial_references("NameService");
			System.out.println("NS... " + rootObj);
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			System.out.println("NC... " + rootNameCon);
			String s = "com/ericsson/nms/agents/BulkCmIRP";
			//Locate IRP
			corbaObjectBulkCmIRP = rootNameCon.resolve(rootNameCon.to_name(s));
			System.out.println("IRP... " + corbaObjectBulkCmIRP);
			_BulkCmIRP = BulkCmIRPHelper.narrow(corbaObjectBulkCmIRP);
			System.out.println("Bulk... " + _BulkCmIRP);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return _BulkCmIRP;
	}
	
	public void UploadConfG(String ipAddress){
		try{
			BulkCmIRP _bulkcm = null;
			String session = "sessg";
			String hostname = ipAddress;//java.net.InetAddress.getLocalHost().getHostName();
			String fileDest = "ftp://" + hostname + "/var/opt/ericsson/blkcm/data/export/sinkirp.txt";
			String baseObj = "ftp://" + hostname + "/var/opt/ericsson/blkcm/data/params/baseobj.txt";
			//String logFile = "ftp://" + hostname + "/irplog.txt";
			BulkCmIRPConstDefs.SearchControl sctrl = new BulkCmIRPConstDefs.SearchControl(BulkCmIRPConstDefs.ScopeType.BaseOnly, 0, "");
			_bulkcm = getBulkCmGeran(ipAddress);
			String[] sessids = _bulkcm.get_session_ids();
			for (String sessid : sessids){
				if (sessid.equals(session)){
					_bulkcm.end_session(session);
				}
			}
			_bulkcm.start_session(session);
			System.out.println(fileDest);
			_bulkcm.upload(session, fileDest, baseObj, sctrl);
			//_bulkcm.get_session_log(logFile, session, true);
			//_bulkcm.end_session(session);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	private NotificationIRPSystem.NotificationIRPOperations attAlarmsNotif(String ipAddress){
		org.omg.CORBA.Object rootObj = null;
		NamingContextExt rootNameCon = null;
		org.omg.CORBA.Object corbaObjectNotif= null;
		NotificationIRPSystem.NotificationIRPOperations neOp = null;
		try{
			int i = 0;
			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitRef", "NameService=corbaloc:iiop:1.2@" + ipAddress + ":49254/NameService");
			notificationOrb = ORB.init(new String[0], props);
			// Resolve the CORBA Naming Service 
			rootObj = notificationOrb.resolve_initial_references("NameService");
			System.out.println("NS... " + rootObj);
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			System.out.println("NC... " + rootNameCon);
			String s = "com/ericsson/nms/cif/service/NMSNAConsumer";
			//Locate IRP
			corbaObjectNotif = rootNameCon.resolve(rootNameCon.to_name(s));
			System.out.println("IRP... " + corbaObjectNotif);
			neOp = NotificationIRPOperationsHelper.narrow(corbaObjectNotif);
			System.out.println("neOp... " + neOp);
			IRPManager irpMan = new IRPManager();
			POA poa = POAHelper.narrow(notificationOrb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();
			org.omg.CORBA.Object objNotiServer = poa.servant_to_reference(irpMan);
			//String manager_reference = orb.object_to_string(objNotiServer);
			//System.out.println("OBJ NOTI SERVER: " + objNotiServer);
			int time_tick = 15;
			String filter = "";
			String[] aCat = {"1f1"};
			String res = neOp.attach_push(objNotiServer, time_tick, aCat, filter);
			System.out.println("notification id... " + res);
			(new notificationThread()).start();
			//notificationOrb.run();
			Thread.sleep(120000);
			neOp.detach(objNotiServer, res);
			notificationOrb.shutdown(true);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return neOp;
	}
	
	
	
	public String[] getAlarms(String ipAddress, String filter, boolean printConsole){

		String[] result = null;

		try{
			int i = 0;
			org.omg.CORBA.Object rootObj = null;
			NamingContextExt rootNameCon = null;
			StructuredEvent[] filterableDataValues = null;
			org.omg.CORBA.Object corbaObjectAlarmIRP= null;
			AlarmIRPSystem.AlarmIRP alarmIRP = null;
			Properties props = new Properties();
			props.put("org.omg.CORBA.ORBInitRef", "NameService=corbaloc:iiop:1.2@" + ipAddress + ":49254/NameService");
			org.omg.CORBA.ORB orb = ORB.init(new String[0], props);
			// Resolve the CORBA Naming Service 
			rootObj = orb.resolve_initial_references("NameService");
			rootNameCon = NamingContextExtHelper.narrow(rootObj);
			String s = "com/ericsson/nms/fm_cirpagent/AlarmIRP";
			//Locate Alarm IRP
			corbaObjectAlarmIRP = rootNameCon.resolve(rootNameCon.to_name(s));
			alarmIRP = AlarmIRPSystem.AlarmIRPHelper.narrow(corbaObjectAlarmIRP);
			String alarmFilter = filter;//"'SubNetwork=ONRM_ROOT_MO,SubNetwork=RNC01'~$f";
			BooleanHolder flag = new BooleanHolder(false);  // false for iteration
			AlarmInformationIteratorHolder iter = new AlarmInformationIteratorHolder();
			alarmIRP.get_alarm_list(alarmFilter, flag, iter);
			EventBatchHolder alarmInformation = new EventBatchHolder();
			short alarmSize = 100;
		    List<StructuredEvent> alarms = new ArrayList();
			boolean haveMoreAlarms = false;
			do{
				haveMoreAlarms = iter.value.next_alarmInformations(alarmSize, alarmInformation);
				alarms.addAll(Arrays.asList(alarmInformation.value));
				//System.out.println("Current alarm size:" + alarms.size());
			}while (haveMoreAlarms);
			result = new String[alarms.size()];
			for (StructuredEvent alarm: alarms) {
				result[i] = alarmPrint(alarm, printConsole);
				i++;
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return result;
	}
	
	private String alarmPrint(StructuredEvent alarm, boolean print){
		
		String result = "";
		String delemiter = ";";
		UtcT timeValue = null;
		if (alarm.filterable_data != null) {
			for (Property filterableData: alarm.filterable_data) {
				String fieldName = filterableData.name;
				switch (fieldName){
					case NotificationIRPConstDefs.NV_ALARM_ID.value:
						result = result + filterableData.value.extract_string() + delemiter;
						break;
					case "f":
						result = result + filterableData.value.extract_string() + delemiter;
						break;
					case "e":
						result = result + filterableData.value.extract_string() + delemiter;
						break;
					case "c":
						timeValue = UtcTHelper.extract(filterableData.value);
						Date dt = new Date((timeValue.time - UNIX_OFFSET) / 10000);
						DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
						result = result + df.format(dt) + delemiter;
						break;
					case "i":
						result = result + filterableData.value.extract_string() + delemiter;
						break;
					case "j":
						result = result + filterableData.value.extract_string();
						break;
				}
				
				// System.out.println("data.name: " + fieldName);
				// if (filterableData.value != null && fieldName.equals("c")) {
					// //System.out.println(filterableData.value.type());
					// //System.out.println(filterableData.value.extract_string());//.type());
					// timeValue = UtcTHelper.extract(filterableData.value);
					// Date dt = new Date(timeValue.time);
					// System.out.println(dt);//.type());
					// //Property readable = filterableData.value;
					// //System.out.println("property.value: " + readable.value);
					// //filterableDataValues = AlarmInformationSeqHelper.extract(filterableData.value);
				// } else {
					// System.out.println("data.value.type: " + filterableData.value.type());
				// }/
			}
		}
		if (print){
			System.out.println(result);
		}
		return result;

	}

}


/*
less /var/opt/ericsson/blkcm/data/bulkcm.nameservice
10.10.10.10 49254 10.250.10.10
javac AlarmClient.java
java -Dcom.sun.CORBA.transport.ORBTCPReadTimeouts="100:180000:180000:20" -cp . AlarmClient
java -cp . AlarmClient
java -jar AlarmAdapter.jar 10.10.10.10 \'SubNetwork=ONRM_ROOT_MO,SubNetwork=RNC01\'~\$f
*/
//jar cvfm AlarmAdapter.jar manifest.txt AlarmClient.class -C *.class
/*  This block encapsulates string used in the name of the Name Value pair of the structured event.       
const string NV_NOTIFICATION_ID ="a";   
const string NV_CORRELATED_NOTIFICATIONS = "b";
const string NV_EVENT_TIME = "c";
const string NV_SYSTEM_DN = "d";
const string NV_MANAGED_OBJECT_CLASS = "e";
const string NV_MANAGED_OBJECT_INSTANCE = "f";
const string NV_PROBABLE_CAUSE = "g";
const string NV_PERCEIVED_SEVERITY = "h";
const string NV_SPECIFIC_PROBLEM = "i";
const string NV_ADDITIONAL_TEXT = "j";
const string NV_ALARM_ID = "k";
const string NV_ACK_USER_ID = "l";
const string NV_ACK_TIME = "m";
const string NV_ACK_SYSTEM_ID = "n";
const string NV_ACK_STATE = "o";
const string NV_BACKED_UP_STATUS = "p";
const string NV_BACK_UP_OBJECT = "q";
const string NV_THRESHOLD_INFO = "r";
const string NV_TREND_INDICATION = "s";
const string NV_STATE_CHANGE_DEFINITION = "t";
const string NV_MONITORED_ATTRIBUTES = "u";
const string NV_PROPOSED_REPAIR_ACTIONS = "v";
*/

/*fixed_header.event_type.name: x1 fixed_header.event_type.domain_name: 1f1
data.name: a
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@7b02881e =
unbounded string
data.name: k
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@1ebd319f =
unbounded string
data.name: f
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@3c0be339 =
unbounded string
data.name: e
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@15ca7889 =
unbounded string
data.name: c
Mon Jan 10 10:53:20 GMT+03:00 4351267
data.name: h
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@7a675056 =
short
data.name: g
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@d21a74c =
short
data.name: i
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@6e509ffa =
unbounded string
data.name: m
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@2898ac89 =
struct UtcT = {
  alias TimeT =  time;
  ulong  inacclo;
  ushort  inacchi;
  alias TdfT =  tdf;
}
data.name: l
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@683dbc2c =
unbounded string
data.name: o
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@68267da0 =
short
data.name: j
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@2638011 =
unbounded string
data.name: d
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@6ff29830 =
unbounded string
data.name: v
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@6a2b953e =
unbounded string
data.name: externalAlarmId
data.value.type: com.sun.corba.se.impl.corba.TypeCodeImpl@313b2ea6 =
unbounded string
*/