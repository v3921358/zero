/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FileoutputUtil {

    // Logging output file
    public static final String Acc_Stuck = "Log/Log_AccountStuck.rtf",
            Login_Error = "Log/Log_Login_Error.rtf",
            // IP_Log = "Log_AccountIP.rtf",
            //GMCommand_Log = "Log_GMCommand.rtf",
            // Zakum_Log = "Log_Zakum.rtf",
            //Horntail_Log = "Log_Horntail.rtf",
            현접풀기 = "Log/현접풀기로그.rtf",
            Pinkbean_Log = "Log/Log_Pinkbean.rtf",
            Cooldown_Log = "Log/Log_Cooldown.rtf",
            ScriptEx_Log = "Log/Log_Script_Except.rtf",
            PacketEx_Log = "Log/Log_Packet_Except.rtf", // I cba looking for every error, adding this back in.
            Donator_Log = "Log/Log_Donator.rtf",
            Hacker_Log = "Log/Log_Hacker.rtf",
            프로세스로그 = "Log/프로세스로그/" + getDCurrentTime() + ".txt",
            커넥터로그 = "Log/커넥터로그/" + getDCurrentTime() + ".txt",
            감지로그 = "Log/감지로그/" + getDCurrentTime() + ".txt",
            채팅로그 = "Log/채팅로그/" + getDCurrentTime() + ".txt",
            타이머로그 = "Log/타이머로그/" + getDCurrentTime() + ".txt", 
            결정석로그 = "Log/결정석로그/" + getDCurrentTime() + ".txt",
            Movement_Log = "Log/Log_Movement.rtf",
            CommandEx_Log = "Log/Log_Command_Except.rtf",
            Attack_Log = "Log/Log_Attack.txt",
            Kill_Log = "Log/Log_Kill.txt", //PQ_Log = "Log_PQ.rtf"
            음수로그 = "Log/음수로그/" + getDCurrentTime() + ".txt"
            ;
    // End
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdf_ = new SimpleDateFormat("yyyy-MM-dd");

    static {
        sdfGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String getDCurrentTime() {
        Calendar calz = Calendar.getInstance(TimeZone.getTimeZone("KST"), Locale.KOREAN);
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("yyyy-MM-dd");
        String time = simpleTimeFormat.format(calz.getTime());
//        if (calz.getTime().getHours() >= 12) {
//            time = "오후 "+time;
//        } else {
//            time = "오전 "+time;
//        }
        return time;
    }

    public static void log(final String file, final String msg) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            out.write(("\n------------------------ " + CurrentReadable_Time() + " ------------------------\n\r\n").getBytes());
            out.write(msg.getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static void outputFileError(final String file, final Throwable t) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            out.write(("\n------------------------ " + CurrentReadable_Time() + " ------------------------\n").getBytes());
            out.write(getString(t).getBytes());
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static String CurrentReadable_Date() {
        return sdf_.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_Time() {
        return sdf.format(Calendar.getInstance().getTime());
    }

    public static String CurrentReadable_TimeGMT() {
        return sdfGMT.format(new Date());
    }

    public static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }
}
