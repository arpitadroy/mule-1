/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Composes {@link ExtensionManager} from a child and a parent artifacts, so child artifact can access extensions provided by the
 * parent.
 */
public class CompositeArtifactExtensionManager implements ExtensionManager {

  private final ExtensionManager parentExtensionManager;
  private final ExtensionManager childExtensionManager;
  private final Set<ExtensionModel> extensionModels;

  /**
   * Creates a composed extension manager
   *
   * @param parentExtensionManager extension manager for the parent artifact. Non null
   * @param childExtensionManager extension manager for the child artifact. Non null
   */
  public CompositeArtifactExtensionManager(ExtensionManager parentExtensionManager,
                                           ExtensionManager childExtensionManager) {
    checkArgument(parentExtensionManager != null, "parentExtensionManager cannot be null");
    checkArgument(childExtensionManager != null, "childExtensionManager cannot be null");

    this.parentExtensionManager = parentExtensionManager;
    this.childExtensionManager = childExtensionManager;

    extensionModels = new HashSet<>();
    extensionModels.addAll(parentExtensionManager.getExtensions());
    extensionModels.addAll(childExtensionManager.getExtensions());
  }

  @Override
  public void registerExtension(ExtensionModel extensionModel) {
    throw new UnsupportedOperationException("Composite extension manager cannot register extensions");
  }

  @Override
  public Set<ExtensionModel> getExtensions() {
    return extensionModels;
  }

  @Override
  public Optional<ExtensionModel> getExtension(String extensionName) {
    return extensionModels.stream().filter(extensionModel -> extensionModel.getName().equals(extensionName)).findFirst();
  }

  @Override
  public ConfigurationInstance getConfiguration(String configurationProviderName, Event event) {
    return getConfigurationProvider(configurationProviderName).map(provider -> provider.get(event))
        .orElseThrow(() -> new IllegalArgumentException(
                                                        format(
                                                               "There is no registered configurationProvider under name '%s'",
                                                               configurationProviderName)));
  }

  @Override
  public Optional<ConfigurationInstance> getConfiguration(ExtensionModel extensionModel, ComponentModel componentModel,
                                                          Event event) {

    Optional<ConfigurationProvider> provider = getConfigurationProvider(extensionModel, componentModel);
    if (provider.isPresent()) {
      return ofNullable(provider.get().get(event));
    }

    throw new IllegalArgumentException(format(
                                              "There is no registered configuration provider for extension '%s'",
                                              extensionModel.getName()));
  }

  @Override
  public Optional<ConfigurationProvider> getConfigurationProvider(String configurationProviderName) {
    Optional<ConfigurationProvider> configurationProvider =
        childExtensionManager.getConfigurationProvider(configurationProviderName);

    if (!configurationProvider.isPresent()) {
      configurationProvider = parentExtensionManager.getConfigurationProvider(configurationProviderName);
    }

    return configurationProvider;
  }

  public Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel, ComponentModel componentModel) {
    Optional<ConfigurationProvider> configurationModel =
        childExtensionManager.getConfigurationProvider(extensionModel, componentModel);

    if (!configurationModel.isPresent()) {
      configurationModel =
          parentExtensionManager.getConfigurationProvider(extensionModel, componentModel);;
    }

    return configurationModel;
  }

  @Override
  public void registerConfigurationProvider(ConfigurationProvider configurationProvider) {
    throw new UnsupportedOperationException("Composite extension manager cannot register extension providers");
  }

  @Override
  public ExtensionManifest parseExtensionManifestXml(URL manifestUrl) {
    throw new UnsupportedOperationException("Composite extension manager cannot parse extension manifests");
  }

  public ExtensionManager getParentExtensionManager() {
    return parentExtensionManager;
  }

  public ExtensionManager getChildExtensionManager() {
    return childExtensionManager;
  }
}
