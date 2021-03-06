package com.code.server.cardgame.handler;



import com.code.server.cardgame.config.ServerState;
import com.code.server.cardgame.core.MsgDispatch;
import com.code.server.cardgame.timer.GameTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by sun on 2015/8/21.
 *
 */
public class GameProcessor implements Runnable{


    private final Logger logger = LoggerFactory.getLogger(GameProcessor.class);

    private GameProcessor(){}

    public static GameProcessor instance;

    public LinkedBlockingQueue<MessageHolder> messageQueue = new LinkedBlockingQueue<>(1000);

    public MsgDispatch handler = new MsgDispatch();

    public static GameProcessor getInstance(){
        if(instance == null){
            instance = new GameProcessor();
        }
        return instance;
    }


    public void handle(){
        while(true){
            try {
                MessageHolder messHolder = messageQueue.poll(10, TimeUnit.MILLISECONDS);
                if(messHolder != null&&messHolder.message !=null){
                    handler.handleMessage(messHolder);
                }
                if (messageQueue.size() == 0) {
                    logger.debug("消息处理完毕");
                    return;
                }
            } catch (Exception e) {
                logger.error("handle message error ",e);
            }
        }
    }

    @Override
    public void run() {
       while(ServerState.isWork){

           try {
               MessageHolder messHolder = messageQueue.poll(10, TimeUnit.MILLISECONDS);
               if(messHolder != null&&messHolder.message !=null){
                   handler.handleMessage(messHolder);
               }
               //定时任务
               GameTimer.getInstance().handle();
//               System.out.println("i1111112ii");
//               test();
//               Thread.sleep(1000);

           } catch (Exception e) {
               logger.error("handle message error ",e);
           }
       }
    }

    private void test(){
        System.out.println("121");
        test1();
    }

    private void test1(){
        System.out.println("000");
    }

}
