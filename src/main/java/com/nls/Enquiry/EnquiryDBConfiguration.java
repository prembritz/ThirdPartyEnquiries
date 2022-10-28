package com.nls.Enquiry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.PooledDataSource;
import com.mchange.v2.log.MLevel;

@ApplicationPath("/Enquiry")
@OpenAPIDefinition(info = @Info(title = "Enquiries API", version = "1.0.0"))
public class EnquiryDBConfiguration extends Application {
	// @ConfigProperties
	Config configProperties;

	private static DataSource cmDBPool;
	private static HashMap<String, String> ActualTableName;
	private static HashMap<String, String> ChannelGlobalParameters;
	//private static HashMap<String, String> GlobalParameters;
	private String ChannelDBSchema;

	public EnquiryDBConfiguration() {
		initializeCMDBPool();
		
		KRAEslipValidationEnquiry.setDBPool(cmDBPool);
		KRAEslipValidationEnquiry.SetSchemaNames(ChannelDBSchema);

		if (cmDBPool != null) {
			System.out.println("****************DB CONNECTION ESTABLISHED*******************");
		} else {
			System.out.println("****************DB CONNECTION NOT ESTABLISHED*******************8");
		}

	}

	@PreDestroy
	public void closePools() {
		try {
			DataSources.destroy(cmDBPool);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void initializeCMDBPool() {
		try {

			configProperties = ConfigProvider.getConfig();

			DriverManager.registerDriver((java.sql.Driver) Class
					.forName(configProperties.getValue("CMDBDriver", String.class)).newInstance());
			String dbURL = configProperties.getValue("CMDBDURL", String.class);
			ChannelDBSchema=configProperties.getValue("ChannelDBSchema", String.class);
			Properties dbProps = new Properties();
			dbProps.setProperty("user", configProperties.getValue("CMDBUser", String.class));
			dbProps.setProperty("password", configProperties.getValue("CMDBPassword", String.class));
			javax.sql.DataSource unpooled = DataSources.unpooledDataSource(dbURL, dbProps);
			HashMap<String, Object> overrides = new HashMap<String, Object>();
			overrides.put("maxPoolSize", configProperties.getValue("CMDBPoolSize", Integer.class));
			overrides.put("idleConnectionTestPeriod", 60);
			overrides.put("preferredTestQuery", configProperties.getValue("CMDBValidationQuery", String.class));
			overrides.put("testConnectionOnCheckout",
					configProperties.getValue("CMDBTestConnectionOnCheckout", Boolean.class));
					overrides.put("checkoutTimeout", configProperties.getValue("CMDBCheckoutTimeout", Integer.class));
			//overrides.put("checkoutTimeout", 5000);

		/*	System.out.println("CMDBDriver =" + configProperties.getValue("CMDBDriver", String.class));
			System.out.println("CMDBDURL =" + configProperties.getValue("CMDBDURL", String.class));
			System.out.println("CMDBUser =" + configProperties.getValue("CMDBUser", String.class));
			System.out.println("CMDBPassword =" + configProperties.getValue("CMDBPassword", String.class));
			System.out.println("CMDBPoolSize =" + configProperties.getValue("CMDBPoolSize", Integer.class));
			System.out
					.println("CMDBValidationQuery =" + configProperties.getValue("CMDBValidationQuery", String.class));
			System.out.println("CMDBTestConnectionOnCheckout ="
					+ configProperties.getValue("CMDBTestConnectionOnCheckout", String.class));*/

			cmDBPool = (PooledDataSource) DataSources.pooledDataSource(unpooled, overrides);

			Connection dbConnection = null;
			try {
				System.getProperties().put("com.mchange.v2.log.MLog", "com.mchange.v2.log.jdk14logging.Jdk14MLog");
				System.getProperties().put("com.mchange.v2.log.jdk14logging.Jdk14MLog.DEFAULT_CUTOFF_LEVEL",
						MLevel.INFO.toString());
				dbConnection = cmDBPool.getConnection();

				InitializeUnitIDconfigs(dbConnection);
			//	InitiailizeGlobalParameters(dbConnection);
				InitiailizeChannelGlobalParameters(dbConnection,ChannelDBSchema);
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (dbConnection != null)
					dbConnection.close();
			}

		} catch (Exception except) {
			throw new RuntimeException(except);
		}
	}

	public Set<Object> getSingletons() {
		return null;
	}

	public Set<Class<?>> getClasses() {
		return null;
	}

	private void InitializeUnitIDconfigs(Connection dbConnection) throws SQLException {
		ActualTableName = new HashMap<String, String>();

		PreparedStatement UnitCofigPS = dbConnection.prepareStatement(
				"select UNIT_ID || '-' || TABLE_REF " + " as TABLE_REFERENCE,TABLE_NAME from unitid$configs ");
		ResultSet UnitCofigRS = UnitCofigPS.executeQuery();
		while (UnitCofigRS.next()) {
			ActualTableName.put(UnitCofigRS.getString("TABLE_REFERENCE"), UnitCofigRS.getString("TABLE_NAME"));
		}
		UnitCofigRS.close();
		UnitCofigPS.close();

		System.out.println("Actual table is print ::::[" + ActualTableName + "]");
	}

	/*private void InitiailizeGlobalParameters(Connection dbConnection) throws SQLException {
		GlobalParameters = new LinkedHashMap<String, String>();

		PreparedStatement GlobalPs = dbConnection.prepareStatement("select * from global$parameters ");
		ResultSet GlobalRs = GlobalPs.executeQuery();
		while (GlobalRs.next()) {
			GlobalParameters.put(GlobalRs.getString("NAME"), GlobalRs.getString("VALUE"));
		}
		GlobalRs.close();
		GlobalPs.close();

		// return GlobalParameters;

	}*/
	
	private void InitiailizeChannelGlobalParameters(Connection dbConnection,String channelschema) throws SQLException {
		ChannelGlobalParameters = new LinkedHashMap<String, String>();

		PreparedStatement GlobalPs = dbConnection.prepareStatement("select * from "+channelschema+".global$parameters ");
		ResultSet GlobalRs = GlobalPs.executeQuery();
		while (GlobalRs.next()) {
			ChannelGlobalParameters.put(GlobalRs.getString("NAME"), GlobalRs.getString("VALUE"));
		}
		GlobalRs.close();
		GlobalPs.close();
		System.out.println("ChannelGlobalParameters ["+ChannelGlobalParameters+"]");

		// return GlobalParameters;

	}

}
