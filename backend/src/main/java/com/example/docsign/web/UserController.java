package com.example.docsign.web;

import com.example.docsign.user.User;
import com.example.docsign.user.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserRepository repo;
  public UserController(UserRepository repo){ this.repo = repo; }

  @GetMapping public List<User> all(){ return repo.findAll(); }
  @PostMapping public User create(@RequestBody User u){ return repo.save(u); }
}
