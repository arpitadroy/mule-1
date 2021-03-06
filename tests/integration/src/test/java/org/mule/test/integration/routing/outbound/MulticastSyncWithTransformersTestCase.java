/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.functional.functional.FlowAssert;
import org.mule.runtime.api.message.Message;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class MulticastSyncWithTransformersTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/multicaster-sync-with-transformers-test-flow.xml";
  }

  @Test
  public void testSyncMulticast() throws Exception {
    Apple apple = new Apple();
    Banana banana = new Banana();
    Orange orange = new Orange();
    FruitBowl fruitBowl = new FruitBowl(apple, banana);
    fruitBowl.addFruit(orange);

    Message result = flowRunner("Distributor").withPayload(fruitBowl).run().getMessage();

    assertNotNull(result);
    assertTrue(result.getPayload().getValue() instanceof List);
    List<Fruit> results = ((List<Message>) result.getPayload().getValue()).stream()
        .map(msg -> (Fruit) msg.getPayload().getValue()).collect(toList());
    assertEquals(3, results.size());

    assertTrue(results.contains(apple));
    assertTrue(results.contains(banana));
    assertTrue(results.contains(orange));

    FlowAssert.verify();
  }
}
