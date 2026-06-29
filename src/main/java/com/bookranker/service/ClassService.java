package com.bookranker.service;

import com.bookranker.model.ClassEntity;
import com.bookranker.repository.ClassRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClassService {

  private final ClassRepository classRepository;

  public ClassService(ClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  public ClassEntity createClass(String name) {
    ClassEntity c = new ClassEntity();
    c.setName(name);
    c.setJoinCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    return classRepository.save(c);
  }

  public ClassEntity getClass(String id) {
    return classRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Class not found"));
  }
}
