/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.scout.commons.CollectionUtility;
import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.IVisitor;
import org.eclipse.scout.commons.filter.AlwaysFilter;
import org.eclipse.scout.rt.platform.IBean;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.testing.commons.BlockingCountDownLatch;
import org.eclipse.scout.rt.testing.platform.job.JobTestUtil;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class JobManagerTest {

  private IBean<IJobManager> m_jobManagerBean;

  @Before
  public void before() {
    m_jobManagerBean = JobTestUtil.registerJobManager();
  }

  @After
  public void after() {
    JobTestUtil.unregisterJobManager(m_jobManagerBean);
  }

  @Test
  public void testVisit() throws Exception {
    final BlockingCountDownLatch latch = new BlockingCountDownLatch(3);

    IFuture<Void> future1 = Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        latch.countDownAndBlock();
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    IFuture<Void> future2 = Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        latch.countDownAndBlock();
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    IFuture<Void> future3 = Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        latch.countDownAndBlock();
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    assertTrue(latch.await());

    // RUN THE TEST
    final Set<IFuture<?>> protocol = new HashSet<>();
    Jobs.getJobManager().visit(new AlwaysFilter<IFuture<?>>(), new IVisitor<IFuture<?>>() {

      @Override
      public boolean visit(IFuture<?> future) {
        protocol.add(future);
        return true;
      }
    });

    // VERIFY
    assertEquals(CollectionUtility.hashSet(future1, future2, future3), protocol);
  }

  @Test
  public void testShutdown() throws Exception {
    final Set<String> protocol = Collections.synchronizedSet(new HashSet<String>()); // synchronized because modified/read by different threads.

    final BlockingCountDownLatch setupLatch = new BlockingCountDownLatch(3);
    final BlockingCountDownLatch verifyLatch = new BlockingCountDownLatch(3);

    Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-1");
        }
        finally {
          verifyLatch.countDown();
        }
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-2");
        }
        finally {
          verifyLatch.countDown();
        }
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    Jobs.getJobManager().schedule(new IRunnable() {

      @Override
      public void run() throws Exception {
        try {
          setupLatch.countDownAndBlock();
        }
        catch (InterruptedException e) {
          protocol.add("interrupted-3");
        }
        finally {
          verifyLatch.countDown();
        }
      }
    }, Jobs.newInput()
        .withRunContext(RunContexts.copyCurrent())
        .withLogOnError(false));

    assertTrue(setupLatch.await());

    // RUN THE TEST
    Jobs.getJobManager().shutdown();

    // VERIFY
    assertTrue(verifyLatch.await());

    assertEquals(CollectionUtility.hashSet("interrupted-1", "interrupted-2", "interrupted-3"), protocol);
  }
}
