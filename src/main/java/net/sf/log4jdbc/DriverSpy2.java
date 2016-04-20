package net.sf.log4jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * A JDBC driver which is a facade that delegates to one or more real underlying
 * JDBC drivers. The driver will spy on any other JDBC driver that is loaded,
 * simply by prepending <code>jdbc:log4</code> to the normal jdbc driver URL
 * used by any other JDBC driver. The driver, by default, also loads several
 * well known drivers at class load time, so that this driver can be
 * "dropped in" to any Java program that uses these drivers without making any
 * code changes.
 * <p/>
 * The well known driver classes that are loaded are:
 * <p/>
 * <p/>
 * <code>
 * <ul>
 * <li>oracle.jdbc.driver.OracleDriver</li>
 * <li>com.sybase.jdbc2.jdbc.SybDriver</li>
 * <li>net.sourceforge.jtds.jdbc.Driver</li>
 * <li>com.microsoft.jdbc.sqlserver.SQLServerDriver</li>
 * <li>com.microsoft.sqlserver.jdbc.SQLServerDriver</li>
 * <li>weblogic.jdbc.sqlserver.SQLServerDriver</li>
 * <li>com.informix.jdbc.IfxDriver</li>
 * <li>org.apache.derby.jdbc.ClientDriver</li>
 * <li>org.apache.derby.jdbc.EmbeddedDriver</li>
 * <li>com.mysql.jdbc.Driver</li>
 * <li>org.postgresql.Driver</li>
 * <li>org.hsqldb.jdbcDriver</li>
 * <li>org.h2.Driver</li>
 * </ul>
 * </code>
 * <p/>
 * <p/>
 * Additional drivers can be set via a property: <b>log4jdbc.drivers</b> This
 * can be either a single driver class name or a list of comma separated driver
 * class names.
 * <p/>
 * The autoloading behavior can be disabled by setting a property:
 * <b>log4jdbc.auto.load.popular.drivers</b> to false. If that is done, then the
 * only drivers that log4jdbc will attempt to load are the ones specified in
 * <b>log4jdbc.drivers</b>.
 * <p/>
 * If any of the above driver classes cannot be loaded, the driver continues on
 * without failing.
 * <p/>
 * Note that the <code>getMajorVersion</code>, <code>getMinorVersion</code> and
 * <code>jdbcCompliant</code> method calls attempt to delegate to the last
 * underlying driver requested through any other call that accepts a JDBC URL.
 * <p/>
 * This can cause unexpected behavior in certain circumstances. For example, if
 * one of these 3 methods is called before any underlying driver has been
 * established, then they will return default values that might not be correct
 * in all situations. Similarly, if this spy driver is used to spy on more than
 * one underlying driver concurrently, the values returned by these 3 method
 * calls may change depending on what the last underlying driver used was at the
 * time. This will not usually be a problem, since the driver is retrieved by
 * it's URL from the DriverManager in the first place (thus establishing an
 * underlying real driver), and in most applications their is only one database.
 * 
 * @author gigdata@semaifour.com
 */
public class DriverSpy2 extends DriverSpy {
	
	static {
		/*
		 * De-register all currently registered drivers so DriverSpy can register
		 * itself as the first driver in the list.
		 */
		try {
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			for (Object o : Collections.list(drivers)) {
				Driver d = (Driver) o;
				DriverManager.deregisterDriver(d);
			}
		} catch (SQLException e) {
			log.debug("ERROR!, de-registering drivers; " + e.toString());
		}

		InputStream propStream = DriverSpy.class.getResourceAsStream("/log4jdbc.properties");

		Properties props = new Properties(System.getProperties());
		if (propStream != null) {
			try {
				props.load(propStream);
			} catch (IOException e) {
				log.debug("ERROR!  io exception loading " + "log4jdbc.properties from classpath: " + e.getMessage());
			} finally {
				try {
					propStream.close();
				} catch (IOException e) {
					log.debug("ERROR!  io exception closing property file stream: " + e.getMessage());
				}
			}
			log.debug("  log4jdbc.properties loaded from classpath");
		} else {
			log.debug("  log4jdbc.properties not found on classpath");
		}

		// look for additional driver specified in properties
		DebugStackPrefix = getStringOption(props, "log4jdbc.debug.stack.prefix");
		TraceFromApplication = DebugStackPrefix != null;

		Long thresh = getLongOption(props, "log4jdbc.sqltiming.warn.threshold");
		SqlTimingWarnThresholdEnabled = (thresh != null);
		if (SqlTimingWarnThresholdEnabled) {
			SqlTimingWarnThresholdMsec = thresh.longValue();
		}

		thresh = getLongOption(props, "log4jdbc.sqltiming.error.threshold");
		SqlTimingErrorThresholdEnabled = (thresh != null);
		if (SqlTimingErrorThresholdEnabled) {
			SqlTimingErrorThresholdMsec = thresh.longValue();
		}

		DumpBooleanAsTrueFalse = getBooleanOption(props, "log4jdbc.dump.booleanastruefalse", false);

		DumpSqlMaxLineLength = getLongOption(props, "log4jdbc.dump.sql.maxlinelength", 90L).intValue();

		DumpFullDebugStackTrace = getBooleanOption(props, "log4jdbc.dump.fulldebugstacktrace", false);

		StatementUsageWarn = getBooleanOption(props, "log4jdbc.statement.warn", false);

		DumpSqlSelect = getBooleanOption(props, "log4jdbc.dump.sql.select", true);
		DumpSqlInsert = getBooleanOption(props, "log4jdbc.dump.sql.insert", true);
		DumpSqlUpdate = getBooleanOption(props, "log4jdbc.dump.sql.update", true);
		DumpSqlDelete = getBooleanOption(props, "log4jdbc.dump.sql.delete", true);
		DumpSqlCreate = getBooleanOption(props, "log4jdbc.dump.sql.create", true);

		DumpSqlFilteringOn = !(DumpSqlSelect && DumpSqlInsert && DumpSqlUpdate && DumpSqlDelete && DumpSqlCreate);

		DumpSqlAddSemicolon = getBooleanOption(props, "log4jdbc.dump.sql.addsemicolon", false);

		AutoLoadPopularDrivers = getBooleanOption(props, "log4jdbc.auto.load.popular.drivers", true);

		TrimSql = getBooleanOption(props, "log4jdbc.trim.sql", true);
		TrimSqlLines = getBooleanOption(props, "log4jdbc.trim.sql.lines", false);
		if (TrimSqlLines && TrimSql) {
			log.debug("NOTE, log4jdbc.trim.sql setting ignored because " + "log4jdbc.trim.sql.lines is enabled.");
		}

		TrimExtraBlankLinesInSql = getBooleanOption(props, "log4jdbc.trim.sql.extrablanklines", true);

		SuppressGetGeneratedKeysException = getBooleanOption(props, "log4jdbc.suppress.generated.keys.exception",
				false);

		// The Set of drivers that the log4jdbc driver will preload at
		// instantiation
		// time. The driver can spy on any driver type, it's just a little bit
		// easier to configure log4jdbc if it's one of these types!

		Set subDrivers = new TreeSet();
		// System.out.println("AutoloadPopularDrivers: " + AutoLoadPopularDrivers);
		if (AutoLoadPopularDrivers) {
			subDrivers.add("oracle.jdbc.driver.OracleDriver");
			subDrivers.add("oracle.jdbc.OracleDriver");
			subDrivers.add("com.sybase.jdbc2.jdbc.SybDriver");
			subDrivers.add("net.sourceforge.jtds.jdbc.Driver");

			// MS driver for Sql Server 2000
			subDrivers.add("com.microsoft.jdbc.sqlserver.SQLServerDriver");

			// MS driver for Sql Server 2005
			subDrivers.add("com.microsoft.sqlserver.jdbc.SQLServerDriver");

			subDrivers.add("weblogic.jdbc.sqlserver.SQLServerDriver");
			subDrivers.add("com.informix.jdbc.IfxDriver");
			subDrivers.add("org.apache.derby.jdbc.ClientDriver");
			subDrivers.add("org.apache.derby.jdbc.EmbeddedDriver");
			subDrivers.add("com.mysql.jdbc.Driver");
			subDrivers.add("org.postgresql.Driver");
			subDrivers.add("org.hsqldb.jdbcDriver");
			subDrivers.add("org.h2.Driver");
		}

		// look for additional driver specified in properties
		String moreDrivers = getStringOption(props, "log4jdbc.drivers");

		if (moreDrivers != null) {
			String[] moreDriversArr = moreDrivers.split(",");

			for (int i = 0; i < moreDriversArr.length; i++) {
				subDrivers.add(moreDriversArr[i]);
				log.debug("    will look for specific driver " + moreDriversArr[i]);
			}
		}

		try {
			DriverManager.registerDriver(new DriverSpy2());
			log.debug("Driver  registration success :" + DriverSpy2.class.getCanonicalName());
		} catch (SQLException s) {
			// this exception should never be thrown, JDBC just defines it
			// for completeness
			throw (RuntimeException) new RuntimeException("could not register log4jdbc driver!").initCause(s);
		}

		// instantiate all the supported drivers and remove
		// those not found
		String driverClass;
		for (Iterator i = subDrivers.iterator(); i.hasNext();) {
			driverClass = (String) i.next();
			try {
				Class c = Class.forName(driverClass);
				DriverManager.registerDriver((Driver) c.newInstance());
				log.debug("Driver  registration success :" + driverClass);
			} catch (Throwable c) {
				log.debug("Driver  registration failure :" + driverClass);
				i.remove();
			}
		}

		if (subDrivers.size() == 0) {
			log.debug("WARNING!  " + "log4jdbc couldn't find any underlying jdbc drivers.");
		}

		SqlServerRdbmsSpecifics sqlServer = new SqlServerRdbmsSpecifics();
		OracleRdbmsSpecifics oracle = new OracleRdbmsSpecifics();
		MySqlRdbmsSpecifics mySql = new MySqlRdbmsSpecifics();

		/** create lookup Map for specific rdbms formatters */
		rdbmsSpecifics = new HashMap();
		rdbmsSpecifics.put("oracle.jdbc.driver.OracleDriver", oracle);
		rdbmsSpecifics.put("oracle.jdbc.OracleDriver", oracle);
		rdbmsSpecifics.put("net.sourceforge.jtds.jdbc.Driver", sqlServer);
		rdbmsSpecifics.put("com.microsoft.jdbc.sqlserver.SQLServerDriver", sqlServer);
		rdbmsSpecifics.put("weblogic.jdbc.sqlserver.SQLServerDriver", sqlServer);
		rdbmsSpecifics.put("com.mysql.jdbc.Driver", mySql);
	}

	/**
	 * Default constructor.
	 */
	public DriverSpy2() {
	}


	/**
	 * Get a Connection to the database from the underlying driver that this
	 * DriverSpy is spying on. If logging is not enabled, an actual Connection
	 * to the database returned. If logging is enabled, a ConnectionSpy object
	 * which wraps the real Connection is returned.
	 * 
	 * @param url
	 *            JDBC connection URL .
	 * @param info
	 *            a list of arbitrary string tag/value pairs as connection
	 *            arguments. Normally at least a "user" and "password" property
	 *            should be included.
	 * 
	 * @return a <code>Connection</code> object that represents a connection to
	 *         the URL.
	 * 
	 * @throws SQLException
	 *             if a database access error occurs
	 */
	public Connection connect(String url, Properties info) throws SQLException {
		Driver d = getUnderlyingDriver(url);
		if (d == null) {
			return null;
		}
		
		lastUnderlyingDriverRequested = d;
		
		Connection c = d.connect(url, info);
		if (c == null) {
			throw new SQLException("invalid or unknown driver url: " + url);
		}
		if (log.isJdbcLoggingEnabled()) {
			ConnectionSpy cspy = new ConnectionSpy(c);
			RdbmsSpecifics r = null;
			String dclass = d.getClass().getName();
			if (dclass != null && dclass.length() > 0) {
				r = (RdbmsSpecifics) rdbmsSpecifics.get(dclass);
			}

			if (r == null) {
				r = defaultRdbmsSpecifics;
			}
			cspy.setRdbmsSpecifics(r);
			return cspy;
		} else {
			return c;
		}
	}

	/**
	 * Given a jdbc URL, find the underlying real driver
	 * that accepts the URL.
	 * 
	 * @param url JDBC connection URL.
	 * 
	 * @return Underlying driver for the given URL.
	 * 
	 * @throws SQLException if a database access error occurs.
	 */
	@Override
	protected Driver getUnderlyingDriver(String url) throws SQLException
	{
		Enumeration e = DriverManager.getDrivers();
		Driver d;
		while (e.hasMoreElements())
		{
			d = (Driver) e.nextElement();

			if (d.acceptsURL(url))
			{
				return d;
			}
		}
		return null;
	}

}
