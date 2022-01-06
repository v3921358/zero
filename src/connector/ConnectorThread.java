package connector;

import client.MapleCharacter;
import static connector.ConnectorServerHandler.ConnecterLog;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.world.World;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.DefaultTableModel;
import server.Randomizer;

/**
 * @author 글귀
 */
public class ConnectorThread extends Thread {

    public ConnectorThread() {
    }

    public void run() {
        //System.out.println("스레드 시작");
        if (ServerConstants.ConnectorSetting) {
            try {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters().values()) {
                        try {
                            if (!chr.hasGmLevel(1)) {
                                if (chr.getClient().getconnecterClient() != null) {
                                    ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getLoginClient(chr.getClient().getconnecterClient().getId());
                                    if (cli != null) {
                                        if (cli.getAccountId() == chr.getClient().getAccID()) {
                                            continue;
                                        } else {
                                            System.out.println("[커넥터 오류] " + chr.getName() + " 캐릭터의 AccoutnID가 다름 : " + cli.getAccountId() + " / " + chr.getClient().getAccID());
                                            chr.changeMap(100000000, 0);
                                            chr.getClient().disconnect(true, false);
                                            chr.getClient().getSession().close();
//                                                        World.Find.forceDeregister(chr.getId(), chr.getName());
                                        }
                                    } else {
                                        System.out.println("[커넥터 오류] " + chr.getName() + " 캐릭터의 저장된 커넥터 클라이언트 정보가 Null");
                                        chr.changeMap(100000000, 0);
                                        chr.getClient().disconnect(true, false);
                                        chr.getClient().getSession().close();
//                                                World.Find.forceDeregister(chr.getId(), chr.getName());
                                    }
                                } else {
                                    ConnectorClient cli = ConnectorServer.getInstance().getClientStorage().getLoginClient(chr.getClient().getAccountName());
                                    if (cli != null) {
                                        chr.getClient().setconnecterClient(cli);
                                    } else {
                                        System.out.println("[커넥터 오류] " + chr.getName() + " 캐릭터의 계정 커넥터 클라이언트 정보가 Null");
                                        chr.changeMap(100000000, 0);
                                        chr.getClient().disconnect(true, false);
                                        chr.getClient().getSession().close();
//                                        World.Find.forceDeregister(chr.getId(), chr.getName());
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                ConnectorClientStorage cs = ConnectorServer.getInstance().getClientStorage();
                try {
                    DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                    for (int i = model.getRowCount() - 1; i >= 0; i--) {
                        String[] names = model.getValueAt(i, 0).toString().split(",");
                        ConnectorClient cli = cs.getClientByName(names[0]);
                        if (cli == null && names.length > 1) {
                            try {
                                cli = cs.getClientByName(model.getValueAt(i, 0).toString().split(",")[1]);
                            } catch (Exception ex) {
                                System.out.println("두번쨰닉\r\n" + ex);
                                ex.printStackTrace();
                            }
                        }
                        if (model.getValueAt(i, 4) != null) {
                            /*
                             접속중인 캐릭터 변경
                             */
                            try {
                                if (cs.getChangeInGameCharWaiting(model.getValueAt(i, 4).toString()) != null) {
                                    if (cli != null) {
                                        model.setValueAt(cli.getIngameCharString(), i, 3);
                                    }
                                    cs.deregisterChangeInGameCharWaiting(model.getValueAt(i, 4).toString());
                                }
                            } catch (Exception ex) {
                                System.out.println("체인지 인게임\r\n" + ex);
                                ex.printStackTrace();
                            }
                            /*
                             접속종료
                             */
                            try {
                                if (cs.getRemoveWaiting(model.getValueAt(i, 4).toString()) != null) {
                                    cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                                    model.removeRow(i);
                                }
                            } catch (Exception ex) {
                                System.out.println("리무브 웨이팅\r\n" + ex);
                                ex.printStackTrace();
                            }
                        } else {
                            cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                        }

                    }
                } catch (Exception ex) {
                    System.out.println("모델을 제거하는 도중 오류 발생\r\n" + ex);
                }
                /*try {
                 DefaultTableModel model = (DefaultTableModel) ConnectorPanel.jTable1.getModel();
                 ConnectorClientStorage cs = ConnectorServer.getInstance().getClientStorage();
                 for (int i = model.getRowCount() - 1; i >= 0; i--) {
                 if (model.getValueAt(i, 4) != null) {
                 if (cs.getRemoveWaiting(model.getValueAt(i, 4).toString()) != null) {
                 cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                 model.removeRow(i);
                 }
                 } else {
                 cs.deregisterRemoveWaiting(model.getValueAt(i, 4).toString());
                 model.removeRow(i);
                 }
                 }
                 } catch (Exception ex) {
                 ex.printStackTrace();
                 }*/
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                /*try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConnectorThread.class.getName()).log(Level.SEVERE, null, ex);
                }*/
            }
        }
    }

    public static void main(String[] args) {
        ConnectorThread CT = new ConnectorThread();
        CT.start();
    }

}
