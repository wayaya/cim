package com.crossoverjie.cim.client.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.crossoverjie.cim.client.config.AppConfiguration;
import com.crossoverjie.cim.client.service.EchoService;
import com.crossoverjie.cim.client.service.RouteRequest;
import com.crossoverjie.cim.client.vo.req.GroupReqVO;
import com.crossoverjie.cim.client.vo.req.LoginReqVO;
import com.crossoverjie.cim.client.vo.req.P2PReqVO;
import com.crossoverjie.cim.client.vo.res.CIMServerResVO;
import com.crossoverjie.cim.client.vo.res.OnlineUsersResVO;
import com.crossoverjie.cim.common.enums.StatusEnum;
import com.crossoverjie.cim.common.res.BaseResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2018/12/22 22:27
 * @since JDK 1.8
 */
@Service
public class RouteRequestImpl implements RouteRequest {

    private final static Logger logger = LoggerFactory.getLogger(RouteRequestImpl.class);

    private final OkHttpClient okHttpClient;

    private MediaType mediaType = MediaType.parse("application/json");

    @Value("${cim.group.route.request.url}")
    private String groupRouteRequestUrl;

    @Value("${cim.p2p.route.request.url}")
    private String p2pRouteRequestUrl;

    @Value("${cim.server.route.request.url}")
    private String serverRouteLoginUrl;

    @Value("${cim.server.online.user.url}")
    private String onlineUserUrl;

    private final EchoService echoService;


    private final AppConfiguration appConfiguration;

    public RouteRequestImpl(OkHttpClient okHttpClient, EchoService echoService, AppConfiguration appConfiguration) {
        this.okHttpClient = okHttpClient;
        this.echoService = echoService;
        this.appConfiguration = appConfiguration;
    }

    @Override
    public void sendGroupMsg(GroupReqVO groupReqVO) throws Exception {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg", groupReqVO.getMsg());
        jsonObject.put("userId", groupReqVO.getUserId());
        RequestBody requestBody = RequestBody.create(mediaType, jsonObject.toString());

        Request request = new Request.Builder()
                .url(groupRouteRequestUrl)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        } finally {
            response.body().close();
        }
    }

    @Override
    public void sendP2PMsg(P2PReqVO p2PReqVO) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg", p2PReqVO.getMsg());
        jsonObject.put("userId", p2PReqVO.getUserId());
        jsonObject.put("receiveUserId", p2PReqVO.getReceiveUserId());
        RequestBody requestBody = RequestBody.create(mediaType, jsonObject.toString());

        Request request = new Request.Builder()
                .url(p2pRouteRequestUrl)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        ResponseBody body = response.body();
        try {
            String json = body.string();
            BaseResponse baseResponse = JSON.parseObject(json, BaseResponse.class);

            //选择的账号不存在
            if (baseResponse.getCode().equals(StatusEnum.OFF_LINE.getCode())) {
                logger.error(p2PReqVO.getReceiveUserId() + ":" + StatusEnum.OFF_LINE.getMessage());
            }

        } finally {
            body.close();
        }
    }

    @Override
    public CIMServerResVO.ServerInfo getCIMServer(LoginReqVO loginReqVO) throws Exception {


        SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
        filter.getExcludes().add("timeStamp");
        logger.info("loginVo:{}", JSONObject.toJSONString(loginReqVO, filter));

        RequestBody requestBody = RequestBody.create(mediaType, JSONObject.toJSONString(loginReqVO, filter));

        logger.info("serverRouteLoginUrl:{}", serverRouteLoginUrl);
        Request request = new Request.Builder()
                .url(serverRouteLoginUrl)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        CIMServerResVO cimServerResVO;
        ResponseBody body = response.body();
        try {
            String json = body.string();
            cimServerResVO = JSON.parseObject(json, CIMServerResVO.class);

            //重复失败
            if (!cimServerResVO.getCode().equals(StatusEnum.SUCCESS.getCode())) {
                echoService.echo(cimServerResVO.getMessage());
                System.exit(-1);
            }

        } finally {
            body.close();
        }


        return cimServerResVO.getDataBody();
    }

    @Override
    public List<OnlineUsersResVO.DataBodyBean> onlineUsers() throws Exception {

        JSONObject jsonObject = new JSONObject();
        RequestBody requestBody = RequestBody.create(mediaType, jsonObject.toString());

        Request request = new Request.Builder()
                .url(onlineUserUrl)
                .post(requestBody)
                .build();

        Response response = okHttpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }


        ResponseBody body = response.body();
        OnlineUsersResVO onlineUsersResVO;
        try {
            String json = body.string();
            onlineUsersResVO = JSON.parseObject(json, OnlineUsersResVO.class);

        } finally {
            body.close();
        }

        return onlineUsersResVO.getDataBody();
    }

    @Override
    public void offLine() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", appConfiguration.getUserId());
        jsonObject.put("msg", "offLine");
        RequestBody requestBody = RequestBody.create(mediaType, jsonObject.toString());

        Request request = new Request.Builder()
                .url(appConfiguration.getClearRouteUrl())
                .post(requestBody)
                .build();

        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            logger.error("exception", e);
        } finally {
            response.body().close();
        }
    }
}
