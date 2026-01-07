package com.meuprojeto.app;

import com.meuprojeto.app.service.UserService;

public class Main {
    public static void main(String[] args){

        UserService service = new UserService();

        service.createUser("Luiz", "luiz@gmail.com");
        service.createUser("Leticia", "leticia@gmail.com");

        service.ListUser().forEach(u ->
            System.out.println(u.getId() + " - " + u.getName())
        );
    }
}