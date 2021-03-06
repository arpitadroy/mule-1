/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link CursorStreamProvider} implementations.
 *
 * @since 4.0
 */
public abstract class AbstractCursorStreamProvider implements CursorStreamProvider {

  protected final InputStream wrappedStream;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ByteBufferManager bufferManager;

  /**
   * Creates a new instance
   *
   * @param wrappedStream the original stream to be decorated
   * @param bufferManager the {@link ByteBufferManager} that will be used to allocate all buffers
   */
  public AbstractCursorStreamProvider(InputStream wrappedStream, ByteBufferManager bufferManager) {
    this.wrappedStream = wrappedStream;
    this.bufferManager = bufferManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final CursorStream openCursor() {
    checkState(!closed.get(), "Cannot open a new cursor on a closed stream");
    return doOpenCursor();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    closed.set(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * @return the {@link ByteBufferManager} that <b>MUST</b> to be used to allocate byte buffers
   */
  protected ByteBufferManager getBufferManager() {
    return bufferManager;
  }

  protected abstract CursorStream doOpenCursor();
}
