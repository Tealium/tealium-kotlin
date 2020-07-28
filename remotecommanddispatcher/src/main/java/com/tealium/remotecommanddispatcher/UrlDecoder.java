package com.tealium.remotecommanddispatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UrlDecoder {
    public static JSONObject decode(String url) {
        String decodedString = "";
        JSONObject json = null;
        try {
            decodedString = URLDecoder.decode(url, "UTF-8");
            json = decodedString.length() == 0 ? new JSONObject() : new JSONObject(decodedString);
        } catch (UnsupportedEncodingException ex) {
            return new JSONObject();
//            throw new RuntimeException(ex);
        } catch (JSONException ex ) {
            return new JSONObject();

        } catch (Throwable t) {
            return new JSONObject();
        }
//        } catch (RuntimeException ex) {
//        return new JSONObject();
//    }

        return json;
    }
}
