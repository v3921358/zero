@echo off
@color F0
@title zero! Ver. 351
set CLASSPATH=.;dist\*;dist\lib\*;lib\*
java -Xms5G -Xmx8G -Dfile.encoding="UTF-8" -Dnet.sf.odinms.wzpath=wz server.Start
pause