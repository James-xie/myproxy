package com.gllue.myproxy.command.result;

import com.gllue.myproxy.command.result.query.QueryResult;
import com.gllue.myproxy.transport.protocol.packet.generic.OKPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommandResult {
  private final long affectedRows;

  private final long lastInsertId;

  private final int statusFlag;

  private final int warnings;

  private final String info;

  private final QueryResult queryResult;

  public CommandResult(
      final long affectedRows,
      final long lastInsertId,
      final int statusFlag,
      final int warnings,
      QueryResult queryResult) {
    this.affectedRows = affectedRows;
    this.lastInsertId = lastInsertId;
    this.statusFlag = statusFlag;
    this.warnings = warnings;
    this.info = "";
    this.queryResult = queryResult;
  }

  public static CommandResult newInstance(final OKPacket packet) {
    return newInstance(packet, null);
  }

  public static CommandResult newEmptyResult() {
    return new CommandResult(0, 0, 0, 0, null);
  }

  public static CommandResult newInstance(final OKPacket packet, final QueryResult queryResult) {
    return new CommandResult(
        packet.getAffectedRows(),
        packet.getLastInsertId(),
        packet.getStatusFlag(),
        packet.getWarnings(),
        packet.getInfo(),
        queryResult);
  }
}
