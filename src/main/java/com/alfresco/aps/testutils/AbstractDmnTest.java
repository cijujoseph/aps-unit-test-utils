package com.alfresco.aps.testutils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.activiti.dmn.engine.DmnEngine;
import com.activiti.dmn.engine.DmnEngineConfiguration;
import com.activiti.dmn.engine.DmnRepositoryService;
import com.activiti.dmn.engine.DmnRuleService;
import com.activiti.dmn.engine.domain.entity.DmnDeployment;
import com.activiti.dmn.engine.test.ActivitiDmnRule;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public abstract class AbstractDmnTest {

	@Autowired
	protected ApplicationContext appContext;
	
	@Autowired
	protected Environment env;
	
	@Autowired
	protected DmnRepositoryService repositoryService;
	
	@Autowired
	protected DmnRuleService ruleService;
    
    @Autowired
    protected DmnEngine dmnEngine;
    
    @Autowired
    protected DmnEngineConfiguration dmnEngineConfiguration;
    
    @Autowired
    protected ActivitiDmnRule activitiDmnRule;
	
	protected static final String DMN_RESOURCE_PATH = "src/test/resources";
	
	protected static ArrayList<Long> deploymentList = new ArrayList<Long>();
	
	@Before
	public void before() throws Exception {

		//Create tables using liquibase
		ComboPooledDataSource ds = new ComboPooledDataSource();
		ds.setDriverClass(dmnEngineConfiguration.getJdbcDriver());
		ds.setJdbcUrl(dmnEngineConfiguration.getJdbcUrl());
		ds.setUser(dmnEngineConfiguration.getJdbcUsername());
		ds.setPassword(dmnEngineConfiguration.getJdbcPassword());

		DatabaseConnection connection = new JdbcConnection(ds.getConnection());
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);

		//Using a liquibase file in activiti-app-data jar file.
		Liquibase liquibase = new Liquibase("META-INF/liquibase/db-changelog-onpremise.xml",
				new ClassLoaderResourceAccessor(), database);
		liquibase.dropAll();
		liquibase.update("dmn");

		//Deploy the dmn files
		Iterator<File> it = FileUtils.iterateFiles(new File(DMN_RESOURCE_PATH), null, false);
		while (it.hasNext()) {
			String bpmnXml = ((File) it.next()).getPath();

			String extension = FilenameUtils.getExtension(bpmnXml);
			if (extension.equals("dmn")) {
				DmnDeployment dmnDeplyment = repositoryService.createDeployment()
						.addInputStream(bpmnXml, new FileInputStream(bpmnXml)).deploy();
				deploymentList.add(dmnDeplyment.getId());
			}
		}
	}

	@After
	public void after() {
		for (Long deploymentId : deploymentList) {
			repositoryService.deleteDeployment(deploymentId);
		}
	}

}
