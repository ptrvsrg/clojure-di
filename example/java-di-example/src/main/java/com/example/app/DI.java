package com.example.app;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import static clojure.java.api.Clojure.read;
import static clojure.java.api.Clojure.var;

public class DI {

    private static final IFn INIT_FN;
    private static final IFn GET_USER_SERVICE_FN;

    static {
        IFn require = var("clojure.core", "require");
        require.invoke(read("clojure-di.core"));
        require.invoke(read("clojure-di.container.java-injection"));
        require.invoke(read("app.core"));

        INIT_FN = var("app.core", "init");
        GET_USER_SERVICE_FN = var("app.core", "get-user-service");
    }

    public static void init() {
        INIT_FN.invoke();
    }

    public static UserService getUserService() {
        Object service = GET_USER_SERVICE_FN.invoke();
        return (UserService) service;
    }
}
