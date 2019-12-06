package com.bisoft.minipg.service.pgwireprotocol.server;

import com.bisoft.minipg.service.pgwireprotocol.Util;
import com.bisoft.minipg.service.pgwireprotocol.server.Response.CommandExecutor;
import com.bisoft.minipg.service.pgwireprotocol.server.Response.Table;
import com.bisoft.minipg.service.pgwireprotocol.server.Response.TableHelper;
import com.bisoft.minipg.service.subservice.ConfigurationService;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PgBaseBackup extends AbstractWireProtocolPacket {

    private static final String PG_BASEBACKUP     = "-- pg_basebackup";
    private static final String MASTER_IP         = "(?<masterIp>.*)";
    private static final String RIGHT_PARANTHESIS = "[)]";
    private static final String LEFT_PARANTHESIS  = "[(]";
    String BASE_BACKUP = ".*" + PG_BASEBACKUP + LEFT_PARANTHESIS + MASTER_IP + RIGHT_PARANTHESIS + ".*";
    public String IpOfMaster;

    public WireProtocolPacket decode(byte[] buffer) {

        Pattern p = Pattern.compile(BASE_BACKUP, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(getPayloadString());
        m.matches();
        IpOfMaster = m.group("masterIp");
        return this;
    }

    @Override
    public byte[] response() {

        Table table     = null;
        File  parentDir = new File(ConfigurationService.GetValue("minipg.postgres_data_path"));
        if (parentDir.isDirectory() && parentDir.list() != null && parentDir.list().length == 0) {
            log.info("minipg.postgres_data_path is empty");
            String basebackupCommand = ConfigurationService.GetValue("minipg.postgres_bin_path") + "pg_basebackup" + "-h"
                + "--source-server=\"host=" + IpOfMaster + "\"" + "-D" + ConfigurationService.GetValue("minipg.postgres_data_path");
            log.info("EXECUTING THIS COMMAND for basebackup===> " + basebackupCommand);
            List<String> cellValues = (new CommandExecutor()).executeCommand(
                ConfigurationService.GetValue("minipg.postgres_bin_path") + "pg_basebackup",
                "--target-pgdata=" + ConfigurationService.GetValue("minipg.postgres_data_path"),
                "--source-server=\"host=" + IpOfMaster + "\"");
            cellValues.add(0, PG_BASEBACKUP + " received.." + basebackupCommand + " command executed at : " + new Date());
            table = (new TableHelper()).generateSingleColumnTable("result", cellValues, "SELECT");
        }
        return table != null ? table.generateMessage() : null;
    }

    public static boolean matches(String messageStr) {

        log.debug(messageStr);
        return Util.caseInsensitiveContains(messageStr, PG_BASEBACKUP);
    }
}
