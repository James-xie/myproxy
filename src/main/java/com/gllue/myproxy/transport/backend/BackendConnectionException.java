package com.gllue.myproxy.transport.backend;

import com.gllue.myproxy.transport.exception.TransportException;

public class BackendConnectionException extends TransportException {
  protected BackendConnectionException() {}

  public BackendConnectionException(Throwable cause, String msg, Object... args) {
    super(String.format(msg, args), cause);
  }
}
