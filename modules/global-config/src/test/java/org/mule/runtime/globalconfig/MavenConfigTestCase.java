/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.RuntimeGlobalConfiguration.MavenGlobalConfiguration.MAVEN_GLOBAL_CONFIGURATION_STORY;
import static org.mule.test.allure.AllureConstants.RuntimeGlobalConfiguration.RUNTIME_GLOBAL_CONFIGURATION;
import org.mule.maven.client.api.MavenConfiguration;
import org.mule.maven.client.api.RemoteRepository;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.globalconfig.api.exception.RuntimeGlobalConfigException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(RUNTIME_GLOBAL_CONFIGURATION)
@Stories(MAVEN_GLOBAL_CONFIGURATION_STORY)
public class MavenConfigTestCase extends AbstractMuleTestCase {

  private static final String MAVEN_CENTRAL_REPO_ID = "mavenCentral";
  private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Description("Test a single file loaded from the classpath and verifies that the mule.conf and mule.properties json are not taken into account.")
  @Test
  public void loadFromFileOnly() throws MalformedURLException {
    GlobalConfigLoader.reset();
    MavenConfiguration mavenConfig = getMavenConfig();
    List<RemoteRepository> remoteRepositories = mavenConfig.getMavenRemoteRepositories();
    assertThat(remoteRepositories, hasSize(1));

    RemoteRepository remoteRepository = remoteRepositories.get(0);
    assertThat(remoteRepository.getId(), is(MAVEN_CENTRAL_REPO_ID));
    assertThat(remoteRepository.getUrl(), is(new URL(MAVEN_CENTRAL_URL)));
    assertThat(remoteRepository.getAuthentication().get().getPassword(), is("password"));
    assertThat(remoteRepository.getAuthentication().get().getUsername(), is("username"));
  }

  @Description("Loads the configuration from mule-config.json and overrides the maven repository location using a system property")
  @Test
  public void loadFromFileWithOverrideFromSystemPropertyOfSimpleValue() throws Exception {
    String repoLocation = temporaryFolder.getRoot().getAbsolutePath();
    testWithSystemProperty("muleRuntimeConfig.maven.repositoryLocation", repoLocation, () -> {
      GlobalConfigLoader.reset();
      MavenConfiguration mavenConfig = getMavenConfig();
      String configuredRepoLocation = mavenConfig.getLocalMavenRepositoryLocation().getAbsolutePath();
      assertThat(configuredRepoLocation, is(repoLocation));
    });
  }

  @Description("Loads the configuration from mule-config.json and adds an additional maven repository using system properties")
  @Test
  public void loadFromFileWithAdditionalRepoFromSystemProperty() throws Exception {
    String additionalRepositoryUrl = "http://localhost/host";
    testWithSystemProperty("muleRuntimeConfig.maven.repositories.customRepo.url", additionalRepositoryUrl, () -> {
      GlobalConfigLoader.reset();
      MavenConfiguration mavenConfig = getMavenConfig();
      List<RemoteRepository> mavenRemoteRepositories = mavenConfig.getMavenRemoteRepositories();
      assertThat(mavenRemoteRepositories, hasSize(2));
      assertThat(mavenRemoteRepositories.get(0).getId(), is("customRepo"));
      assertThat(mavenRemoteRepositories.get(0).getUrl(), is(new URL(additionalRepositoryUrl)));
      assertThat(mavenRemoteRepositories.get(1).getId(), is("mavenCentral"));
      assertThat(mavenRemoteRepositories.get(1).getUrl(), is(new URL(MAVEN_CENTRAL_URL)));
    });
  }

  @Description("Loads the configuration from mule-config.json and overrides a single attribute of a complex value")
  @Test
  public void loadFromFileWithOverrideFromSystemPropertyOfComplexValue() throws Exception {
    String mavenCentralOverriddenUrl = "http://localhost/host";
    testWithSystemProperty("muleRuntimeConfig.maven.repositories.mavenCentral.url", mavenCentralOverriddenUrl, () -> {
      GlobalConfigLoader.reset();
      MavenConfiguration mavenConfig = getMavenConfig();
      List<RemoteRepository> mavenRemoteRepositories = mavenConfig.getMavenRemoteRepositories();
      assertThat(mavenRemoteRepositories, hasSize(1));
      assertThat(mavenRemoteRepositories.get(0).getId(), is("mavenCentral"));
      assertThat(mavenRemoteRepositories.get(0).getUrl(), is(new URL(mavenCentralOverriddenUrl)));
    });
  }

  @Description("Loads the configuration from mule-config.json and defines the maven local repository location wrongly")
  @Test
  public void wrongLocalRepositoryLocationConfig() throws Exception {
    String repoLocation = "badLocation";
    testWithSystemProperty("muleRuntimeConfig.maven.repositoryLocation", repoLocation, () -> {
      expectedException.expect(instanceOf(RuntimeGlobalConfigException.class));
      expectedException.expectMessage("Repository folder badLocation configured for the mule runtime does not exists");
      GlobalConfigLoader.reset();
    });
  }

}
