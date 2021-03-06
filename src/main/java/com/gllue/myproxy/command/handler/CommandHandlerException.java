package com.gllue.myproxy.command.handler;

import com.gllue.myproxy.common.exception.BaseServerException;

public class CommandHandlerException extends BaseServerException {
  public CommandHandlerException() {
  }

  public CommandHandlerException(Throwable cause) {
    super(cause);
  }

  public CommandHandlerException(String msg, Object... args) {
    super(String.format(msg, args));
  }

  public CommandHandlerException(Throwable cause, String msg, Object... args) {
    super(String.format(msg, args), cause);
  }
}
