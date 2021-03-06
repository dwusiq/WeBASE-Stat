/**
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.webank.webase.stat.base.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.webank.webase.stat.base.code.ConstantCode;
import com.webank.webase.stat.base.code.RetCode;
import com.webank.webase.stat.base.entity.BaseResponse;
import com.webank.webase.stat.base.exception.BaseException;
import com.webank.webase.stat.base.tools.pagetools.entity.MapHandle;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * common method.
 */
@Log4j2
public class CommonUtils {

    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_FORMAT_NO_SPACE = "yyyyMMddHHmmss";

    /**
     * convert hex to localDateTime.
     */
    public static LocalDateTime hex2LocalDateTime(String hexStr) {
        if (StringUtils.isBlank(hexStr)) {
            return null;
        }
        Long timeLong = Long.parseLong(hexStr, 16);
        return timestamp2LocalDateTime(timeLong);
    }

    /**
     * convert timestamp to localDateTime.
     */
    public static LocalDateTime timestamp2LocalDateTime(Long inputTime) {
        if (inputTime == null) {
            log.warn("timestamp2LocalDateTime fail. inputTime is null");
            return null;
        }
        Instant instant = Instant.ofEpochMilli(inputTime);
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }

    /**
     * convert String to localDateTime.
     */
    public static LocalDateTime string2LocalDateTime(String time, String format) {
        if (StringUtils.isBlank(time)) {
            log.info("string2LocalDateTime. time is null");
            return null;
        }
        if (StringUtils.isBlank(format)) {
            log.info("string2LocalDateTime. format is null");
            format = DEFAULT_DATE_TIME_FORMAT;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(time, df);
    }

    /**
     * convert localDateTime to String.
     */
    public static String localDateTime2String(LocalDateTime dateTime, String format) {
        if (dateTime == null) {
            log.warn("localDateTime2String. dateTime is null");
            return null;
        }
        if (StringUtils.isBlank(format)) {
            log.info("localDateTime2String. format is null");
            format = DEFAULT_DATE_TIME_FORMAT;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
        String localTimeStr = df.format(dateTime);
        return localTimeStr;
    }

    /**
     * conver object to java bean.
     */
    public static <T> T object2JavaBean(Object obj, Class<T> clazz) {
        if (obj == null || clazz == null) {
            log.warn("object2JavaBean. obj or clazz null");
            return null;
        }
        String jsonStr = JSON.toJSONString(obj);

        return JSON.parseObject(jsonStr, clazz);
    }


    public static JSONObject Object2JSONObject(Object obj) {
        if (obj == null) {
            log.warn("obj is null");
            return null;
        }
        String objJson = JSON.toJSONString(obj);
        return JSONObject.parseObject(objJson);
    }


    /**
     * convert list to url param.
     */
    public static String convertUrlParam(List<String> nameList, List<Object> valueList) {
        if (nameList == null || valueList == null || nameList.size() != valueList.size()) {
            log.error("fail convertUrlParm. nameList or valuesList is error");
            return "";
        }
        StringBuilder urlParamB = new StringBuilder("");
        for (int i = 0; i < valueList.size(); i++) {
            Object value = valueList.get(i);
            if (value != null) {
                urlParamB.append("&").append(nameList.get(i)).append("=").append(value);
            }
        }

        if (urlParamB.length() == 0) {
            log.info("fail convertUrlParam. urlParamB length is 0");
            return "";
        }

        String urlParam = urlParamB.toString();
        return urlParam.substring(1);

    }

    /**
     * convert list to map.
     */
    public static Map<String, Object> buidMap(List<String> nameList, List<Object> valueList) {
        if (nameList == null || valueList == null || nameList.size() != valueList.size()) {
            log.error("fail buidMap. nameList or valuesList is error");
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < nameList.size(); i++) {
            map.put(nameList.get(i), valueList.get(i));
        }
        return map;
    }

    /**
     * check server host.
     */
    public static void checkServerHostConnect(String serverHost) {
        Boolean state;
        try {
            InetAddress address = InetAddress.getByName(serverHost);
            state = address.isReachable(500);
        } catch (Exception ex) {
            log.error("fail checkServerHostConnect", ex);
            throw new BaseException(ConstantCode.SERVER_CONNECT_FAIL);
        }

        if (!state) {
            log.info("host connect state:{}", state);
            throw new BaseException(ConstantCode.SERVER_CONNECT_FAIL);
        }
    }


    /**
     * check host and port.
     */
    public static void checkServerConnect(String serverHost, int serverPort) {
        // check host
        // checkServerHostConnect(serverHost);

        Socket socket = null;
        try {
            // check port
            socket = new Socket();
            socket.setReceiveBufferSize(8193);
            socket.setSoTimeout(500);
            SocketAddress address = new InetSocketAddress(serverHost, serverPort);
            socket.connect(address, 1000);
        } catch (Exception ex) {
            log.error("fail checkServerConnect", ex);
            throw new BaseException(ConstantCode.SERVER_CONNECT_FAIL);
        } finally {
            if (Objects.nonNull(socket)) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("fail close socket", e);
                }
            }
        }
    }


    /**
     * response exception.
     */
    public static void responseRetCodeException(HttpServletResponse response, RetCode retCode) {
        BaseResponse baseResponse = new BaseResponse(retCode);
        try {
            response.getWriter().write(JSON.toJSONString(baseResponse));
        } catch (IOException e) {
            log.error("fail responseRetCodeException", e);
        }
    }


    /**
     * check target time is valid.
     *
     * @param dateTime target time.
     * @param validLength y:year, M:month, d:day of month, h:hour, m:minute, n:forever valid;
     *        example1:1d;example2:n
     */
    public static boolean isDateTimeInValid(LocalDateTime dateTime, String validLength) {
        log.debug("start isDateTimeInValid. dateTime:{} validLength:{}", dateTime, validLength);
        if ("n".equals(validLength)) {
            return true;
        }
        if (Objects.isNull(dateTime) || StringUtils.isBlank(validLength)
                || validLength.length() < 2) {
            return false;
        }

        String lifeStr = validLength.substring(0, validLength.length() - 1);
        if (!StringUtils.isNumeric(lifeStr)) {
            log.warn("fail isDateTimeInValid");
            throw new RuntimeException("fail isDateTimeInValid. validLength is error");
        }
        int lifeValue = Integer.valueOf(lifeStr);
        String lifeUnit = validLength.substring(validLength.length() - 1);

        LocalDateTime now = LocalDateTime.now();
        switch (lifeUnit) {
            case "y":
                return now.getYear() - dateTime.getYear() < lifeValue;
            case "M":
                return now.getMonthValue() - dateTime.getMonthValue() < lifeValue;
            case "d":
                return now.getDayOfMonth() - dateTime.getDayOfMonth() < lifeValue;
            case "m":
                return now.getMinute() - dateTime.getMinute() < lifeValue;
            default:
                log.warn("fail isDateTimeInValid lifeUnit:{}", lifeUnit);
                return false;
        }
    }

    /**
     * is json.
     */
    public static boolean isJSON(String str) {
        boolean result;
        try {
            JSON.parse(str);
            result = true;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    /**
     * response string.
     */
    public static void responseString(HttpServletResponse response, String str) {
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SYSTEM_EXCEPTION);
        if (StringUtils.isNotBlank(str)) {
            baseResponse.setMessage(str);
        }

        RetCode retCode;
        if (isJSON(str) && (retCode = JSONObject.parseObject(str, RetCode.class)) != null) {
            baseResponse = new BaseResponse(retCode);
        }

        try {
            response.getWriter().write(JSON.toJSONString(baseResponse));
        } catch (IOException e) {
            log.error("fail responseRetCodeException", e);
        }
    }

    /**
     * sort Mappings
     * 
     * @param mapping
     * @return List<MapHandle>
     */
    public static List<MapHandle> sortMap(Map<?, ?> mapping) {
        List<MapHandle> list = new ArrayList<>();
        Iterator it = mapping.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            MapHandle handle = new MapHandle(key, mapping.get(key));
            list.add(handle);
        }
        Collections.sort(list, new Comparator<MapHandle>() {
            @Override
            public int compare(MapHandle o1, MapHandle o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        return list;
    }

    /**
     * parseHexStr2Int.
     * 
     * @param str str
     * @return
     */
    public static int parseHexStr2Int(String str) {
        if (StringUtils.isBlank(str)) {
            return 0;
        }
        return Integer.parseInt(str.substring(2), 16);
    }
}
