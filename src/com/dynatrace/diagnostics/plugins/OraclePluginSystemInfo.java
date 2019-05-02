/*Copyright (c) 2008-2018, DYNATRACE LLC
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the dynaTrace software nor the names of its contributors
      may be used to endorse or promote products derived from this software without
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
DAMAGE.
*/

package com.dynatrace.diagnostics.plugins;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.logging.Level;

import oracle.jdbc.pool.OracleDataSource;


import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import com.dynatrace.diagnostics.pdk.Status.StatusCode;

import com.dynatrace.diagnostics.plugins.domain.WaitMetrics;
import com.dynatrace.diagnostics.plugins.domain.ObjectStatus;

import com.dynatrace.diagnostics.plugins.utils.HelperUtils;

public class OraclePluginSystemInfo {
	
	static public final String DEFAULT_ENCODING = System.getProperty("file.encoding","UTF-8");
	

	private static String CONFIG_DB_HOST_NAME = "hostName";
    private static String CONFIG_DB_NAME = "dbName";
	private static String CONFIG_ORACLE_CON_TYPE = "oracleConType";

    private static String CONFIG_DB_USERNAME = "dbUsername";
    private static String CONFIG_DB_PASSWORD = "dbPassword";
	private static String CONFIG_RAC_NODEID = "nodeId";
    private static String CONFIG_PORT = "dbPort";
    private static String CONFIG_DB_SECURITY = "security";
    private static String CONFIG_DB_COMPLETESTRING = "completestring";
	
    private String SGA_METRIC_GROUP = "Oracle SGA";
    private String SESSION_METRIC_GROUP = "Oracle Sessions";
    private String SYSTEM_METRIC_GROUP = "Oracle System";
    private String SQL_METRIC_GROUP = "Oracle SQL";
    private String SQL_SPLIT_NAME = "Sql Name";
	private String WAIT_EVENT_SPLIT_NAME = "Wait Event Name";
    private String LOCKS_METRIC_GROUP = "Oracle Locks";
    private String LOCKS_SPLIT_NAME = "Lock Name";
    
    public String FREE_BUFFER_WAIT = "Free Buffer Waits";
    public String WRITE_COMPLETE_WAIT = "Write Complete Waits";
    public String BUFFER_BUSY_WAIT = "Buffer Busy Waits";
    public String DB_BLOCK_CHANGE = "DB Block Changes";
    public String DB_BLOCK_GETS = "DB Block Gets";
    public String CONSISTENT_GETS = "Consistent Gets";
    public String PHYSICAL_READS = "Physical Reads";
    public String PHYSICAL_WRITES = "Physical Writes";
    public String BUFFER_CACHE_HIT_RATIO = "Buffer Cache Hit Ratio";
    public String EXEC_NO_PARSE_RATIO = "Execution Without Parse Ratio";
    public String MEMORY_SORT_RATIO = "Memory Sort Ratio";
    public String SQL_AREA_GET_RATIO = "SQL Area Get Ratio";
    
    static final String[] ORACLE_SQL_METRICS = {"Executions", "Elapsed Time", "Average Elapsed Time", 
    	"CPU Time", "Average CPU Time", "Disk Reads", "Direct Writes", "Buffer Gets", "Rows Processed", "Parse Calls", 
    	"First Load Time", "Last Load Time", "Child Number"};
    static final String[] ORACLE_LOCKS_METRICS = {"LockMode", "Status", "LastDdl"};
	

	static final String[] ORACLE_WAIT_NAME_METRICS = {"Wait Count", "Sum Wait in ms"};


    public String CONNECTION_TIME = "Connection Time";

	public String WAIT_CPU = "Waits of class CPU in last min";
	public String WAIT_OTHER = "Waits of class OTHER in last min";
	public String WAIT_APPLICATION = "Waits of class APPLICATION in last min";
	public String WAIT_CONFIGURATION = "Waits of class Configuration in last min";
	public String WAIT_ADMINISTRATIVE = "Waits of class Administrative in last min";
	public String WAIT_CONCURRENCY = "Waits of class Concurrency in last min";
	public String WAIT_COMMIT = "Waits of class Commit in last min";
	public String WAIT_NETWORK = "Waits of class Network in last min";
	public String WAIT_USERIO = "Waits of class User I/O in last min";
	public String WAIT_SYSTEMIO = "Waits of class System I/O in last min";
	public String WAIT_SCHEDULER = "Waits of class Scheduler in last min";
	public String WAIT_CLUSTER = "Waits of class Cluster in last min";
	public String AVG_ACT_SESSIONS="Avg act sessions last min";
	public String OPEN_CURSOR_CURRENT="Open Cursor Current";
	public String SESSION_CACHED_CURSOR="Session Cached Cursor";
	public String COUNT_INACT_REDOLOG_GROUPS="Count of inactive Redolog Groups";
	public String STORAGE_LATENCY_USER="Storage Latency User I/O in ms";
	public String STORAGE_LATENCY_SYSTEM="Storage Latency System I/O in ms";
	public String STORAGE_LATENCY_COMMIT="Storage Latency Commit I/O in ms";
    public String SESSIONS_MAX = "Maximum Concurrent User Sessions";
    public String SESSIONS_CURRENT = "Current Concurrent User Sessions";
    public String SESSIONS_HIGHWATER = "Highest Concurrent User Sessions";
    public String USERS_MAX = "Maximum Named Users";
	public String LOCKS = "Count of Locks";
    private java.sql.Connection con = null;

    private String urloracle = "jdbc:oracle:thin";

    private  String host;
    private  String dbName;
	private  String oracleConType;

    private  String userName;
	private  long nodeid;
    private  String password;
    private  String port;
	private  String security;
	private  String completestring;
	
	private java.util.Properties properties = new java.util.Properties();	

    public String connectionUrl = "";

    private static final Logger log = Logger.getLogger(OraclePluginSystemInfo.class.getName());
    
    	
	public static final String SGA_STATEMENT = "select FREE_BUFFER_WAIT, WRITE_COMPLETE_WAIT, BUFFER_BUSY_WAIT, DB_BLOCK_CHANGE, DB_BLOCK_GETS, CONSISTENT_GETS, PHYSICAL_READS, PHYSICAL_WRITES from v$buffer_pool_statistics";
	public static final String BUFFER_RATIO_BUFFER_CACHE_STATEMENT = "SELECT ROUND ( (congets.VALUE + dbgets.VALUE - physreads.VALUE)  * 100    / (congets.VALUE + dbgets.VALUE),  2   ) VALUE  FROM v$sysstat congets, v$sysstat dbgets, v$sysstat physreads  WHERE congets.NAME = 'consistent gets'  AND dbgets.NAME = 'db block gets'  AND physreads.NAME = 'physical reads'  ";
	public static final String BUFFER_RATIO_EXEC_NOPARSE_STATEMENT = "SELECT DECODE (SIGN (ROUND ( (ec.VALUE - pc.VALUE)  * 100  / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  ),  -1, 0,  ROUND ( (ec.VALUE - pc.VALUE)  * 100    / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  )  VALUE FROM v$sysstat ec, v$sysstat pc  WHERE ec.NAME = 'execute count'  AND pc.NAME IN ('parse count', 'parse count (total)')  ";
	public static final String BUFFER_RATIO_MEMORY_SORT_STATEMENT = "SELECT ROUND ( ms.VALUE  / DECODE ((ds.VALUE + ms.VALUE), 0, 1, (ds.VALUE + ms.VALUE))  * 100,    2  ) VALUE FROM v$sysstat ds, v$sysstat ms  WHERE ms.NAME = 'sorts (memory)' AND ds.NAME = 'sorts (disk)'  ";
	public static final String BUFFER_RATIO_SQLAREA_STATEMENT = "SELECT ROUND (gethitratio * 100, 2) VALUE FROM v$librarycache  WHERE namespace = 'SQL AREA'";
	public static final String SESSION_STATEMENT = "select SESSIONS_MAX, SESSIONS_CURRENT, SESSIONS_HIGHWATER, USERS_MAX from v$license";
	public static final String AVG_ACT_SESSIONS_STATEMENT1="select round(sum(AAS),3) AS AVG_ACT_SESS_NODE from (select (m.time_waited/m.INTSIZE_CSEC) AAS from gv$waitclassmetric m, gv$system_wait_class n where m.wait_class_id=n.wait_class_id and n.wait_class != 'Idle' and m.INST_ID = ";
	public static final String AVG_ACT_SESSIONS_STATEMENT2=" union select (value/100) AAS from gv$sysmetric where metric_name='CPU Usage Per Sec' and group_id=2 and INST_ID = ";
	public static final String AVG_ACT_SESSIONS_STATEMENT3=") ";
	public static final String WAITS_LAST_MIN_STATEMENT1="WITH main_query AS (select  n.wait_class#,n.wait_class, round(m.time_waited/m.INTSIZE_CSEC,3) AAS from gv$waitclassmetric  m, gv$system_wait_class n where m.wait_class_id=n.wait_class_id and n.wait_class != 'Idle' and m.INST_ID = ";
	public static final String WAITS_LAST_MIN_STATEMENT2=" union select  -1,'CPU', round(value/100,3) AAS from gv$sysmetric where metric_name='CPU Usage Per Sec' and group_id=2 and INST_ID = ";
	public static final String WAITS_LAST_MIN_STATEMENT3=") "
    + "SELECT  (SELECT y.AAS FROM main_query y WHERE y.wait_class# = -1 ) AS CPU"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 0 ) AS Other"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 1 ) AS Application"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 2 ) AS Configuration"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 3 ) AS Administrative"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 4 ) AS Concurrency"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 5 ) AS \"Commit\""
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 7 ) AS Network"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 8 ) AS \"User I/O\""
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 9 ) AS \"System I/O\""
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 10 ) AS Scheduler"
        + ",(SELECT y.AAS FROM main_query y WHERE y.wait_class# = 11 ) AS \"Cluster\""
     + " from DUAL";
	public static final String COUNT_INACT_REDOLOG_GROUPS_STATEMENT1="select COUNT('GROUP#') as COUNT_INACTIVE_GROUPS_NODE from V$LOG where THREAD# = ";
	public static final String COUNT_INACT_REDOLOG_GROUPS_STATEMENT2=" AND STATUS='INACTIVE'"; 
	public static final String STORAGE_LATENCY_STATEMENT1="WITH main_query AS (select wait_class, sum(m.wait_count)  cnt, 10*sum(m.time_waited) ms, nvl(round(10*(sum(m.time_waited))/nullif(sum(m.wait_count),0),3) ,0) avg_ms from gv$eventmetric m, gv$event_name n where m.inst_id=";
	public static final String STORAGE_LATENCY_STATEMENT2=" and m.event_id=n.event_id and wait_class in ('System I/O', 'User I/O', 'Commit') and m.wait_count > 0 group by wait_class)SELECT (SELECT y.avg_ms FROM main_query y WHERE y.wait_class = 'User I/O' ) AS \"User I/O [ms]\" ,(SELECT y.avg_ms FROM main_query y WHERE y.wait_class = 'System I/O' ) AS \"System I/O [ms]\",(SELECT y.avg_ms FROM main_query y WHERE y.wait_class = 'Commit' ) AS \"Commit I/O [ms]\" from DUAL";
	public static final String WAIT_EVENT_NAME_CLASS_OTHER_STATEMENT_PART1="select n.name, wait_class,"
                        + " sum(m.wait_count)  wait_count,"
                        + " 10*sum(m.time_waited) sum_wait_in_ms,"
                        + " nvl(round(10*(sum(m.time_waited))/nullif(sum(m.wait_count),0),3) ,0) avg_wait_in_ms from gv$eventmetric m, gv$event_name n"
						+ " where m.event_id=n.event_id and wait_class != 'Idle' and m.wait_count > 0 and m.inst_id=";
	public static final String WAIT_EVENT_NAME_CLASS_OTHER_STATEMENT_PART2 = " group by n.name, wait_class";
	public static final String CHECK_SID = "select sql_id from top_sql_fulltext where sql_id = ?";

	public static final String LINE_SEPARATOR = "\n";

	public static final String OPEN_CURSOR_CURRENT_STATEMENT = "select max(sum(s.value)) as openCursorCurrent from V$STATNAME n, V$SESSTAT s where n.name in ('opened cursors current', 'session cursor cache count') and s.statistic# = n.statistic# group by s.sid";
	public static final String SESSION_CACHED_CURSOR_STATEMENT = "select max(s.value) as sessionCachedCursor from V$STATNAME n, V$SESSTAT s	where n.name = 'session cursor cache count' and s.statistic# = n.statistic#";
	public static final String LOCK_STATEMENT = "select count(1) as countOfLocks from V$LOCK";


   
    private String getConnectionUrl(String url, String host, String port, String dbName, String security, String completestring) {
    	log.finer("Inside getConnectionUrl method ...");

			log.finer("oracleConType is: " + oracleConType);
			if(oracleConType.equals("SID")) {
				// SID
				log.finer("getConnectionUrl method: connection string is " + url + ":" + "//@" + host + ":" + port + ":" + dbName + ";");
				return url + ":" + "//@" + host + ":" + port + ":" + dbName;
			}
		
			// ServiceName
			if(oracleConType.equals("servicename")) {
				log.finer("getConnectionUrl method: connection string is " + url + ":" + "@//" + host + ":" + port + "/" + dbName + ";");
				return url + ":" + "@//" + host + ":" + port + "/" + dbName;
			}
			// CompleteString
			if(oracleConType.equals("connectionstring")) {
				log.finer("getConnectionUrl method: connection string is " + completestring);
				return completestring;
			}
			// SecurityString
			else {
			log.finer("getConnectionUrl method: connection string is: " + url + " +:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=" + host + ")(PORT=" + port + "))(CONNECT_DATA=(SERVICE_NAME=" + dbName + "))( //security=(" + security +" + ))");
				return url + ":@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=" + host + ")(PORT=" + port + "))(CONNECT_DATA=(SERVICE_NAME=" + dbName + "))( security=(" + security +"))";      			

			} 
 }

    

    
    public Status setup(PluginEnvironment env) throws Exception {

		
        log.finer("Inside setup method ...");

        //get configuration
        host = env.getConfigString(CONFIG_DB_HOST_NAME);
        dbName = env.getConfigString(CONFIG_DB_NAME);
        oracleConType = env.getConfigString(CONFIG_ORACLE_CON_TYPE);
        userName = env.getConfigString(CONFIG_DB_USERNAME);
        port = env.getConfigString(CONFIG_PORT);
        password = env.getConfigPassword(CONFIG_DB_PASSWORD);
		security = env.getConfigString(CONFIG_DB_SECURITY);	
		completestring = env.getConfigString(CONFIG_DB_COMPLETESTRING);
		nodeid = env.getConfigLong(CONFIG_RAC_NODEID);
        Status stat;
		
        connectionUrl = getConnectionUrl(urloracle, host, port, dbName, security, completestring);

		properties.setProperty("user", userName);
		properties.setProperty("password", password);

	    // get connection to the monitored database
        try {
            log.info("setup method: Connecting to Oracle ...");
			log.info("setup method: Connection string is ... " + connectionUrl);
            log.info("setup method: Opening database connection ...");
            Class.forName("oracle.jdbc.driver.OracleDriver");
			
 		    con = DriverManager.getConnection(connectionUrl, properties); 
	
            stat = new Status();
        } catch (ClassNotFoundException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        	return getErrorStatus(e);
         } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            return getErrorStatus(e);
        } finally {
        	// do nothing here
        }
	   

    stat = new Status();
    return stat;
    }
    
        
     
    private Status getErrorStatus(Exception e) {
    	Status stat = new Status();
    	stat.setStatusCode(Status.StatusCode.ErrorTargetService);
        stat.setShortMessage(e.getMessage());
        stat.setMessage(e.getMessage());
        stat.setException(e);
        
        return stat;
    }
    
    public Status execute(PluginEnvironment env)  throws Exception {	
		//reconnect, if database connection was lost
    	if (con == null || con.isClosed()) {
    		this.setup(env);
    	}
        Status stat = new Status();

		log.finer("Inside execute method ...");
		// Oracle RDBMS metrics
		try {
			populateSGAInfo((MonitorEnvironment)env);
			populateSystemInfo((MonitorEnvironment)env);
			populateSessionInfo((MonitorEnvironment)env);
			stat = new Status();
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			stat = getErrorStatus(e);


		}

    return stat;
  }
    
    

  public void populateSGAInfo(MonitorEnvironment env) throws SQLException {

        ResultSet sgaResult = null;
        Collection<MonitorMeasure> measures;
        Statement st = null;
        ResultSet ratioResult = null;
        
        log.finer("Inside populateSGAInfo method ...");

        try {
        	st = con.createStatement();
			
	if (((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, FREE_BUFFER_WAIT)) != null)	
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, WRITE_COMPLETE_WAIT)) != null) 		
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_BUSY_WAIT)) != null)
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_CHANGE)) != null)
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_GETS)) != null)
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, CONSISTENT_GETS)) != null)
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_READS)) != null)
		||	((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_WRITES)) != null)
		)
	{		
			
            sgaResult = st.executeQuery(SGA_STATEMENT);
            while (sgaResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, FREE_BUFFER_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating FREE_BUFFER_WAIT ... ");
                        measure.setValue(sgaResult.getDouble("FREE_BUFFER_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, WRITE_COMPLETE_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating WRITE_COMPLETE_WAIT ...");
                        measure.setValue(sgaResult.getDouble("WRITE_COMPLETE_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_BUSY_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating BUFFER_BUSY_WAIT ...");
                        measure.setValue(sgaResult.getDouble("BUFFER_BUSY_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_CHANGE)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating DB_BLOCK_CHANGE ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_CHANGE"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating DB_BLOCK_GETS ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, CONSISTENT_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating CONSISTENT_GETS ...");
                        measure.setValue(sgaResult.getDouble("CONSISTENT_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_READS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating PHYSICAL_READS ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_READS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_WRITES)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating PHYSICAL_WRITES ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_WRITES"));
                    }
                }
            }
	}

			if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_CACHE_HIT_RATIO)) != null) {
				ratioResult = st.executeQuery(BUFFER_RATIO_BUFFER_CACHE_STATEMENT);
				while (ratioResult.next()) {
						for (MonitorMeasure measure : measures) {
							log.finer("populateSGAInfo method: Populating BUFFER_CACHE_HIT_RATIO ...");
							measure.setValue(ratioResult.getDouble(1));
						}
					}
            }
			
            if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, EXEC_NO_PARSE_RATIO)) != null) {
				ratioResult = st.executeQuery(BUFFER_RATIO_EXEC_NOPARSE_STATEMENT);
				while (ratioResult.next()) {
						for (MonitorMeasure measure : measures) {
							log.finer("populateSGAInfo method: Populating EXEC_NO_PARSE_RATIO ...");
							measure.setValue(ratioResult.getDouble(1));
						}
					}
            }

            if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, MEMORY_SORT_RATIO)) != null) {    
				ratioResult = st.executeQuery(BUFFER_RATIO_MEMORY_SORT_STATEMENT);
				while (ratioResult.next()) {
						for (MonitorMeasure measure : measures) {
							log.finer("populateSGAInfo method: Populating MEMORY_SORT_RATIO ...");
							measure.setValue(ratioResult.getDouble(1));
						}
					}
            }

            if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, SQL_AREA_GET_RATIO)) != null) {			
				ratioResult = st.executeQuery(BUFFER_RATIO_SQLAREA_STATEMENT);
				while (ratioResult.next()) {
						for (MonitorMeasure measure : measures) {
							log.finer("populateSGAInfo method: Populating SQL_AREA_GET_RATIO ...");
							measure.setValue(ratioResult.getDouble(1));
						}
					}
            }

        	} catch(SQLException e){
        		// ignore exception
        	}finally {				
            	if (st != null) st.close();
    		}

  }

	private Timestamp getCurrentTimestamp(Connection con) throws Exception {
		Timestamp timestamp = null;
		ResultSet rs = null;
		Statement st = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery("select sysdate from dual");
			timestamp = rs.getTimestamp(1);
			
		} catch (Exception e) {
			log.severe("getCurrentTimestamp method: '" + HelperUtils.getExceptionAsString(e) + "'");
			throw e;
		} finally {				
        	if (st != null) st.close();
		}
		return timestamp;

	}
  
  public void populateSystemInfo(MonitorEnvironment env) throws SQLException {

        Collection<MonitorMeasure> measures = null;
        double timeBefore = 0;
        double timeAfter = 0;
        double totalConnectionTime = 0;
        Connection timerCon = null;
		ResultSet systemResult = null;
        Statement st = null;
		
        log.finer("Inside populateSystemInfo method ...");
        

        try {
            log.finer("populateSystemInfo method: Connecting to Oracle ...");
            log.finer("populateSystemInfo method: Connection string is ... " + connectionUrl);
            log.finer("populateSystemInfo method: Opening database connection ...");

            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, CONNECTION_TIME)) != null) {
				        
				timeBefore = System.currentTimeMillis();
				timerCon = DriverManager.getConnection(connectionUrl, properties);	
			
				timeAfter = System.currentTimeMillis();
				timerCon.close();
				
				totalConnectionTime = timeAfter - timeBefore;
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating CONNECTION_TIME ... ");
                    measure.setValue(totalConnectionTime);
                }
            }

						
			st = con.createStatement();


            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, AVG_ACT_SESSIONS)) != null) {
				String sql = new StringBuilder(AVG_ACT_SESSIONS_STATEMENT1).append(nodeid).append(AVG_ACT_SESSIONS_STATEMENT2).append(nodeid).append(AVG_ACT_SESSIONS_STATEMENT3).toString();
				log.finer("populateSqlsActSessionInfo method: sql is '" + sql + "'");
				systemResult = st.executeQuery(sql);
				systemResult.next();
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating AVG_ACT_SESSIONS ... ");
                    measure.setValue(systemResult.getDouble("AVG_ACT_SESS_NODE"));
                }
            }
            

    		
			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, OPEN_CURSOR_CURRENT)) != null) {
	            systemResult = st.executeQuery(OPEN_CURSOR_CURRENT_STATEMENT);
				systemResult.next();         
				log.finer("populateSystemInfo method: Start Populating Open Cursors ... " + systemResult.getDouble("openCursorCurrent"));
	                for (MonitorMeasure measure : measures) {
	                    log.finer("populateSystemInfo method: Populating Open Cursors ... ");
				//		something strange happened ... so fix that 
						if (systemResult.getDouble("openCursorCurrent") > 10000000) {
							measure.setValue(0);
						}
						else {
							measure.setValue(systemResult.getDouble("openCursorCurrent"));
	                    }
	                }
	            }

			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, LOCKS)) != null) {				
				systemResult = st.executeQuery(LOCK_STATEMENT);
				systemResult.next();
					for (MonitorMeasure measure : measures) {
						log.finer("populateSystemInfo method: Populating Count of Locks ... ");
						measure.setValue(systemResult.getDouble("countOfLocks"));
					}
			}
            

			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, SESSION_CACHED_CURSOR)) != null) {				
				systemResult = st.executeQuery(SESSION_CACHED_CURSOR_STATEMENT);
				systemResult.next();
					for (MonitorMeasure measure : measures) {
						log.finer("populateSystemInfo method: Populating Session Cached Cursors ... ");
						measure.setValue(systemResult.getDouble("sessionCachedCursor"));
					}
            }
            
	
		
	if (((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CPU)) != null)		
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_OTHER)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_APPLICATION)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CONFIGURATION)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_ADMINISTRATIVE)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CONCURRENCY)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_COMMIT)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_NETWORK)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_USERIO)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_SYSTEMIO)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_SCHEDULER)) != null)
		 ||	((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CLUSTER)) != null)
		)
	{		

			String sqlwait = new StringBuilder(WAITS_LAST_MIN_STATEMENT1).append(nodeid).append(WAITS_LAST_MIN_STATEMENT2).append(nodeid).append(WAITS_LAST_MIN_STATEMENT3).toString();
        	log.finer("populateWaitClassInfo method: sql is '" + sqlwait + "'");		
            systemResult = st.executeQuery(sqlwait);
            systemResult.next();

            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CPU)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating CPU ... ");
                    measure.setValue(systemResult.getDouble("CPU"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_OTHER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_OTHER ... ");
                    measure.setValue(systemResult.getDouble("Other"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_APPLICATION)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_APPLICATION ... ");
                    measure.setValue(systemResult.getDouble("Application"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CONFIGURATION)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_CONFIGURATION ... ");
                    measure.setValue(systemResult.getDouble("Configuration"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_ADMINISTRATIVE)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_ADMINISTRATIVE ... ");
                    measure.setValue(systemResult.getDouble("Administrative"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CONCURRENCY)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_CONCURRENCY ... ");
                    measure.setValue(systemResult.getDouble("Concurrency"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_COMMIT)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_COMMIT ... ");
                    measure.setValue(systemResult.getDouble("Commit"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_NETWORK)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_NETWORK ... ");
                    measure.setValue(systemResult.getDouble("Network"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_USERIO)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_USERIO ... ");
                    measure.setValue(systemResult.getDouble("User I/O"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_SYSTEMIO)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_SYSTEMIO ... ");
                    measure.setValue(systemResult.getDouble("System I/O"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_SCHEDULER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_SCHEDULER ... ");
                    measure.setValue(systemResult.getDouble("Scheduler"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, WAIT_CLUSTER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating WAIT_CLUSTER ... ");
                    measure.setValue(systemResult.getDouble("Cluster"));
                }
            }
		} 

           if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, COUNT_INACT_REDOLOG_GROUPS)) != null) {			 
				String sqlredo = new StringBuilder(COUNT_INACT_REDOLOG_GROUPS_STATEMENT1).append(nodeid).append(COUNT_INACT_REDOLOG_GROUPS_STATEMENT2).toString();
				log.finer("populateSqlsCountInactiveRedolog: sql is '" + sqlredo + "'");
			
				systemResult = st.executeQuery(sqlredo);
				systemResult.next();
					for (MonitorMeasure measure : measures) {
						log.finer("populateSystemInfo method: Populating COUNT_INACT_REDOLOG_GROUPS ... ");
						measure.setValue(systemResult.getDouble("COUNT_INACTIVE_GROUPS_NODE"));
					}
            }
			

		if (((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_USER)) != null) 
			|| ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_SYSTEM)) != null) 
			|| ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_COMMIT)) != null)
			) 
		{
			
			String sqllatency = new StringBuilder(STORAGE_LATENCY_STATEMENT1).append(nodeid).append(STORAGE_LATENCY_STATEMENT2).toString();
        	log.finer("populateSqlsStorageLatency: sql is '" + sqllatency + "'");
			
            systemResult = st.executeQuery(sqllatency);
            systemResult.next();

            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_USER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating STORAGE_LATENCY_USER ... ");
                    measure.setValue(systemResult.getDouble("User I/O [ms]"));
                }
            }
			
			if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_SYSTEM)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating STORAGE_LATENCY_SYSTEM ... ");
                    measure.setValue(systemResult.getDouble("System I/O [ms]"));
                }
            }
			
			 if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, STORAGE_LATENCY_COMMIT)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating STORAGE_LATENCY_COMMIT ... ");
                    measure.setValue(systemResult.getDouble("Commit I/O [ms]"));
                }
            }
		}  



    if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, ORACLE_WAIT_NAME_METRICS[0])) != null) {

	  
    	String sqlwaits = new StringBuilder(WAIT_EVENT_NAME_CLASS_OTHER_STATEMENT_PART1).append(nodeid).append(WAIT_EVENT_NAME_CLASS_OTHER_STATEMENT_PART2).toString();
	    log.finer("populateWaitNameMeasures method: sql is '" + sqlwaits + "'");
	
    	systemResult = st.executeQuery(sqlwaits);
		List<WaitMetrics> otherwaits = getWaits(systemResult);
		log.finer("populateWaitNameMeasures method: wait other list is '" + Arrays.toString(otherwaits.toArray()));
		populateWaitNameMeasures(env, otherwaits);
	}
	  
	  		
        } catch (SQLException e) {
        	log.severe("populateSystemInfo method: " + HelperUtils.getExceptionAsString(e));
            
        } finally {				
        	if (st != null) st.close();
		}
		
  }

  public void populateSessionInfo(MonitorEnvironment env) throws SQLException {

        ResultSet sessionResult = null;
        Collection<MonitorMeasure> measures;
        Statement st = null;


        log.finer("Inside populateSessionInfo method ...");

        try {
        	st = con.createStatement();

		if (((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_MAX)) != null)
			|| ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_CURRENT)) != null)
			|| ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_HIGHWATER)) != null)
			|| ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, USERS_MAX)) != null)) 
		{
            sessionResult = st.executeQuery(SESSION_STATEMENT);
            sessionResult.next();

            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_MAX ... ");
                    measure.setValue(sessionResult.getDouble("SESSIONS_MAX"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_CURRENT)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_CURRENT ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_CURRENT"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_HIGHWATER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_HIGHWATER ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_HIGHWATER"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, USERS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating USERS_MAX ...");
                    measure.setValue(sessionResult.getDouble("USERS_MAX"));
                }
            }
		}
        } catch (SQLException e) {
        	log.severe("populateSessionInfo method: " + HelperUtils.getExceptionAsString(e));
        } 
        finally {				
        	if (st != null) st.close();
		}
  }

  public void teardown(PluginEnvironment env) throws Exception {
        log.finer("teardown method: Exiting Oracle Monitor Plugin ... ");
        if (con != null)
        	con.close();
  }
    
        

  private List<WaitMetrics> getWaits(ResultSet rs) throws SQLException {
		List<WaitMetrics> list = new ArrayList<WaitMetrics>();
		while (rs.next()) {
			WaitMetrics wm = new WaitMetrics();
			wm.setEventName(rs.getString("NAME"));
			wm.setParentWaitClass(rs.getString("WAIT_CLASS"));
			wm.setWaitSumMeasure(rs.getDouble("SUM_WAIT_IN_MS"));
			wm.setWaitCountMeasure(rs.getDouble("WAIT_COUNT"));
			wm.setKey(rs.getString("WAIT_CLASS") + " " + rs.getString("NAME"));
			list.add(wm);
		}
		return list;
	}

  

  

  private void populateWaitNameMeasures(MonitorEnvironment env, List<WaitMetrics> waits) {
	  log.finer("Inside of populateWaitNameMeasures method...");
	  
	  for (int i = 0; i < ORACLE_WAIT_NAME_METRICS.length; i++) {
		  log.finer("populateWaitNameMeasures method: metric # " + i + ", metric name is '" + ORACLE_WAIT_NAME_METRICS[i] + "'");
		  for (WaitMetrics wait : waits) {
			  log.finer("populateWaitNameMeasures method: wait's key is '" + wait.getKey() + "'");
			  Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, ORACLE_WAIT_NAME_METRICS[i]);
				for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
					MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, WAIT_EVENT_SPLIT_NAME, wait.getKey());
					switch (i) {
					case 0:
						// Wait count
						dynamicMeasure.setValue(wait.getWaitCountMeasure());
						break;
					case 1:
						// Sum Wait in ms
						dynamicMeasure.setValue(wait.getWaitSumMeasure());
						break;
					default:
						log.severe("populateWaitNameMeasures method: index " + i + " is unknown. Index skipped");
					}		
				}
		  }
	  }
  }

  }
