package smartnavi.markargus.com.smartnavi.rest;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

/**
 * Class that initializes the server which runs the Server module.
 *
 * Created by mc on 3/17/15.
 */
public class Server extends Application {

    private static final int SERVER_PORT = 3030;

    @Override
    public synchronized Restlet createInboundRoot() {
        //Set resource routes
        Router router = new Router(getContext());
        router.attach("/data", Resource.class);
        return router;
    }

    private DefaultHttpClient getHttpClient(){
        HttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        return new DefaultHttpClient(cm, params);
    }

}

