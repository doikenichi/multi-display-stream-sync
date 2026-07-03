package com.streaminglab.testframework;

import io.cucumber.core.options.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.streaminglab.testframework.steps")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty,summary,json:build/reports/cucumber/cucumber.json")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "@contract and @smoke")
public class RunCucumberTest {}
