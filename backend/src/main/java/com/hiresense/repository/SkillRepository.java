package com.hiresense.repository;

import com.hiresense.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByNameIgnoreCaseIn(Collection<String> names);

    List<Skill> findAllByOrderByNameAsc();
}