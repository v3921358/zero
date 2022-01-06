/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import client.MapleCharacter;
import client.messages.commands.AdminCommand;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.DefaultListModel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import scripting.NPCScriptManager;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.Timer.EventTimer;
import server.Timer.EtcTimer;
import server.ShutdownServer;
import server.control.MapleHotTimeControl;
import server.control.MapleRateEventControl;
import server.life.MapleMonsterInformationProvider;
import server.shops.MapleShopFactory;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class ControlPannel extends javax.swing.JFrame implements ActionListener {

    static String txtMsg, txtOpt;
    static int intOpt;
    public static DefaultListModel accounts = new DefaultListModel();
    public static DefaultListModel characters = new DefaultListModel();

    public ControlPannel() {
        initComponents();
        EtcTimer.getInstance().register(new Runnable() {

            @Override
            public void run() {
                jLabel17.setText(DatabaseConnection.getIdleConnections() + "");
                jLabel19.setText(DatabaseConnection.getActiveConnections() + "");
                jLabel21.setText(Runtime.getRuntime().availableProcessors() + " / " + Thread.activeCount());
            }
        }, 500);
        /*
         String filePath = "KickList.txt";
         DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
         try (
         InputStream inStream = new FileInputStream(filePath);
         InputStreamReader reader = new InputStreamReader(inStream, "UTF-8");
         BufferedReader bufReader = new BufferedReader(reader);) {
         StringBuilder sb = new StringBuilder();
         String line = null;
         while ((line = bufReader.readLine()) != null) {
         sb.append(line).append('\n');
         model.addRow(new Object[]{line});
         }
         if (sb.length() > 0) {
         sb.setLength(sb.length() - 1);
         }
         } catch (FileNotFoundException e) {
         e.printStackTrace();
         } catch (IOException e) {
         e.printStackTrace();
         };
         */

    }

    public static void addaccount(String account) {
        try {
            accounts.addElement(account);
            jList3.setModel(accounts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeaccount(String account) {
        accounts.removeElement(account);
        jList3.setModel(accounts);
    }

    public static void addcharacter(String chr) {
        try {
            characters.addElement(chr);
            jList4.setModel(characters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removecharacter(String chr) {
        characters.removeElement(chr);
        jList4.setModel(characters);
    }

    public static class resetAccountOnline extends TimerTask {

        @Override
        public void run() {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters1()) {
                    if (!accounts.contains(player.getName())) {
                        addaccount(player.getName());
                        jLabel25.setText(String.valueOf((int) (Integer.parseInt(jLabel25.getText()) + 1)));
                    }
                }
            }
        }
    }

    public static class resetCharacterOnline extends TimerTask {

        @Override
        public void run() {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters1()) {
                    if (!characters.contains(player.getName())) {
                        addcharacter(player.getName());
                        jLabel26.setText(String.valueOf((int) (Integer.parseInt(jLabel26.getText()) + 1)));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jFrame2 = new javax.swing.JFrame();
        jButton5 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        jButton8 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jButton9 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jTextField9 = new javax.swing.JTextField();
        jButton11 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jButton12 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jTextField10 = new javax.swing.JTextField();
        jButton15 = new javax.swing.JButton();
        jTextField11 = new javax.swing.JTextField();
        jScrollPane6 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jButton16 = new javax.swing.JButton();
        jButton17 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList<>();
        jLabel7 = new javax.swing.JLabel();
        jTextField12 = new javax.swing.JTextField();
        jTextField13 = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jButton20 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jButton19 = new javax.swing.JButton();
        jButton22 = new javax.swing.JButton();
        jTextField14 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jTextField15 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextField16 = new javax.swing.JTextField();
        jButton23 = new javax.swing.JButton();
        jComboBox2 = new javax.swing.JComboBox<>();
        jTextField17 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextField18 = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jTextField19 = new javax.swing.JTextField();
        jButton24 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton10 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList<>();
        jLabel24 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList<>();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        jFrame2.setName("jFrame2"); // NOI18N
        jFrame2.setResizable(false);

        jButton5.setText("강제종료");
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Process Name", "Window Title"
                }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        jTable3.setName("jTable3"); // NOI18N
        jTable3.getTableHeader().setReorderingAllowed(false);
        jTable3.getTableHeader().setResizingAllowed(false);
        jScrollPane3.setViewportView(jTable3);

        org.jdesktop.layout.GroupLayout jFrame2Layout = new org.jdesktop.layout.GroupLayout(jFrame2.getContentPane());
        jFrame2.getContentPane().setLayout(jFrame2Layout);
        jFrame2Layout.setHorizontalGroup(
                jFrame2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jFrame2Layout.createSequentialGroup()
                                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 475, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(0, 0, Short.MAX_VALUE))
                        .add(jFrame2Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jButton5)
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jFrame2Layout.setVerticalGroup(
                jFrame2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jFrame2Layout.createSequentialGroup()
                                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                                .add(18, 18, 18)
                                .add(jButton5)
                                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setName("Form"); // NOI18N
        setResizable(false);

        jTabbedPane1.setName("서버관련"); // NOI18N

        jPanel5.setName("jPanel5"); // NOI18N

        jButton8.setText("지급");
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton8MouseClicked(evt);
            }
        });
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel1.setText("후원 포인트 지급");
        jLabel1.setName("jLabel1"); // NOI18N

        jTextField2.setText("유저 이름");
        jTextField2.setName("jTextField2"); // NOI18N
        jTextField2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField2MouseClicked(evt);
            }
        });
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField2ActionPerformed(evt);
            }
        });

        jTextField3.setText("아이템 코드");
        jTextField3.setName("jTextField3"); // NOI18N
        jTextField3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField3MouseClicked(evt);
            }
        });
        jTextField3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField3ActionPerformed(evt);
            }
        });

        jTextField5.setText("개수");
        jTextField5.setName("jTextField5"); // NOI18N
        jTextField5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField5MouseClicked(evt);
            }

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                jTextField5MouseEntered(evt);
            }
        });
        jTextField5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField5ActionPerformed(evt);
            }
        });

        jLabel2.setText("아이템 지급");
        jLabel2.setName("jLabel2"); // NOI18N

        jTextField6.setText("유저 이름");
        jTextField6.setName("jTextField6"); // NOI18N
        jTextField6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField6MouseClicked(evt);
            }
        });
        jTextField6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField6ActionPerformed(evt);
            }
        });

        jTextField7.setText("포인트");
        jTextField7.setName("jTextField7"); // NOI18N
        jTextField7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField7MouseClicked(evt);
            }
        });
        jTextField7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField7ActionPerformed(evt);
            }
        });

        jButton9.setText("지급");
        jButton9.setName("jButton9"); // NOI18N
        jButton9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton9MouseClicked(evt);
            }
        });
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jLabel3.setText("홍보 포인트 지급");
        jLabel3.setName("jLabel3"); // NOI18N

        jTextField8.setText("유저 이름");
        jTextField8.setName("jTextField8"); // NOI18N
        jTextField8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField8MouseClicked(evt);
            }
        });
        jTextField8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField8ActionPerformed(evt);
            }
        });

        jTextField9.setText("포인트");
        jTextField9.setName("jTextField9"); // NOI18N
        jTextField9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField9MouseClicked(evt);
            }
        });
        jTextField9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField9ActionPerformed(evt);
            }
        });

        jButton11.setText("지급");
        jButton11.setName("jButton11"); // NOI18N
        jButton11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton11MouseClicked(evt);
            }
        });
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jLabel4.setText("데이터 리셋");
        jLabel4.setName("jLabel4"); // NOI18N

        jButton12.setText("리셋");
        jButton12.setName("jButton12"); // NOI18N
        jButton12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton12MouseClicked(evt);
            }
        });
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"옵코드", "드롭", "포탈", "엔피시", "상점", "이벤트"}));
        jComboBox1.setName("jComboBox1"); // NOI18N
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel5.setText("핫타임 (자동)");
        jLabel5.setName("jLabel5"); // NOI18N

        jTextField10.setText("아이템 코드");
        jTextField10.setName("jTextField10"); // NOI18N
        jTextField10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField10MouseClicked(evt);
            }
        });
        jTextField10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField10ActionPerformed(evt);
            }
        });

        jButton15.setText("추가");
        jButton15.setName("jButton15"); // NOI18N
        jButton15.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton15MouseClicked(evt);
            }
        });
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jTextField11.setText("개수");
        jTextField11.setName("jTextField11"); // NOI18N
        jTextField11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField11MouseClicked(evt);
            }
        });
        jTextField11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField11ActionPerformed(evt);
            }
        });

        jScrollPane6.setName("jScrollPane6"); // NOI18N

        jList1.setName("jList1"); // NOI18N
        jScrollPane6.setViewportView(jList1);

        jButton16.setText("지급");
        jButton16.setName("jButton16"); // NOI18N
        jButton16.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton16MouseClicked(evt);
            }
        });
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jButton17.setText("전체 삭제");
        jButton17.setName("jButton17"); // NOI18N
        jButton17.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton17MouseClicked(evt);
            }
        });
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jButton18.setText("선택 삭제");
        jButton18.setName("jButton18"); // NOI18N
        jButton18.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton18MouseClicked(evt);
            }
        });
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        jLabel6.setText("핫타임 (수동)");
        jLabel6.setName("jLabel6"); // NOI18N

        jScrollPane7.setName("jScrollPane7"); // NOI18N

        jList2.setName("jList2"); // NOI18N
        jScrollPane7.setViewportView(jList2);

        jLabel7.setText("지급 시간");
        jLabel7.setName("jLabel7"); // NOI18N

        jTextField12.setEditable(false);
        jTextField12.setName("jTextField12"); // NOI18N
        jTextField12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField12MouseClicked(evt);
            }
        });
        jTextField12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField12ActionPerformed(evt);
            }
        });

        jTextField13.setEditable(false);
        jTextField13.setName("jTextField13"); // NOI18N
        jTextField13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextField13MouseClicked(evt);
            }
        });
        jTextField13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField13ActionPerformed(evt);
            }
        });

        jLabel8.setText("시");
        jLabel8.setName("jLabel8"); // NOI18N

        jLabel9.setText("분");
        jLabel9.setName("jLabel9"); // NOI18N

        jButton20.setText("로드");
        jButton20.setName("jButton20"); // NOI18N
        jButton20.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton20MouseClicked(evt);
            }
        });
        jButton20.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton20ActionPerformed(evt);
            }
        });

        jButton21.setText("<html>\n<center>프로퍼티<br>\n리로드</center>\n</html>");
        jButton21.setActionCommand("hottime.txt 리로드");
        jButton21.setName("jButton21"); // NOI18N
        jButton21.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton21MouseClicked(evt);
            }
        });
        jButton21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton21ActionPerformed(evt);
            }
        });

        jLabel27.setText("GM설정");
        jLabel27.setName("jLabel27"); // NOI18N

        jTextField1.setText("유저 이름");
        jTextField1.setName("jTextField1"); // NOI18N

        jTextField4.setText("레벨");
        jTextField4.setName("jTextField4"); // NOI18N

        jButton3.setText("설정");
        jButton3.setName("jButton3"); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jComboBox1, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jScrollPane7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane6)
                                                        .add(jPanel5Layout.createSequentialGroup()
                                                                .add(jLabel5)
                                                                .add(0, 0, Short.MAX_VALUE))
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                                                                .add(jTextField10)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                                .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 95, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPanel5Layout.createSequentialGroup()
                                                                .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jLabel8)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 6, Short.MAX_VALUE)
                                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                                                                                .add(jLabel7)
                                                                                .add(19, 19, 19))
                                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel9)))
                                                        .add(jButton12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton17, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton18, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton15, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton16, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton20, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton21)))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 198, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 197, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton9, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 135, Short.MAX_VALUE))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jLabel3)
                                                        .add(jLabel1)
                                                        .add(jLabel2)
                                                        .add(jLabel4)
                                                        .add(jLabel6)
                                                        .add(jLabel27))
                                                .add(0, 0, Short.MAX_VALUE))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPanel5Layout.createSequentialGroup()
                                                                .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 95, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 218, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                        .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jButton8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jButton3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel27)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton3))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel2)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton8))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel1)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton9))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel3)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField9, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton11))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel6)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jButton18)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton17)
                                                .add(103, 103, 103)
                                                .add(jButton16))
                                        .add(jScrollPane6, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 166, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPanel5Layout.createSequentialGroup()
                                                                .add(jLabel7)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .add(jLabel9))
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                                                                .add(0, 0, Short.MAX_VALUE)
                                                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                                                                .add(jTextField12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                                .add(jLabel8))
                                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jTextField13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                .add(jButton21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton20))
                                        .add(jPanel5Layout.createSequentialGroup()
                                                .add(0, 0, Short.MAX_VALUE)
                                                .add(jLabel5)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jScrollPane7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 90, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                .add(10, 10, 10)
                                .add(jLabel4)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jButton12)
                                        .add(jComboBox1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        jTabbedPane1.addTab("서버 관련 1", jPanel5);

        jPanel6.setName("jPanel6"); // NOI18N

        jLabel10.setText("배율 변경");
        jLabel10.setName("jLabel10"); // NOI18N

        jButton19.setText("로드");
        jButton19.setName("jButton19"); // NOI18N
        jButton19.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton19MouseClicked(evt);
            }
        });
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        jButton22.setText("이벤트 시작");
        jButton22.setName("jButton22"); // NOI18N
        jButton22.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton22MouseClicked(evt);
            }
        });

        jTextField14.setName("jTextField14"); // NOI18N
        jTextField14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField14ActionPerformed(evt);
            }
        });

        jLabel11.setText("경험치");
        jLabel11.setName("jLabel11"); // NOI18N

        jLabel12.setText("메소");
        jLabel12.setName("jLabel12"); // NOI18N

        jTextField15.setName("jTextField15"); // NOI18N

        jLabel13.setText("드롭");
        jLabel13.setName("jLabel13"); // NOI18N

        jTextField16.setName("jTextField16"); // NOI18N

        jButton23.setText("변경");
        jButton23.setName("jButton23"); // NOI18N
        jButton23.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton23MouseClicked(evt);
            }
        });
        jButton23.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton23ActionPerformed(evt);
            }
        });

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"경험치", "메소", "드롭"}));
        jComboBox2.setName("jComboBox2"); // NOI18N

        jTextField17.setText("배율");
        jTextField17.setName("jTextField17"); // NOI18N

        jLabel14.setText("배율 이벤트");
        jLabel14.setName("jLabel14"); // NOI18N

        jTextField18.setText("시간 (분)");
        jTextField18.setName("jTextField18"); // NOI18N

        jLabel15.setText("리붓");
        jLabel15.setName("jLabel15"); // NOI18N

        jTextField19.setText("시간 (분)");
        jTextField19.setName("jTextField19"); // NOI18N

        jButton24.setText("리붓하기");
        jButton24.setToolTipText("");
        jButton24.setName("jButton24"); // NOI18N
        jButton24.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton24MouseClicked(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
                jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel6Layout.createSequentialGroup()
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jLabel10)
                                                        .add(jLabel14)
                                                        .add(jLabel15))
                                                .add(0, 0, Short.MAX_VALUE))
                                        .add(jPanel6Layout.createSequentialGroup()
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                                        .add(jComboBox2, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel6Layout.createSequentialGroup()
                                                                .add(jLabel11)
                                                                .add(10, 10, 10)
                                                                .add(jTextField14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel12)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel13)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jTextField16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 88, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jButton23, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .add(jPanel6Layout.createSequentialGroup()
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                                        .add(jPanel6Layout.createSequentialGroup()
                                                                .add(94, 94, 94)
                                                                .add(jTextField18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jTextField17))
                                                        .add(jTextField19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 299, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton24, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .add(jPanel6Layout.createSequentialGroup()
                                                                .add(jButton22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 124, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .add(0, 0, Short.MAX_VALUE)))))
                                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
                jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel10)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jLabel11)
                                        .add(jLabel12)
                                        .add(jTextField15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jLabel13)
                                        .add(jTextField16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton19)
                                        .add(jButton23))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jLabel14)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jComboBox2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jTextField18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton22))
                                .add(18, 18, 18)
                                .add(jLabel15)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jTextField19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jButton24))
                                .addContainerGap(455, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("서버 관련 2", jPanel6);

        jPanel4.setName("jPanel4"); // NOI18N

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane5.setViewportView(jTextArea1);

        jButton10.setText("청소");
        jButton10.setName("jButton10"); // NOI18N
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jScrollPane5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel4Layout.createSequentialGroup()
                                .add(jScrollPane5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 406, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("채팅로그", jPanel4);

        jPanel1.setName("jPanel1"); // NOI18N

        jLabel16.setText("Idle DB Connection :");
        jLabel16.setName("jLabel16"); // NOI18N

        jLabel17.setText("jLabel17");
        jLabel17.setName("jLabel17"); // NOI18N

        jLabel18.setText("Active DB Connection : ");
        jLabel18.setName("jLabel18"); // NOI18N

        jLabel19.setText("jLabel19");
        jLabel19.setName("jLabel19"); // NOI18N

        jLabel20.setText("Active Threads : ");
        jLabel20.setName("jLabel20"); // NOI18N

        jLabel21.setText("jLabel21");
        jLabel21.setName("jLabel21"); // NOI18N

        jLabel22.setText("DataBase Connection");
        jLabel22.setName("jLabel22"); // NOI18N

        jLabel23.setText("Connection account : ");
        jLabel23.setName("jLabel23"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jList3.setName("jList3"); // NOI18N
        jScrollPane1.setViewportView(jList3);

        jLabel24.setText("Connection Character :");
        jLabel24.setName("jLabel24"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jList4.setName("jList4"); // NOI18N
        jScrollPane2.setViewportView(jList4);

        jLabel25.setText("0");
        jLabel25.setName("jLabel25"); // NOI18N

        jLabel26.setText("0");
        jLabel26.setName("jLabel26"); // NOI18N

        jButton1.setText("새로고침");
        jButton1.setName("jButton1"); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("새로고침");
        jButton2.setName("jButton2"); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jPanel1Layout.createSequentialGroup()
                                                .add(jButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 219, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                .add(jButton2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addContainerGap())
                                        .add(jPanel1Layout.createSequentialGroup()
                                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                        .add(jPanel1Layout.createSequentialGroup()
                                                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 219, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                        .add(jPanel1Layout.createSequentialGroup()
                                                                                .add(jLabel23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 125, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                                .add(jLabel25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                                                                .add(10, 10, 10)
                                                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                                                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                                        .add(jPanel1Layout.createSequentialGroup()
                                                                                .add(jLabel24)
                                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                                .add(jLabel26)
                                                                                .add(0, 90, Short.MAX_VALUE))))
                                                        .add(jLabel22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 192, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                        .add(jPanel1Layout.createSequentialGroup()
                                                                .add(jLabel20)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jLabel21))
                                                        .add(jPanel1Layout.createSequentialGroup()
                                                                .add(jLabel16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jLabel17)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                                                .add(jLabel18)
                                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                                .add(jLabel19)))
                                                .add(12, 12, 12))))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .add(jLabel22)
                                .add(9, 9, 9)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel16)
                                        .add(jLabel17)
                                        .add(jLabel18)
                                        .add(jLabel19))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel21)
                                        .add(jLabel20))
                                .add(18, 18, 18)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jLabel23)
                                        .add(jLabel24)
                                        .add(jLabel25)
                                        .add(jLabel26))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 111, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                        .add(jButton1)
                                        .add(jButton2))
                                .addContainerGap(373, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Connection", jPanel1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jTabbedPane1)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(jTabbedPane1)
        );

        pack();
    }// </editor-fold>

    public void logChat(String a) {

    }

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {
        String procn = jTable3.getValueAt(jTable3.getSelectedRow(), 0).toString();

    }

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {
        ControlPannel.jTextArea1.setText("");
        //c.getSession().writeAndFlush();
    }

    private void jTextField2ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField3ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField2MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField2.setText("");        // TODO add your handling code here:
    }

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField5ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField3MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField3.setText("");        // TODO add your handling code here:
    }

    private void jTextField5MouseEntered(java.awt.event.MouseEvent evt) {

    }

    private void jTextField5MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField5.setText("");        // TODO add your handling code here:
    }

    private void jButton8MouseClicked(java.awt.event.MouseEvent evt) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters1()) {
                if (chr.getName().equals(jTextField2.getText())) {
                    chr.gainItem(Integer.parseInt(jTextField3.getText()), Integer.parseInt(jTextField5.getText()));
                    chr.dropMessage(1, "아이템이 지급되었습니다. 인벤토리를 확인해 주세요!");
                    JOptionPane.showMessageDialog(null, "지급이 완료되었습니다.");
                    return;
                }
            }
        }
        JOptionPane.showMessageDialog(null, "캐릭터를 찾을 수 없습니다.");
        // TODO add your handling code here:
    }

    private void jTextField6MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField6.setText("");        // TODO add your handling code here:
    }

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField7MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField7.setText("");    // TODO add your handling code here:
    }

    private void jTextField7ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton9MouseClicked(java.awt.event.MouseEvent evt) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters1()) {
                if (chr.getName().equals(jTextField6.getText())) {
                    chr.gainDonationPoint(Integer.parseInt(jTextField7.getText()));
                    chr.dropMessage(1, "후원포인트가 지급되었습니다.");
                    JOptionPane.showMessageDialog(null, "지급이 완료되었습니다.");
                    return;
                }
            }
        }
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            int dpoint = 0;
            int accountid = 0;
            PreparedStatement ps1 = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps1.setString(1, jTextField6.getText());
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            } else {
                JOptionPane.showMessageDialog(null, "존재하지 않는 캐릭터입니다.");
                return;
            }
            PreparedStatement ps2 = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps2.setInt(1, accountid);
            ResultSet rs1 = ps2.executeQuery();
            if (rs1.next()) {
                dpoint = rs1.getInt("dpoint");
            } else {
                JOptionPane.showMessageDialog(null, "존재하지 않는 계정입니다.");
                return;
            }
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET dpoint = ? WHERE id = ?");
            ps.setInt(1, dpoint + Integer.parseInt(jTextField7.getText()));
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
            rs.close();
            ps1.close();
            rs1.close();
            ps2.close();
            con.close();
            JOptionPane.showMessageDialog(null, "오프라인 지급이 완료되었습니다.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField8MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField8.setText("");        // TODO add your handling code here:
    }

    private void jTextField8ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField9MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField9.setText("");          // TODO add your handling code here:
    }

    private void jTextField9ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton11MouseClicked(java.awt.event.MouseEvent evt) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters1()) {
                if (chr.getName().equals(jTextField8.getText())) {
                    chr.gainHPoint(Integer.parseInt(jTextField9.getText()));
                    chr.dropMessage(1, "홍보포인트가 지급되었습니다.");
                    JOptionPane.showMessageDialog(null, "지급이 완료되었습니다.");
                    return;
                }
            }
        }
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            int hpoint = 0;
            int accountid = 0;
            PreparedStatement ps1 = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps1.setString(1, jTextField6.getText());
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            } else {
                JOptionPane.showMessageDialog(null, "존재하지 않는 캐릭터입니다.");
                return;
            }
            PreparedStatement ps2 = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps2.setInt(1, accountid);
            ResultSet rs1 = ps2.executeQuery();
            if (rs1.next()) {
                hpoint = rs1.getInt("hpoint");
            } else {
                JOptionPane.showMessageDialog(null, "존재하지 않는 계정입니다.");
                return;
            }
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hpoint = ? WHERE id = ?");
            ps.setInt(1, hpoint + Integer.parseInt(jTextField7.getText()));
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
            rs.close();
            ps1.close();
            rs1.close();
            ps2.close();
            con.close();
            JOptionPane.showMessageDialog(null, "오프라인 지급이 완료되었습니다.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }  // TODO add your handling code here:
    }

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton12MouseClicked(java.awt.event.MouseEvent evt) {
        switch (jComboBox1.getSelectedIndex()) {
            case 0:
                handling.SendPacketOpcode.reloadValues();
                handling.RecvPacketOpcode.reloadValues();
                break;
            case 1:
                MapleMonsterInformationProvider.getInstance().clearDrops();
                ReactorScriptManager.getInstance().clearDrops();
                break;
            case 2:
                PortalScriptManager.getInstance().clearScripts();
                break;
            case 3:
                NPCScriptManager.getInstance().scriptClear();
                break;
            case 4:
                MapleShopFactory.getInstance().clear();
                break;
            case 5:
                for (ChannelServer instance : ChannelServer.getAllInstances()) {
                    instance.reloadEvents();
                }
                break;
            default:
                break;
        }// TODO add your handling code here:
        JOptionPane.showMessageDialog(null, jComboBox1.getSelectedItem() + "리셋이 완료되었습니다.");
    }

    private void jTextField10MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField10.setText("");        // TODO add your handling code here:
    }

    private void jTextField10ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton15MouseClicked(java.awt.event.MouseEvent evt) {
        String data = Integer.parseInt(jTextField10.getText()) + "," + Integer.parseInt(jTextField11.getText());
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < jList1.getModel().getSize(); i++) {
            model.addElement(jList1.getModel().getElementAt(i));
        }
        model.addElement(data);
        jList1.setModel(model);
        // TODO add your handling code here:
    }

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField11MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField11.setText(""); // TODO add your handling code here:
    }

    private void jTextField11ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton16MouseClicked(java.awt.event.MouseEvent evt) {
        String players = "";
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters1()) {
                for (int j = 0; j < jList1.getModel().getSize(); j++) {
                    String singleItem = jList1.getModel().getElementAt(j);
                    chr.gainItem(Integer.parseInt(singleItem.split(",")[0]), Integer.parseInt(singleItem.split(",")[1]));
                }
                //chr.dropMessage(1, "핫타임이 지급되었습니다. 인벤토리를 확인해 주세요!");
                if (chr != null) {
                    players += ", ";
                }
                players += chr.getName();
            }
        }
        JOptionPane.showMessageDialog(null, "핫타임이 지급되었습니다. 아래는 핫타임을 지급받은 유저의 닉네임입니다.\n\n" + players);
    }

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton17MouseClicked(java.awt.event.MouseEvent evt) {
        DefaultListModel model = new DefaultListModel();
        jList1.setModel(model);
    }

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton18MouseClicked(java.awt.event.MouseEvent evt) {
        String data = Integer.parseInt(jTextField10.getText()) + "," + Integer.parseInt(jTextField11.getText());
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < jList1.getModel().getSize(); i++) {
            if (i != jList1.getSelectedIndex()) {
                model.addElement(jList1.getModel().getElementAt(i));
            }
        }

        jList1.setModel(model);// TODO add your handling code here:
    }

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField12MouseClicked(java.awt.event.MouseEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField12ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField13MouseClicked(java.awt.event.MouseEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField13ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton20MouseClicked(java.awt.event.MouseEvent evt) {
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < MapleHotTimeControl.getHotTimeItemIds().split(",").length; i++) {
            model.addElement(MapleHotTimeControl.getHotTimeItemIds().split(",")[i] + "," + MapleHotTimeControl.getHotTimeItemQtys().split(",")[i]);
        }
        jList2.setModel(model);
        jTextField12.setText(MapleHotTimeControl.getHotTimeHour() + "");
        jTextField13.setText(MapleHotTimeControl.getHotTimeMinute() + "");
    }

    private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {

    }

    private void jButton21MouseClicked(java.awt.event.MouseEvent evt) {
        MapleHotTimeControl.reloadProperty();
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < MapleHotTimeControl.getHotTimeItemIds().split(",").length; i++) {
            model.addElement(MapleHotTimeControl.getHotTimeItemIds().split(",")[i] + "," + MapleHotTimeControl.getHotTimeItemQtys().split(",")[i]);
        }
        jList2.setModel(model);
        jTextField12.setText(MapleHotTimeControl.getHotTimeHour() + "");
        jTextField13.setText(MapleHotTimeControl.getHotTimeMinute() + ""); // TODO add your handling code here:
    }

    private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton23ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jTextField14ActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void jButton19MouseClicked(java.awt.event.MouseEvent evt) {
        jTextField14.setText(ChannelServer.getInstance(1).getExpRate() + "");
        jTextField15.setText(ChannelServer.getInstance(1).getMesoRate() + "");
        jTextField16.setText(ChannelServer.getInstance(1).getDropRate() + "");

    }

    private void jButton23MouseClicked(java.awt.event.MouseEvent evt) {
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            if (MapleRateEventControl.getExpMin() == 0) {
                cserv.setExpRate(Integer.parseInt(jTextField14.getText()));
            }
            if (MapleRateEventControl.getMesoMin() == 0) {
                cserv.setMesoRate(Integer.parseInt(jTextField15.getText()));
            }
            if (MapleRateEventControl.getDropMin() == 0) {
                cserv.setDropRate(Integer.parseInt(jTextField16.getText()));
            }
        }
        JOptionPane.showMessageDialog(null, "변경이 완료되었습니다.\n\n(배율이벤트가 진행중일 경우 변경되지 않습니다.)");
    }

    private void jButton22MouseClicked(java.awt.event.MouseEvent evt) {
        switch (jComboBox2.getSelectedIndex()) {
            case 0:
                if (MapleRateEventControl.getExpMin() == 0) {
                    MapleRateEventControl.setExpMin(Integer.parseInt(jTextField18.getText()));
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(Integer.parseInt(jTextField17.getText()));
                    }
                    World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[경험치 이벤트] " + jTextField18.getText() + "분동안 경험치 배율이 " + jTextField17.getText() + "배로 변경됩니다!"));
                } else {
                    JOptionPane.showMessageDialog(null, "이벤트가 이미 진행중입니다.");
                }
                break;
            case 1:
                if (MapleRateEventControl.getMesoMin() == 0) {
                    MapleRateEventControl.setMesoMin(Integer.parseInt(jTextField18.getText()));
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(Integer.parseInt(jTextField17.getText()));
                    }
                    World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[메소 이벤트] " + jTextField18.getText() + "분동안 메소 배율이 " + jTextField17.getText() + "배로 변경됩니다!"));
                } else {
                    JOptionPane.showMessageDialog(null, "이벤트가 이미 진행중입니다.");
                }
                break;
            case 2:
                if (MapleRateEventControl.getDropMin() == 0) {
                    MapleRateEventControl.setDropMin(Integer.parseInt(jTextField18.getText()));
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setDropRate(Integer.parseInt(jTextField17.getText()));
                    }
                    World.Broadcast.broadcastMessage(CField.getGameMessage(8, "[드롭 이벤트] " + jTextField18.getText() + "분동안 드롭 배율이 " + jTextField17.getText() + "배로 변경됩니다!"));
                } else {
                    JOptionPane.showMessageDialog(null, "이벤트가 이미 진행중입니다.");
                }
                break;
            default:
                break;

        }
    }

    private int minutesLeft = 0;

    private void jButton24MouseClicked(java.awt.event.MouseEvent evt) {

        if (AdminCommand.getts() == null && (AdminCommand.gett() == null || !AdminCommand.gett().isAlive())) {
            minutesLeft = Integer.parseInt(jTextField19.getText());
            AdminCommand.sett(new Thread(ShutdownServer.getInstance()));
            AdminCommand.setts(EventTimer.getInstance().register(new Runnable() {
                public void run() {
                    if (minutesLeft == 0) {
                        ShutdownServer.getInstance().shutdown();
                        AdminCommand.gett().start();
                        AdminCommand.getts().cancel(false);
                    }
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(0, "서버가 " + minutesLeft + "분 뒤 종료될 예정입니다. 안전한 저장을 위해 로그아웃 해주세요."));
                    minutesLeft--;
                }
            }, 60000));
            JOptionPane.showMessageDialog(null, "리붓 설정이 완료되었습니다.");
        } else {
            JOptionPane.showMessageDialog(null, "리붓이 이미 진행중입니다.\n\n" + minutesLeft + " ~ " + (minutesLeft + 1) + "분 뒤에 서버가 종료됩니다.");
        }        // TODO add your handling code here:
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters1()) {
                if (accounts.contains(player.getName())) {
                    removeaccount(player.getName());
                    jLabel25.setText(String.valueOf((int) (Integer.parseInt(jLabel25.getText()) - 1)));
                }
            }
        }
        Timer timer = new Timer();
        timer.schedule(new resetAccountOnline(), 5000);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (MapleCharacter player : cs.getPlayerStorage().getAllCharacters1()) {
                if (characters.contains(player.getName())) {
                    removecharacter(player.getName());
                    jLabel26.setText(String.valueOf((int) (Integer.parseInt(jLabel26.getText()) - 1)));
                }
            }
        }
        Timer timer = new Timer();
        timer.schedule(new resetCharacterOnline(), 5000);
    }

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
        boolean check = false;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            MapleCharacter hp = null;
            hp = cs.getPlayerStorage().getCharacterByName(this.jTextField1.getText());
            if (hp == null && !check) {
                check = false;
            } else if (hp != null) {
                if (hp.getGMLevel() <= 0) {
                    check = true;
                    hp.setGMLevel(Integer.valueOf(jTextField4.getText()).byteValue());
                    hp.getClient().getSession().writeAndFlush(CWvsContext.getTopMsg("[알림] 해당 플레이어가 GM " + Integer.valueOf(jTextField4.getText()).byteValue() + "레벨이 되었습니다."));
                    JOptionPane.showMessageDialog(null, "GM설정을 하였습니다.");
                    jTextField1.setText("유저 이름");
                    jTextField4.setText("레벨");
                    return;
                } else {
                    check = true;
                    hp.setGMLevel((byte) 0);
                    hp.getClient().getSession().writeAndFlush(CWvsContext.getTopMsg("[알림] GM설정이 해제 되었습니다."));
                    JOptionPane.showMessageDialog(null, "GM설정이 해제 되었습니다.");
                    jTextField1.setText("유저 이름");
                    jTextField4.setText("레벨");
                    return;
                }
            }
        }
        if (!check) {
            JOptionPane.showMessageDialog(null, "플레이어가 접속 중이지 않습니다.");
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JMenuItem menu = (JMenuItem) evt.getSource();
        jFrame2.setVisible(true);
        jFrame2.setSize(480, 440);
    }

    class Alert extends Thread {

        @Override
        public void run() {
            try {
                Alert(txtMsg, txtOpt, intOpt);
            } catch (Exception e) {

            }
        }
    }

    private void Alert(String msg, String opt, int opt2) {
        JOptionPane.showMessageDialog(null, msg, opt, opt2);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ControlPannel().setVisible(true);
            }
        });
    }

    public static int getModelId(String ip) {
        /*DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if (model.getValueAt(i, 2).toString().equals(ip)) {
                return i;
            }
        }*/
        return 1;
    }

    // Variables declaration - do not modify
    public static javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    public static javax.swing.JButton jButton2;
    private javax.swing.JButton jButton20;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton24;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    public static javax.swing.JFrame jFrame2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    public static javax.swing.JLabel jLabel25;
    public static javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JList<String> jList1;
    private javax.swing.JList<String> jList2;
    public static javax.swing.JList<String> jList3;
    public static javax.swing.JList<String> jList4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JTabbedPane jTabbedPane1;
    public static final javax.swing.JTable jTable3 = new javax.swing.JTable();
    public static javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    // End of variables declaration
}
