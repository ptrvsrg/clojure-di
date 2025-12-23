package com.example.app;

public class Main {
    public static void main(String[] args) {
        System.out.println("---------- Starting Java Application ----------");

        DI.init();

        UserService service = DI.getUserService();

        String result = service.processUser(1L);

        System.out.println(result);
        System.out.println("-----------------------------------------------");
    }
}
