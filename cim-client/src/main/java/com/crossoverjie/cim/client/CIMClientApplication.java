package com.crossoverjie.cim.client;

import com.crossoverjie.cim.client.scanner.Scan;
import com.crossoverjie.cim.client.service.impl.ClientInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author crossoverJie
 */
@SpringBootApplication
public class CIMClientApplication implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(CIMClientApplication.class);

    private final ClientInfo clientInfo;

    public CIMClientApplication(ClientInfo clientInfo) {
        logger.info("应用初始化 ...");
        this.clientInfo = clientInfo;
    }

    public static void main(String[] args) {
        logger.info("main start");
        SpringApplication.run(CIMClientApplication.class, args);
        logger.info("启动 Client 服务成功");
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("守护进程...");
        Scan scan = new Scan();
        Thread thread = new Thread(scan);
        thread.setName("scan-thread");
        thread.start();
        clientInfo.saveStartDate();
    }
}
