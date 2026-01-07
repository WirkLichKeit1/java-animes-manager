package com.meuprojeto.app.service;

import com.meuprojeto.app.model.User;
import com.meuprojeto.app.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

    public void createUser(String nome, String email) {
        User user = new User(nome, email);
        repository.save(user);
    }

    public List<User> ListUsers() {
        return repository.findAll();
    }
}