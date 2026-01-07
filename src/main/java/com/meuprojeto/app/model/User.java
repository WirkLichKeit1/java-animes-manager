package com.meuprojeto.app.model;

public class User {
    private int id;
    private String nome;
    private String email;

    public User(int id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public User(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return nome;
    }

    public String getEmail() {
        return email;
    }
}