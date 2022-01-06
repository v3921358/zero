package connector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import server.Timer;
import server.control.MapleSkillControl;

/**
 * @author SLFCG
 */
public class ConnectorServer {

    private static ConnectorServer instance = new ConnectorServer();
    private ServerBootstrap bootstrap;
    private ConnectorClientStorage clients;

    public static ConnectorServer getInstance() {
        return instance;
    }

    public final ConnectorClientStorage getClientStorage() {
        if (clients == null) { //wth
            clients = new ConnectorClientStorage(); //wthhhh
        }
        return clients;
    }

    public void run_startup_configurations() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("decodeer", new ConnectorNettyDecoder());
                    ch.pipeline().addLast("encodeer", new ConnectorNettyEncoder());
                    ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(30, 5, 0));
                    ch.pipeline().addLast("handler", new ConnectorNettyHandler());
                    ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(0xFFFF));
                    ch.config().setOption(ChannelOption.SO_RCVBUF, 0xFFFF);
                }
            }).option(ChannelOption.SO_BACKLOG, 128).option(ChannelOption.SO_RCVBUF, 0xFFFF).option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(0xFFFF)).childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture cf = bootstrap.bind(5466).sync();
            Timer.WorldTimer.getInstance().register(new ConnectorThread(), 10000);
            clients = new ConnectorClientStorage();
            System.out.println("접속기 서버 개방 성공");
        } catch (InterruptedException ex) {
            System.err.println("관리기 서버 개방 실패\r\n" + ex);
        }
    }
}
