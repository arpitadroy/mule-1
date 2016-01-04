/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi.support;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.bootstrap.BootstrapService;
import org.mule.config.bootstrap.ClassPathRegistryBootstrapDiscoverer;
import org.mule.config.bootstrap.PropertiesBootstrapService;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

public class BootstrapPropertiesServiceTracker implements Initialisable, Disposable
{

    private final BundleContext bundleContext;
    private ServiceRegistration<?> registeredService;

    public BootstrapPropertiesServiceTracker(BundleContext bundleContext)
    {
        this.bundleContext = bundleContext;
    }

    @Override
    public void dispose()
    {
        if (registeredService != null)
        {
            registeredService.unregister();
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        URL resource = bundleContext.getBundle().getResource(ClassPathRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES);

        if (resource != null)
        {
            BundleWiring bundleWiring = bundleContext.getBundle().adapt(BundleWiring.class);
            ClassLoader bundleClassLoader = bundleWiring.getClassLoader();

            try
            {
                Properties properties = new Properties();
                properties.load(resource.openStream());
                BootstrapService service = new PropertiesBootstrapService(bundleClassLoader, properties);
                registeredService = bundleContext.registerService(BootstrapService.class.getName(), service, null);
            }
            catch (IOException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }
}