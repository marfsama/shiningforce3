package com.sf3.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;

/**
 * Ein {@link ImageInputStream} der auf einem Byte-Array basiert.
 */
public class ByteArrayImageInputStream extends ImageInputStreamImpl
{
  private byte[] bytes;
  private ByteArrayInputStream stream;

  public ByteArrayImageInputStream(byte[] bytes)
  {
    this.bytes = bytes;
    this.stream = new ByteArrayInputStream(bytes);
  }

  @Override
  public int read() throws IOException
  {
    checkClosed();
    bitOffset = 0;
    int val = stream.read();
    if (val != -1)
    {
        ++streamPos;
    }
    return val;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException
  {
    checkClosed();
    bitOffset = 0;
    int nbytes = stream.read(b, off, len);
    if (nbytes != -1)
    {
      streamPos += nbytes;
    }
    return nbytes;
  }

  @Override
  public long length()
  {
    return bytes.length;
  }

  @Override
  public void seek(long pos) throws IOException
  {
    checkClosed();
    if (pos < flushedPos)
    {
      throw new IndexOutOfBoundsException("pos < flushedPos: "+pos+" < "+flushedPos);
    }
    bitOffset = 0;
    stream = new ByteArrayInputStream(bytes);
    streamPos = stream.skip(pos);
  }

  @Override
  public void close() throws IOException
  {
    super.close();
    stream.close();
    stream = null;
    bytes = null;
  }

}
