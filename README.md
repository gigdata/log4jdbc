#=======================================
# Introduction
#=======================================

- DriverSpy2 to register itself as the main driver that accepts connections from other JDBC drivers
- Users don't have to manually load the DriverSpy2 class and prepend jdbc:log4 to their DB URLs

#=======================================
# Usage
#=======================================

1. Include the log4jdbc.jar to the application's classpath (use 'mvn package' to build the jar from source code).

2. Set log4jdbc as the main JDBC driver using Java/JVM arguments:
    -Djdbc.drivers=net.sf.log4jdbc.DriverSpy2 

3. The following drivers are automatically monitored: UNLESS: -Dlog4jdbc.auto.load.popular.drivers=false
     * oracle.jdbc.driver.OracleDriver
     * com.sybase.jdbc2.jdbc.SybDriver
     * net.sourceforge.jtds.jdbc.Driver
     * com.microsoft.jdbc.sqlserver.SQLServerDriver
     * com.microsoft.sqlserver.jdbc.SQLServerDriver
     * weblogic.jdbc.sqlserver.SQLServerDriver
     * com.informix.jdbc.IfxDriver
     * org.apache.derby.jdbc.ClientDriver
     * org.apache.derby.jdbc.EmbeddedDriver
     * com.mysql.jdbc.Driver
     * org.postgresql.Driver
     * org.hsqldb.jdbcDriver
     * org.h2.Driver
     

To monitor specific drivers or drivers that are not automatically monitored, do append as below
-Dlog4jdbc.drivers=net.sf.log4jdbc.DriverSpy2[,<driverclass>...]

4. Include the slf4j-api.jar and the appropriate SLF4J binding as required by the application's main logging framework 
(i.e. slf4j-log4j12.jar, slf4j-jdk14.jar) in the classpath.

5. Include the appropriate log properties file (see sample log4j.properties file below).

#=======================================
# Sample log4j.properties file for log4j framework
#=======================================

log4j.rootLogger=ERROR
log4j.logger.com.sample.db.app=INFO,console

log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c] - %m%n

# turn on the internal log4j debugging flag if needed
log4j.debug=false

#========================================
# JDBC API layer call logging settings
#========================================

# Log all JDBC calls except for ResultSet calls
log4j.logger.jdbc.audit=INFO,jdbc
log4j.additivity.jdbc.audit=false

# Log only JDBC calls to ResultSet objects
log4j.logger.jdbc.resultset=INFO,jdbc
log4j.additivity.jdbc.resultset=false

# Log only the SQL that is executed.
log4j.logger.jdbc.sqlonly=INFO,sql
log4j.additivity.jdbc.sqlonly=false

# Log timing information about the SQL that is executed.
log4j.logger.jdbc.sqltiming=INFO,sqltiming,console
log4j.additivity.jdbc.sqltiming=false

# Log connection open/close events and connection number dump
log4j.logger.jdbc.connection=INFO,connection
log4j.additivity.jdbc.connection=false


#========================================
# JDBC API appender settings - optional 
#========================================

# appender only for sql logging
log4j.appender.sql=org.apache.log4j.FileAppender
log4j.appender.sql.File=/Library/Tomcat/logs/sql.log
log4j.appender.sql.Append=true
log4j.appender.sql.layout=org.apache.log4j.PatternLayout
log4j.appender.sql.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c] - %m%n

# appender only for sqltiming logging
log4j.appender.sqltiming=org.apache.log4j.FileAppender
log4j.appender.sqltiming.File=/Library/Tomcat/logs/sqltiming.log
log4j.appender.sqltiming.Append=true
log4j.appender.sqltiming.layout=org.apache.log4j.PatternLayout
log4j.appender.sqltiming.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c] - %m%n

# appender for all jdbc api call logging
log4j.appender.jdbc=org.apache.log4j.FileAppender
log4j.appender.jdbc.File=/Library/Tomcat/logs/jdbc.log
log4j.appender.jdbc.Append=true
log4j.appender.jdbc.layout=org.apache.log4j.PatternLayout
log4j.appender.jdbc.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c] - %m%n

# appender only for connection logging
log4j.appender.connection=org.apache.log4j.FileAppender
log4j.appender.connection.File=/Library/Tomcat/logs/connection.log
log4j.appender.connection.Append=true
log4j.appender.connection.layout=org.apache.log4j.PatternLayout
log4j.appender.connection.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss.SSS} %-5p [%c] - %m%n
